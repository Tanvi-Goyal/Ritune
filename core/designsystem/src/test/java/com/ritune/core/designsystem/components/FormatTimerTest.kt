package com.ritune.core.designsystem.components

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

class FormatTimerTest {

    @Test
    fun formatTimer_zero_showsZero() {
        assertEquals("0:00", formatTimer(Duration.ZERO))
    }

    @Test
    fun formatTimer_negative_clampsToZero() {
        assertEquals("0:00", formatTimer((-5).seconds))
    }

    @Test
    fun formatTimer_partialSecond_roundsUp() {
        assertEquals("0:01", formatTimer(200.milliseconds))
        assertEquals("1:00", formatTimer(59.seconds + 1.milliseconds))
    }

    @Test
    fun formatTimer_underAnHour_padsSecondsOnly() {
        assertEquals("2:05", formatTimer(2.minutes + 5.seconds))
        assertEquals("59:59", formatTimer(59.minutes + 59.seconds))
    }

    @Test
    fun formatTimer_anHourOrMore_addsPaddedMinutes() {
        assertEquals("1:00:00", formatTimer(1.hours))
        assertEquals("1:02:03", formatTimer(1.hours + 2.minutes + 3.seconds))
    }

    @Test
    fun formatTimer_justUnderAnHour_roundsUpIntoHours() {
        assertEquals("1:00:00", formatTimer(59.minutes + 59.seconds + 500.milliseconds))
    }
}
