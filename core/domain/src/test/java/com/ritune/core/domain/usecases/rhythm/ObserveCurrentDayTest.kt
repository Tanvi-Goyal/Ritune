package com.ritune.core.domain.usecases.rhythm

import app.cash.turbine.test
import com.ritune.core.testing.time.VirtualTimekeeper
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant

class ObserveCurrentDayTest {

    @Test
    fun invoke_atLocalMidnight_emitsNextDay() = runTest {
        // 18:29 UTC is 23:59 in Kolkata.
        val clock = VirtualTimekeeper(
            testScheduler,
            startWall = Instant.parse("2026-01-05T18:29:00Z"),
            zone = TimeZone.of("Asia/Kolkata"),
        )

        ObserveCurrentDay(clock)().test {
            assertEquals(LocalDate(2026, 1, 5), awaitItem())
            assertEquals(LocalDate(2026, 1, 6), awaitItem())
            assertEquals(1.minutes.inWholeMilliseconds, testScheduler.currentTime)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
