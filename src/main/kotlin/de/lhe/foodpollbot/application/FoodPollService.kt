package de.lhe.foodpollbot.application

import de.lhe.foodpollbot.domain.*
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.*
import kotlin.random.Random

val timer = Timer()
val timeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

fun handleFoodPollCommand(chatId: Long, userId: Long, userName: String, foodPollType: String, args: List<String>) {
    if (args.isEmpty()) {
        chatBot.sendTranslatedMessage(chatId, foodPollType, HELP_KEY)
        return
    }

    val time = parseTime(args[0])
    val name = if (args.size > 1) args.slice(1..<args.size).joinToString(" ") else null

    if (time == null) {
        chatBot.sendTranslatedMessage(chatId, foodPollType, TIME_FORMAT_ERROR_KEY, args[0])
    } else if (LocalDateTime.now().isAfter(time)) {
        chatBot.sendTranslatedMessage(chatId, foodPollType, TIME_IN_PAST_ERROR_KEY)
    } else if (findFoodPoll(chatId, time) != null) {
        chatBot.sendTranslatedMessage(chatId, foodPollType, TIME_EXISTS_ERROR_KEY)
    } else {
        val translationsKey = if (name == null) FOOD_POLL_KEY else NAMED_FOOD_POLL_KEY
        val translationNumber = Random.nextInt(0, getNumOfPossibleTranslations(foodPollType, translationsKey))
        val translationArgs = if (name == null)
            arrayOf(time.format(timeFormatter), userName)
        else
            arrayOf(name, time.format(timeFormatter), userName)

        val messageId = chatBot.sendTranslatedMessage(
            chatId = chatId,
            foodPollType = foodPollType,
            messageKey = translationsKey,
            messageVariant = translationNumber,
            messageArgs = translationArgs,
            includeButtons = true,
        ) ?: return

        val foodPoll = FoodPoll(
            chatId = chatId,
            messageId = messageId,
            type = foodPollType,
            time = time,
            name = name,
            translationNumber = translationNumber,
            members = arrayListOf(FoodPollMember(userId, userName)),
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
        val members = foodPoll.members.joinToString(", ") { it.name }
        val translationArgs = if (foodPoll.name == null) arrayOf(members) else arrayOf(foodPoll.name, members)
        val messageKey = if (foodPoll.name == null) FOOD_POLL_START_KEY else NAMED_FOOD_POLL_START_KEY
        chatBot.sendTranslatedMessage(foodPoll.chatId, foodPoll.type, messageKey, *translationArgs)

        removeFoodPoll(foodPoll)
    }
}

fun handleGetInCallback(chatId: Long, messageId: Long, userId: Long, userName: String) {
    val foodPoll = findFoodPoll(chatId, messageId) ?: return

    if (foodPoll.members.find { it.userId == userId } == null) {
        foodPoll.members.add(FoodPollMember(userId, userName))

        updateFoodPollMessage(foodPoll)
    }
}

fun handleGetOutCallback(chatId: Long, messageId: Long, userId: Long) {
    val foodPoll = findFoodPoll(chatId, messageId) ?: return
    val foodPollMember = foodPoll.members.find { it.userId == userId }
    foodPoll.members.remove(foodPollMember)

    if (foodPoll.members.isEmpty()) {
        removeFoodPoll(foodPoll)

        val translationArgs = if (foodPoll.name == null)
            arrayOf(foodPoll.time.format(timeFormatter))
        else
            arrayOf(foodPoll.name, foodPoll.time.format(timeFormatter))

        chatBot.editTranslatedMessage(
            chatId = foodPoll.chatId,
            messageId = foodPoll.messageId,
            foodPollType = foodPoll.type,
            messageKey = if (foodPoll.name == null) FOOD_POLL_CANCELED_KEY else NAMED_FOOD_POLL_CANCELED_KEY,
            messageArgs = translationArgs
        )
    } else {
        updateFoodPollMessage(foodPoll)
    }
}

fun updateFoodPollMessage(foodPoll: FoodPoll) {
    val translationArgs = if (foodPoll.name == null)
        arrayOf(foodPoll.time.format(timeFormatter), foodPoll.members.joinToString(", ") { it.name })
    else
        arrayOf(foodPoll.name, foodPoll.time.format(timeFormatter), foodPoll.members.joinToString(", ") { it.name })

    chatBot.editTranslatedMessage(
        chatId = foodPoll.chatId,
        messageId = foodPoll.messageId,
        foodPollType = foodPoll.type,
        messageKey = FOOD_POLL_KEY,
        messageVariant = foodPoll.translationNumber,
        messageArgs = translationArgs,
        includeButtons = true
    )
}
