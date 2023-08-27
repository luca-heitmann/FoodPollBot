package de.lhe.foodpollbot

import de.lhe.foodpollbot.application.chatBot
import de.lhe.foodpollbot.application.scheduleStartFoodPoll
import de.lhe.foodpollbot.domain.getFoodPolls
import de.lhe.foodpollbot.domain.loadFoodPolls

fun main() {
    loadFoodPolls()
    getFoodPolls().forEach {
        scheduleStartFoodPoll(it)
    }
    chatBot.initBot()
}
