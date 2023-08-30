package de.lhe.foodpollbot.domain

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.IOException
import java.nio.charset.Charset
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardOpenOption
import java.time.LocalDateTime

val FOOD_POLLS_FILE = System.getenv("FOOD_POLLS_FILE") ?: "foodPolls.json"

@Serializable
data class FoodPoll(
    val chatId: Long,
    val messageId: Long,
    val type: String,
    @Serializable(with = LocalDateTimeSerializer::class)
    val time: LocalDateTime,
    val name: String?,
    val translationNumber: Int,
    val members: ArrayList<FoodPollMember>,
)

@Serializable
data class FoodPollMember(
    val userId: Long,
    val name: String,
)

private var currentFoodPolls = ArrayList<FoodPoll>()

fun createFoodPoll(foodPoll: FoodPoll) {
    currentFoodPolls.add(foodPoll)
    persistFoodPolls()
}

fun removeFoodPoll(foodPoll: FoodPoll) {
    currentFoodPolls.remove(foodPoll)
    persistFoodPolls()
}

fun getFoodPolls(): List<FoodPoll> = currentFoodPolls.toList()

fun findFoodPoll(chatId: Long, messageId: Long) =
    currentFoodPolls.find { it.chatId == chatId && it.messageId == messageId }

fun findFoodPoll(chatId: Long, time: LocalDateTime) =
    currentFoodPolls.find { it.chatId == chatId && it.time == time }

fun persistFoodPolls() {
    try {
        Files.writeString(
            Paths.get(FOOD_POLLS_FILE),
            Json.encodeToString(currentFoodPolls),
            Charset.defaultCharset(),
            StandardOpenOption.CREATE,
            StandardOpenOption.WRITE,
            StandardOpenOption.TRUNCATE_EXISTING
        )
    } catch (e: IOException) {
        e.printStackTrace()
    }
}

fun loadFoodPolls() {
    try {
        if (Files.exists(Paths.get(FOOD_POLLS_FILE))) {
            val json = Files.readString(Paths.get(FOOD_POLLS_FILE))
            val foodPolls = Json.decodeFromString<List<FoodPoll>>(json)
            foodPolls
                .filter { LocalDateTime.now().isBefore(it.time) }
                .forEach(::createFoodPoll)
        }
    } catch (e: IOException) {
        e.printStackTrace()
    }
}
