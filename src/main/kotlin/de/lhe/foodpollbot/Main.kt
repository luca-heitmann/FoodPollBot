package de.lhe.foodpollbot

import de.lhe.foodpollbot.application.chatBot
import de.lhe.foodpollbot.application.getFoodPollTypes
import de.lhe.foodpollbot.application.scheduleStartFoodPoll
import de.lhe.foodpollbot.domain.getFoodPolls
import de.lhe.foodpollbot.domain.loadFoodPolls

fun main() {
    println("Found the following FoodPoll types: " + getFoodPollTypes())
    loadFoodPolls()
    getFoodPolls().forEach { scheduleStartFoodPoll(it) }
    chatBot.initBot()
}
