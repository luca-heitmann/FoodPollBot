import com.github.kotlintelegrambot.Bot
import com.github.kotlintelegrambot.bot
import com.github.kotlintelegrambot.dispatch
import com.github.kotlintelegrambot.dispatcher.callbackQuery
import com.github.kotlintelegrambot.dispatcher.command
import com.github.kotlintelegrambot.dispatcher.handlers.CallbackQueryHandlerEnvironment
import com.github.kotlintelegrambot.dispatcher.handlers.CommandHandlerEnvironment
import com.github.kotlintelegrambot.entities.ChatId
import com.github.kotlintelegrambot.entities.InlineKeyboardMarkup
import com.github.kotlintelegrambot.entities.keyboard.InlineKeyboardButton
import com.github.kotlintelegrambot.logging.LogLevel
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.*

const val FOOD_POLL_COMMAND = "foodpoll"
const val GET_IN_COMMAND = "getin"
const val GET_OUT_COMMAND = "getout"

val timeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
val timer = Timer()

/**
 * TODO
 * - Konfigurationsmöglichkeiten (Kommandos und Nachrichten)
 */
fun main() {
    val bot = bot {
        token = System.getenv("BOT_TOKEN")!!
        logLevel = LogLevel.All()
        dispatch {
            command(FOOD_POLL_COMMAND, ::handleFoodPoll)
            callbackQuery(GET_IN_COMMAND, ::handleGetIn)
            callbackQuery(GET_OUT_COMMAND, ::handleGetOut)
        }
    }
    loadFoodPolls()
    getFoodPolls().forEach {
        scheduleStartFoodPoll(it, bot)
    }
    bot.startPolling()
}

fun handleFoodPoll(env: CommandHandlerEnvironment) {
    if (env.args.isEmpty()) {
        env.bot.sendMessage(
            chatId = ChatId.fromId(env.message.chat.id),
            text = """
                Falsche Benutzung!
                /foodpoll <Zeit> [<Name>]
                /foodpoll 12:00 Lecker Schnitzel
                /foodpoll 13
            """.trimIndent()
        )
        return
    }

    try {
        val time = parseTime(env.args[0])
        val name = if (env.args.size > 1) env.args.slice(1..<env.args.size).joinToString(" ") else null

        if (findFoodPoll(time) != null) {
            env.bot.sendMessage(
                chatId = ChatId.fromId(env.message.chat.id),
                text = "Es existiert bereits ein FoodPoll um diese Zeit"
            )
        } else if (time.isBefore(LocalDateTime.now())) {
            env.bot.sendMessage(
                chatId = ChatId.fromId(env.message.chat.id),
                text = "Ein FoodPoll kann nicht in der Vergangenheit erstellt werden"
            )
        } else {
            val result = env.bot.sendMessage(
                chatId = ChatId.fromId(env.message.chat.id),
                text = formatMessageText(name, time, listOf(env.message.from!!.firstName)),
                replyMarkup = createGetInOutButtons()
            )

            val foodPoll = FoodPoll(
                chatId = env.message.chat.id,
                messageId = result.get().messageId,
                time = time,
                name = name,
                members = arrayListOf(
                    FoodPollMember(
                        userId = env.message.from!!.id,
                        name = env.message.from!!.firstName
                    )
                )
            )

            result.fold({
                createAndScheduleFoodPoll(foodPoll, env.bot)
            }, {
                env.bot.sendMessage(
                    chatId = ChatId.fromId(env.message.chat.id),
                    text = "FoodPoll konnte nicht erstellt werden"
                )
            })
        }
    } catch (e: DateTimeParseException) {
        e.printStackTrace()
        env.bot.sendMessage(
            chatId = ChatId.fromId(env.message.chat.id),
            text = "Zeit nicht erkannt: ${env.args[0]}"
        )
    }
}

/**
 * Returns the current date with the given time. The time can be in the following formats:
 * 1. 09:00 or 9:0
 * 2. 09.00 or 9.0
 * 3. 09 or 9
 */
fun parseTime(timeStr: String): LocalDateTime =
    if (timeStr matches "\\d\\d?:\\d\\d?".toRegex())
        LocalTime.parse(timeStr, DateTimeFormatter.ofPattern("H:m")).atDate(LocalDate.now())
    else if (timeStr matches "\\d\\d?.\\d\\d?".toRegex())
        LocalTime.parse(timeStr, DateTimeFormatter.ofPattern("H.m")).atDate(LocalDate.now())
    else if (timeStr matches "\\d\\d?".toRegex())
        LocalTime.parse(timeStr, DateTimeFormatter.ofPattern("H")).atDate(LocalDate.now())
    else
        throw DateTimeParseException("Unable to parse time", timeStr, 0)

fun formatMessageText(foodPoll: FoodPoll) =
    formatMessageText(foodPoll.name, foodPoll.time, foodPoll.members.map { it.name })

fun formatMessageText(name: String?, time: LocalDateTime, members: List<String>) =
    "FoodPoll ${if (name != null) "$name " else ""}um ${time.format(timeFormatter)} Uhr mit ${members.joinToString(", ")}"

fun createGetInOutButtons() = InlineKeyboardMarkup.createSingleRowKeyboard(
    InlineKeyboardButton.CallbackData(
        text = "Einsteigen",
        callbackData = GET_IN_COMMAND
    ),
    InlineKeyboardButton.CallbackData(
        text = "Aussteigen",
        callbackData = GET_OUT_COMMAND
    )
)

fun createAndScheduleFoodPoll(foodPoll: FoodPoll, bot: Bot) {
    createFoodPoll(foodPoll)
    scheduleStartFoodPoll(foodPoll, bot)
}

fun scheduleStartFoodPoll(foodPoll: FoodPoll, bot: Bot) {
    val startDate = Date.from(foodPoll.time.atZone(ZoneId.systemDefault()).toInstant())
    val startTask = object : TimerTask() {
        override fun run() = startFoodPoll(foodPoll, bot)
    }
    timer.schedule(startTask, startDate)
}

fun startFoodPoll(foodPoll: FoodPoll, bot: Bot) {
    // check if poll is still existing because polls are deleted when all members leave
    if (getFoodPolls().contains(foodPoll)) {
        bot.deleteMessage(
            chatId = ChatId.fromId(foodPoll.chatId),
            messageId = foodPoll.messageId
        )

        bot.sendMessage(
            chatId = ChatId.fromId(foodPoll.chatId),
            text = "FoodPoll hebt ab mit ${foodPoll.members.joinToString(", ") { it.name }}"
        )

        removeFoodPoll(foodPoll)
    }
}

fun handleGetIn(env: CallbackQueryHandlerEnvironment) {
    val chatId = env.callbackQuery.message?.chat?.id ?: return
    val messageId = env.callbackQuery.message?.messageId ?: return
    val userId = env.callbackQuery.from.id
    val userName = env.callbackQuery.from.firstName

    val foodPoll = findFoodPoll(chatId, messageId) ?: return

    if (foodPoll.members.find { it.userId == userId } == null) {
        foodPoll.members.add(FoodPollMember(userId, userName))

        env.bot.editMessageText(
            chatId = ChatId.fromId(foodPoll.chatId),
            messageId = foodPoll.messageId,
            text = formatMessageText(foodPoll),
            replyMarkup = createGetInOutButtons()
        )
    }
}

fun handleGetOut(env: CallbackQueryHandlerEnvironment) {
    val chatId = env.callbackQuery.message?.chat?.id ?: return
    val messageId = env.callbackQuery.message?.messageId ?: return
    val userId = env.callbackQuery.from.id

    val foodPoll = findFoodPoll(chatId, messageId) ?: return
    val foodPollMember = foodPoll.members.find { it.userId == userId }
    foodPoll.members.remove(foodPollMember)

    if (foodPoll.members.isEmpty()) {
        removeFoodPoll(foodPoll)

        env.bot.editMessageText(
            chatId = ChatId.fromId(foodPoll.chatId),
            messageId = foodPoll.messageId,
            text = "FoodPoll ${if (foodPoll.name != null) "${foodPoll.name} " else ""}um ${foodPoll.time.format(timeFormatter)} Uhr abgebrochen, weil alle ausgestiegen sind"
        )
    } else {
        env.bot.editMessageText(
            chatId = ChatId.fromId(foodPoll.chatId),
            messageId = foodPoll.messageId,
            text = formatMessageText(foodPoll),
            replyMarkup = createGetInOutButtons()
        )
    }
}
