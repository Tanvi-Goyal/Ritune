package com.ritune.core.domain.usecases.player

import app.cash.turbine.test
import com.ritune.core.models.player.PlayerState
import com.ritune.core.testing.data.aSession
import com.ritune.core.testing.ports.FakePlayerController
import com.ritune.core.testing.time.VirtualTimekeeper
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

class ObserveRunProgressTest {

    private val player = FakePlayerController()

    @Test
    fun invoke_running_emitsOncePerDisplayedSecond() = runTest {
        player.state.value = PlayerState.Running(aSession(), stepEndsAtMono = 2_500)

        ObserveRunProgress(player, VirtualTimekeeper(testScheduler))().test {
            assertEquals(3.seconds, awaitItem().remaining)
            assertEquals(2.seconds, awaitItem().remaining)
            assertEquals(600, testScheduler.currentTime)
            assertEquals(1.seconds, awaitItem().remaining)
            assertEquals(Duration.ZERO, awaitItem().remaining)
            assertEquals(2_600, testScheduler.currentTime)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun invoke_paused_emitsRemainingOnceAndStopsTicking() = runTest {
        player.state.value = PlayerState.Running(aSession(), stepEndsAtMono = 10_000)

        ObserveRunProgress(player, VirtualTimekeeper(testScheduler))().test {
            awaitItem()
            player.state.value = PlayerState.Paused(aSession(), remaining = 7.seconds)

            val paused = awaitItem()
            assertEquals(7.seconds, paused.remaining)
            assertEquals(PlayerState.Paused(aSession(), 7.seconds), paused.state)
            expectNoEvents()
        }
    }

    @Test
    fun invoke_idle_reportsZero() = runTest {
        ObserveRunProgress(player, VirtualTimekeeper(testScheduler))().test {
            assertEquals(Duration.ZERO, awaitItem().remaining)
            expectNoEvents()
        }
    }
}
