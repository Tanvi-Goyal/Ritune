package com.ritune.core.domain.usecases.rhythm

import app.cash.turbine.test
import com.ritune.core.testing.data.anAnchorEvent
import com.ritune.core.testing.repository.FakeAnchorEventRepository
import com.ritune.core.testing.repository.FakeAnchorRepository
import com.ritune.core.testing.time.FakeTimekeeper
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.days
import kotlin.time.Instant

class ObserveWeekRhythmTest {

    private val events = FakeAnchorEventRepository()
    private val observeWeekRhythm = ObserveWeekRhythm(FakeAnchorRepository(), events, FakeTimekeeper())

    @Test
    fun invoke_newLog_recomputesRhythm() = runTest {
        val weekEnd = LocalDate(2026, 1, 11)

        observeWeekRhythm(weekEnd).test {
            assertEquals(0, awaitItem().loggedDays)

            events.events.update { it + anAnchorEvent() }

            assertEquals(1, awaitItem().loggedDays)
        }
    }

    @Test
    fun invoke_readsTwentyOneDaysEndingOnWeekEnd() = runTest {
        val windowStart = Instant.parse("2025-12-22T00:00:00Z")
        events.events.value = listOf(
            anAnchorEvent(id = "before", occurredAt = windowStart - 1.days),
            anAnchorEvent(id = "first", occurredAt = windowStart),
            anAnchorEvent(id = "last", occurredAt = windowStart + 20.days),
            anAnchorEvent(id = "after", occurredAt = windowStart + 21.days),
        )

        observeWeekRhythm(LocalDate(2026, 1, 11)).test {
            assertEquals(2, awaitItem().loggedDays)
        }
    }
}
