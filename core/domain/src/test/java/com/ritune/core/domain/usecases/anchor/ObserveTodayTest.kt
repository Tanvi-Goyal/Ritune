package com.ritune.core.domain.usecases.anchor

import app.cash.turbine.test
import com.ritune.core.models.DefaultCatalog
import com.ritune.core.testing.data.anAnchorEvent
import com.ritune.core.testing.repository.FakeAnchorEventRepository
import com.ritune.core.testing.repository.FakeAnchorRepository
import com.ritune.core.testing.time.FakeTimekeeper
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.time.Instant

class ObserveTodayTest {

    private val wake = DefaultCatalog.wake
    private val events = FakeAnchorEventRepository()
    private val observeToday = ObserveToday(
        FakeAnchorRepository(listOf(wake, DefaultCatalog.lightsOut)),
        events,
        FakeTimekeeper(zone = TimeZone.of("Asia/Kolkata")),
    )

    @Test
    fun invoke_anchorLoggedTwice_reportsEarliestLog() = runTest {
        val first = Instant.parse("2026-01-05T01:30:00Z")
        events.events.value = listOf(
            anAnchorEvent(id = "late", occurredAt = Instant.parse("2026-01-05T02:00:00Z")),
            anAnchorEvent(id = "early", occurredAt = first),
        )

        val today = observeToday(LocalDate(2026, 1, 5))

        today.test {
            val (wakeStatus, lightsOutStatus) = awaitItem()
            assertEquals(first, wakeStatus.loggedAt)
            assertNull(lightsOutStatus.loggedAt)
        }
    }

    @Test
    fun invoke_logOutsideLocalDay_isIgnoredAndNewLogsAppear() = runTest {
        // 18:29 UTC is 23:59 on the 4th in Kolkata.
        events.events.value = listOf(anAnchorEvent(occurredAt = Instant.parse("2026-01-04T18:29:00Z")))

        observeToday(LocalDate(2026, 1, 5)).test {
            assertNull(awaitItem().first().loggedAt)

            val midnight = Instant.parse("2026-01-04T18:30:00Z")
            events.events.update { it + anAnchorEvent(id = "event-2", occurredAt = midnight) }

            assertEquals(midnight, awaitItem().first().loggedAt)
        }
    }
}
