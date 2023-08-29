package de.lhe.foodpollbot.application

import com.github.kotlintelegrambot.bot
import com.github.kotlintelegrambot.dispatch
import com.github.kotlintelegrambot.dispatcher.callbackQuery
import com.github.kotlintelegrambot.dispatcher.command
import com.github.kotlintelegrambot.entities.ChatId
import com.github.kotlintelegrambot.entities.InlineKeyboardMarkup
import com.github.kotlintelegrambot.entities.keyboard.InlineKeyboardButton
import com.github.kotlintelegrambot.logging.LogLevel

const val GET_IN_COMMAND = "getIn"
const val GET_OUT_COMMAND = "getOut"

val chatBot = TelegramChatBotInterface

interface ChatBotInterface {

    fun sendTranslatedMessage(
        chatId: Long,
        foodPollType: String,
        messageKey: String,
        vararg messageArgs: String,
        includeButtons: Boolean = false,
    ) = sendMessage(
        chatId,
        foodPollType,
        getTranslatedMessage(foodPollType, messageKey, null, *messageArgs),
        includeButtons
    )

    fun sendTranslatedMessage(
        chatId: Long,
        foodPollType: String,
        messageKey: String,
        messageVariant: Int? = null,
        vararg messageArgs: String,
        includeButtons: Boolean = false,
    ) = sendMessage(
        chatId,
        foodPollType,
        getTranslatedMessage(foodPollType, messageKey, messageVariant, *messageArgs),
        includeButtons
    )

    fun sendMessage(
        chatId: Long,
        foodPollType: String,
        text: String,
        includeButtons: Boolean = false,
    ): Long?

    fun editTranslatedMessage(
        chatId: Long,
        messageId: Long,
        foodPollType: String,
        messageKey: String,
        vararg messageArgs: String,
        includeButtons: Boolean = false,
    ) = editMessage(
        chatId,
        messageId,
        foodPollType,
        getTranslatedMessage(foodPollType, messageKey, null, *messageArgs),
        includeButtons
    )

    fun editTranslatedMessage(
        chatId: Long,
        messageId: Long,
        foodPollType: String,
        messageKey: String,
        messageVariant: Int? = null,
        vararg messageArgs: String,
        includeButtons: Boolean = false,
    ) = editMessage(
        chatId,
        messageId,
        foodPollType,
        getTranslatedMessage(foodPollType, messageKey, messageVariant, *messageArgs),
        includeButtons
    )

    fun editMessage(
        chatId: Long,
        messageId: Long,
        foodPollType: String,
        text: String,
        includeButtons: Boolean = false,
    )

    fun deleteMessage(
        chatId: Long,
        messageId: Long,
    )
}

object TelegramChatBotInterface : ChatBotInterface {
    private val bot = bot {
        token = System.getenv("BOT_TOKEN")!!
        logLevel = LogLevel.Error
        dispatch {
            getFoodPollTypes().forEach { foodPollType ->
                command(foodPollType) {
                    handleFoodPollCommand(
                        chatId = message.chat.id,
                        userId = message.from!!.id,
                        userName = message.from!!.firstName.split(" ")[0],
                        foodPollType = foodPollType,
                        args = args
                    )
                }
            }
            callbackQuery(GET_IN_COMMAND) {
                handleGetInCallback(
                    chatId = callbackQuery.message!!.chat.id,
                    messageId = callbackQuery.message!!.messageId,
                    userId = callbackQuery.from.id,
                    userName = callbackQuery.from.firstName.split(" ")[0]
                )
            }
            callbackQuery(GET_OUT_COMMAND) {
                handleGetOutCallback(
                    chatId = callbackQuery.message!!.chat.id,
                    messageId = callbackQuery.message!!.messageId,
                    userId = callbackQuery.from.id
                )
            }
        }
    }

    fun initBot() = bot.startPolling()

    override fun sendMessage(chatId: Long, foodPollType: String, text: String, includeButtons: Boolean): Long? {
        val result = bot.sendMessage(
            chatId = ChatId.fromId(chatId),
            text = text,
            replyMarkup = if (includeButtons) createGetInOutButtons(foodPollType) else null
        )

        return result.getOrNull()?.messageId
    }

    override fun editMessage(chatId: Long, messageId: Long, foodPollType: String, text: String, includeButtons: Boolean) {
        bot.editMessageText(
            chatId = ChatId.fromId(chatId),
            messageId = messageId,
            text = text,
            replyMarkup = if (includeButtons) createGetInOutButtons(foodPollType) else null
        )
    }

    override fun deleteMessage(chatId: Long, messageId: Long) {
        bot.deleteMessage(
            chatId = ChatId.fromId(chatId),
            messageId = messageId
        )
    }

    private fun createGetInOutButtons(foodPollType: String) = InlineKeyboardMarkup.createSingleRowKeyboard(
        InlineKeyboardButton.CallbackData(
            text = getTranslatedMessage(foodPollType, GET_IN_KEY),
            callbackData = GET_IN_COMMAND
        ),
        InlineKeyboardButton.CallbackData(
            text = getTranslatedMessage(foodPollType, GET_OUT_KEY),
            callbackData = GET_OUT_COMMAND
        )
    )
}
