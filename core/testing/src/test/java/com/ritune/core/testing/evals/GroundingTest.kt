package com.ritune.core.testing.evals

import kotlinx.datetime.LocalTime
import org.junit.Test
import kotlin.test.assertEquals

class GroundingTest {
    private val source = """{"consistency_percent":86,"usual_time":"22:30","week":"2026-11-05..2026-11-11"}"""

    @Test
    fun ungrounded_numbersAndTimesInSource_returnsNothing() {
        val reply = "You were 86% steady, and screens went off around 22:30 on Nov 5."
        assertEquals(emptyList(), Grounding.ungrounded(reply, source))
    }

    @Test
    fun ungrounded_twelveHourTime_matchesTwentyFourHourSource() {
        assertEquals(emptyList(), Grounding.ungrounded("Usually about 10:30 PM.", source))
        assertEquals(emptyList(), Grounding.ungrounded("Usually about 10:30pm.", source))
        assertEquals(emptyList(), Grounding.ungrounded("Usually about 10:30.", source))
    }

    @Test
    fun ungrounded_numberInNoSource_returnsIt() {
        assertEquals(listOf("7.5"), Grounding.ungrounded("You slept 7.5 hours.", source))
        assertEquals(listOf("9 PM"), Grounding.ungrounded("Try 9 PM instead.", source))
    }

    @Test
    fun ungrounded_allowNewTimes_passesTimesButNotOtherNumbers() {
        val reply = "Try 22:45, about 75 minutes later."

        assertEquals(listOf("75"), Grounding.ungrounded(reply, source, allowNewTimes = true))
    }

    @Test
    fun ungrounded_leadingZeroInSource_stillMatches() {
        assertEquals(emptyList(), Grounding.ungrounded("That was on the 5th.", source))
    }

    @Test
    fun times_withoutAmPm_returnsMorningAndEvening() {
        assertEquals(setOf(LocalTime(21, 30)), Grounding.times("It's back to 9:30 PM."))
        assertEquals(setOf(LocalTime(9, 30), LocalTime(21, 30)), Grounding.times("It's 9:30."))
    }
}
