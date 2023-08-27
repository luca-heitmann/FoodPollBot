package de.lhe.foodpollbot.application

import de.lhe.foodpollbot.domain.*
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.*

val timer = Timer()
val timeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

fun handleFoodPollCommand(chatId: Long, userId: Long, userName: String, args: List<String>) {
    if (args.isEmpty()) {
        chatBot.sendMessage(
            chatId = chatId,
            text = """
                Falsche Benutzung!
                /foodpoll <Zeit> [<Name>]
                /foodpoll 12:00 Lecker Schnitzel
                /foodpoll 13
            """.trimIndent()
        )
        return
    }

    val time = parseTime(args[0])
    val name = if (args.size > 1) args.slice(1..<args.size).joinToString(" ") else null

    if (time == null) {
        chatBot.sendMessage(chatId, "Zeit nicht erkannt: ${args[0]}")
    } else if (findFoodPoll(time) != null) {
        chatBot.sendMessage(chatId, "Es existiert bereits ein FoodPoll für diese Zeit")
    } else if (LocalDateTime.now().isAfter(time)) {
        chatBot.sendMessage(chatId, "Ein FoodPoll kann nicht in der Vergangenheit erstellt werden")
    } else {
        val messageId = chatBot.sendMessage(chatId, formatMessageText(name, time, listOf(userName)), true) ?: return

        val foodPoll = FoodPoll(
            chatId = chatId,
            messageId = messageId,
            time = time,
            name = name,
            members = arrayListOf(FoodPollMember(userId, userName))
        )

        createFoodPoll(foodPoll)
        scheduleStartFoodPoll(foodPoll)
    }
}

/**
 * Returns the current date with the given time. The time can be in the following formats:
 * 1. 09:00 or 9:0
 * 2. 09.00 or 9.0
 * 3. 09 or 9
 */
fun parseTime(timeStr: String): LocalDateTime? {
    return try {
        if (timeStr matches "\\d\\d?:\\d\\d?".toRegex())
            LocalTime.parse(timeStr, DateTimeFormatter.ofPattern("H:m")).atDate(LocalDate.now())
        else if (timeStr matches "\\d\\d?.\\d\\d?".toRegex())
            LocalTime.parse(timeStr, DateTimeFormatter.ofPattern("H.m")).atDate(LocalDate.now())
        else if (timeStr matches "\\d\\d?".toRegex())
            LocalTime.parse(timeStr, DateTimeFormatter.ofPattern("H")).atDate(LocalDate.now())
        else
            null
    } catch (e: DateTimeParseException) {
        null
    }
}

fun formatMessageText(foodPoll: FoodPoll) =
    formatMessageText(foodPoll.name, foodPoll.time, foodPoll.members.map { it.name })

fun formatMessageText(name: String?, time: LocalDateTime, members: List<String>) =
    "FoodPoll ${if (name != null) "$name " else ""}um ${time.format(timeFormatter)} Uhr mit ${members.joinToString(", ")}"

fun scheduleStartFoodPoll(foodPoll: FoodPoll) {
    if (LocalDateTime.now().isBefore(foodPoll.time)) {
        val startDate = Date.from(foodPoll.time.atZone(ZoneId.systemDefault()).toInstant())
        val startTask = object : TimerTask() {
            override fun run() = startFoodPoll(foodPoll)
        }
        timer.schedule(startTask, startDate)
    }
}

fun startFoodPoll(foodPoll: FoodPoll) {
    // check if poll is still existing because polls are deleted when all members leave
    if (getFoodPolls().contains(foodPoll)) {
        chatBot.deleteMessage(foodPoll.chatId, foodPoll.messageId)
        chatBot.sendMessage(foodPoll.chatId, "FoodPoll hebt ab mit ${foodPoll.members.joinToString(", ") { it.name }}")

        removeFoodPoll(foodPoll)
    }
}

fun handleGetInCallback(chatId: Long, messageId: Long, userId: Long, userName: String) {
    val foodPoll = findFoodPoll(chatId, messageId) ?: return

    if (foodPoll.members.find { it.userId == userId } == null) {
        foodPoll.members.add(FoodPollMember(userId, userName))

        chatBot.editMessage(foodPoll.chatId, foodPoll.messageId, formatMessageText(foodPoll), true)
    }
}

fun handleGetOutCallback(chatId: Long, messageId: Long, userId: Long) {
    val foodPoll = findFoodPoll(chatId, messageId) ?: return
    val foodPollMember = foodPoll.members.find { it.userId == userId }
    foodPoll.members.remove(foodPollMember)

    if (foodPoll.members.isEmpty()) {
        removeFoodPoll(foodPoll)

        chatBot.editMessage(foodPoll.chatId, foodPoll.messageId, "FoodPoll ${if (foodPoll.name != null) "${foodPoll.name} " else ""}um ${foodPoll.time.format(timeFormatter)} Uhr abgebrochen, weil alle ausgestiegen sind")
    } else {
        chatBot.editMessage(foodPoll.chatId, foodPoll.messageId, formatMessageText(foodPoll), true)
    }
}
