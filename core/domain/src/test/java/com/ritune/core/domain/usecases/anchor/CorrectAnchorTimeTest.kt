package com.ritune.core.domain.usecases.anchor

import com.ritune.core.domain.models.anchor.CorrectResult
import com.ritune.core.models.EventId
import com.ritune.core.models.EventSource
import com.ritune.core.testing.data.anAnchorEvent
import com.ritune.core.testing.id.SequentialIdGenerator
import com.ritune.core.testing.repository.FakeAnchorEventRepository
import com.ritune.core.testing.time.FakeTimekeeper
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.time.Instant

class CorrectAnchorTimeTest {

    private val logged = anAnchorEvent()
    private val events = FakeAnchorEventRepository(listOf(logged))

    // testInstant is 13:30 in Kolkata.
    private val clock = FakeTimekeeper(zone = TimeZone.of("Asia/Kolkata"))
    private val correct = CorrectAnchorTime(events, clock, SequentialIdGenerator("event"))

    private val today = LocalDate(2026, 1, 5)

    @Test
    fun invoke_pastTime_replacesEventAsBackfillInLocalZone() = runTest {
        val result = correct(logged.id, logged.anchorId, today, LocalTime(7, 0))

        val corrected = assertIs<CorrectResult.Corrected>(result).event
        assertEquals(EventId("event-1"), corrected.id)
        assertEquals(Instant.parse("2026-01-05T01:30:00Z"), corrected.occurredAt)
        assertEquals(19_800, corrected.utcOffsetSeconds)
        assertEquals(EventSource.BACKFILL, corrected.source)
        assertEquals(listOf(corrected.occurredAt), events.events.value.map { it.occurredAt })
    }

    @Test
    fun invoke_futureTime_isRejectedAndKeepsEvent() = runTest {
        val result = correct(logged.id, logged.anchorId, today, LocalTime(13, 31))

        assertEquals(CorrectResult.InFuture, result)
        assertEquals(listOf(logged), events.events.value)
    }
}
