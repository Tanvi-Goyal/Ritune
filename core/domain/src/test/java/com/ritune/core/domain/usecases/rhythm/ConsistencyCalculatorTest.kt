package com.ritune.core.domain.usecases.rhythm

import com.ritune.core.models.Anchor
import com.ritune.core.models.AnchorEvent
import com.ritune.core.models.DefaultCatalog
import com.ritune.core.testing.data.anAnchorEvent
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atTime
import kotlinx.datetime.toInstant
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ConsistencyCalculatorTest {

    private val wake = DefaultCatalog.wake
    private val lightsOut = DefaultCatalog.lightsOut
    private val anchors = listOf(wake, lightsOut)

    private val weekEnd = LocalDate(2026, 1, 11)
    private val weekStart = LocalDate(2026, 1, 5)
    private val lastWeek = LocalDate(2025, 12, 29)

    private var nextId = 0

    private fun log(anchor: Anchor, day: LocalDate, hour: Int, minute: Int = 0): AnchorEvent =
        anAnchorEvent(
            id = "event-${nextId++}",
            anchorId = anchor.id,
            occurredAt = day.atTime(LocalTime(hour, minute)).toInstant(TimeZone.UTC),
        )

    private fun LocalDate.plusDays(days: Int) = LocalDate.fromEpochDays(toEpochDays() + days)

    private fun compute(events: List<AnchorEvent>, anchors: List<Anchor> = this.anchors) =
        ConsistencyCalculator.compute(anchors, events, weekEnd)

    @Test
    fun compute_noEvents_hasNothingToScore() {
        val rhythm = compute(emptyList())

        assertEquals((0..6).map { weekStart.plusDays(it) }, rhythm.days.map { it.day })
        assertTrue(rhythm.days.all { it.fraction == null })
        assertNull(rhythm.consistency)
        assertNull(rhythm.steadiest)
        assertEquals(0, rhythm.loggedDays)
        assertFalse(rhythm.hasEnoughData)
    }

    @Test
    fun compute_loggedDays_needsThreeForEnoughData() {
        val twoDays = listOf(log(wake, weekStart, 7), log(wake, weekStart.plusDays(1), 7))

        assertFalse(compute(twoDays).hasEnoughData)
        assertTrue(compute(twoDays + log(lightsOut, lastWeek, 22)).hasEnoughData)
    }

    @Test
    fun compute_anchorWithTwoLogs_isNotScored() {
        val rhythm = compute(listOf(log(wake, weekStart, 7), log(wake, weekStart.plusDays(1), 7)))

        assertNull(rhythm.anchors.first { it.anchor == wake }.medianMinutes)
        assertNull(rhythm.consistency)
        assertTrue(rhythm.days.all { it.fraction == null })
    }

    @Test
    fun compute_logWithinThirtyMinutesOfMedian_isSteady() {
        val history = (0..2).map { log(wake, lastWeek.plusDays(it), 7) }
        val thisWeek = listOf(log(wake, weekStart, 7, 30), log(wake, weekStart.plusDays(1), 7, 31))

        val rhythm = compute(history + thisWeek)

        assertEquals(7 * 60, rhythm.anchors.first { it.anchor == wake }.medianMinutes)
        assertEquals(listOf(1f, 0f, null, null, null, null, null), rhythm.days.map { it.fraction })
        assertEquals(0.5f, rhythm.consistency)
    }

    @Test
    fun compute_logsAcrossMidnight_measureDistanceAroundTheClock() {
        val events = listOf(
            log(lightsOut, weekStart, 23, 50),
            log(lightsOut, weekStart.plusDays(1), 23, 55),
            log(lightsOut, weekStart.plusDays(3), 0, 10),
        )

        val rhythm = compute(events)

        assertEquals(23 * 60 + 55, rhythm.anchors.first { it.anchor == lightsOut }.medianMinutes)
        assertEquals(1f, rhythm.consistency)
    }

    @Test
    fun compute_evenLogsAcrossMidnight_medianStaysAtMidnight() {
        val events = listOf(
            log(lightsOut, weekStart, 23, 50),
            log(lightsOut, weekStart.plusDays(1), 23, 50),
            log(lightsOut, weekStart.plusDays(3), 0, 10),
            log(lightsOut, weekStart.plusDays(4), 0, 10),
        )

        val rhythm = compute(events)

        assertEquals(0, rhythm.anchors.first { it.anchor == lightsOut }.medianMinutes)
        assertEquals(1f, rhythm.consistency)
    }

    @Test
    fun compute_eventOffset_placesLogOnLocalDay() {
        val tokyoMorning = anAnchorEvent(
            anchorId = wake.id,
            occurredAt = weekStart.plusDays(-1).atTime(LocalTime(22, 0)).toInstant(TimeZone.UTC),
            utcOffsetSeconds = 9 * 3_600,
        )

        val rhythm = compute(listOf(tokyoMorning))

        assertEquals(1, rhythm.anchors.first { it.anchor == wake }.logsThisWeek)
    }

    @Test
    fun compute_steadiest_isAnchorWithLeastDrift() {
        val events = (0..2).flatMap { day ->
            listOf(
                log(wake, weekStart.plusDays(day), 7),
                log(lightsOut, weekStart.plusDays(day), 22, if (day == 1) 20 else 0),
            )
        }

        val rhythm = compute(events)

        val lightsOutRhythm = rhythm.anchors.first { it.anchor == lightsOut }
        assertEquals(7, lightsOutRhythm.driftMinutes)
        assertEquals(3, lightsOutRhythm.logsThisWeek)
        assertEquals(wake, rhythm.steadiest)
    }

    @Test
    fun compute_eventsBeforeWindow_areIgnored() {
        val windowStart = weekEnd.plusDays(-20)
        val events = listOf(
            log(wake, windowStart.plusDays(-1), 7),
            log(wake, windowStart, 7),
            log(wake, lastWeek, 7),
        )

        val rhythm = compute(events)

        assertEquals(2, rhythm.loggedDays)
        assertNull(rhythm.anchors.first { it.anchor == wake }.medianMinutes)
    }
}
