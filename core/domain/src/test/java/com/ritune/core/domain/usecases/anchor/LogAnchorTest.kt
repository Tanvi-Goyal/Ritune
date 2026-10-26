package com.ritune.core.domain.usecases.anchor

import com.ritune.core.domain.models.anchor.LogResult
import com.ritune.core.models.AnchorEvent
import com.ritune.core.models.DefaultCatalog
import com.ritune.core.models.EventId
import com.ritune.core.models.EventSource
import com.ritune.core.testing.data.anAnchorEvent
import com.ritune.core.testing.data.testInstant
import com.ritune.core.testing.id.SequentialIdGenerator
import com.ritune.core.testing.repository.FakeAnchorEventRepository
import com.ritune.core.testing.time.FakeTimekeeper
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.TimeZone
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

class LogAnchorTest {

    private val events = FakeAnchorEventRepository()
    private val clock = FakeTimekeeper(zone = TimeZone.of("Asia/Kolkata"))
    private val logAnchor = LogAnchor(events, clock, SequentialIdGenerator("event"))

    private val wake = DefaultCatalog.wake.id

    @Test
    fun invoke_firstLog_appendsEventWithLocalOffset() = runTest {
        val result = logAnchor(wake, EventSource.MANUAL)

        val expected = AnchorEvent(
            id = EventId("event-1"),
            anchorId = wake,
            occurredAt = testInstant,
            utcOffsetSeconds = 19_800,
            source = EventSource.MANUAL,
        )
        assertEquals(LogResult.Logged(expected), result)
        assertEquals(listOf(expected), events.events.value)
    }

    @Test
    fun invoke_withinDedupeWindow_returnsExistingEvent() = runTest {
        val first = assertIs<LogResult.Logged>(logAnchor(wake, EventSource.MANUAL)).event
        clock.advanceBy(1_999.milliseconds)

        val result = logAnchor(wake, EventSource.NOTIFICATION)

        assertEquals(LogResult.Duplicate(first), result)
        assertEquals(1, events.events.value.size)
    }

    @Test
    fun invoke_afterDedupeWindow_logsAgain() = runTest {
        logAnchor(wake, EventSource.MANUAL)
        clock.advanceBy(2.seconds)

        val result = logAnchor(wake, EventSource.MANUAL)

        assertIs<LogResult.Logged>(result)
        assertEquals(2, events.events.value.size)
    }

    @Test
    fun invoke_recentEventForOtherAnchor_doesNotDedupe() = runTest {
        events.append(anAnchorEvent(anchorId = DefaultCatalog.daylight.id, occurredAt = testInstant))

        val result = logAnchor(wake, EventSource.MANUAL)

        assertIs<LogResult.Logged>(result)
    }
}
