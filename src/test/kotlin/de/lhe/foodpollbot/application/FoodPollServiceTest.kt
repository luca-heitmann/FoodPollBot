package de.lhe.foodpollbot.application

import org.junit.jupiter.api.Test

import org.junit.jupiter.api.Assertions.*
import java.time.LocalDateTime

class FoodPollServiceTest {

    @Test
    fun parseTime() {
        verifyParseTime("12:01", 12, 1)
        verifyParseTime("12:10", 12, 10)
        verifyParseTime("09:10", 9, 10)
        verifyParseTime("9:10", 9, 10)
        verifyParseTime("9:01", 9, 1)
        verifyParseTime("9:1", 9, 1)
        verifyParseTime("12.01", 12, 1)
        verifyParseTime("12.10", 12, 10)
        verifyParseTime("09.10", 9, 10)
        verifyParseTime("9.10", 9, 10)
        verifyParseTime("9.01", 9, 1)
        verifyParseTime("9.1", 9, 1)
        verifyParseTime("12", 12, 0)
        verifyParseTime("9", 9, 0)
        verifyUnableToParseTime("asfd")
        verifyUnableToParseTime("25:00")
        verifyUnableToParseTime("1200")
    }

    private fun verifyParseTime(time: String, expectedHour: Int, expectedMinute: Int) {
        val expected = LocalDateTime.now()
            .withHour(expectedHour)
            .withMinute(expectedMinute)
            .withSecond(0)
            .withNano(0)

        assertEquals(parseTime(time), expected)
    }

    private fun verifyUnableToParseTime(time: String) {
        assertNull(parseTime(time))
    }
}