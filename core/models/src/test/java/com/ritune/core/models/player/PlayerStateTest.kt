package com.ritune.core.models.player

import com.ritune.core.models.Routine
import com.ritune.core.models.RoutineId
import com.ritune.core.models.RunId
import com.ritune.core.models.RunOutcome
import com.ritune.core.models.RunSummary
import com.ritune.core.models.Session
import com.ritune.core.models.Step
import com.ritune.core.models.StepId
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

class PlayerStateTest {

    private val session = Session(
        RunId("run"),
        Routine(RoutineId("routine"), "Morning", listOf(Step(StepId("a"), "Stretch", 1.minutes))),
        stepIndex = 0,
        startedAt = Instant.DISTANT_PAST,
    )

    @Test
    fun remainingAt_running_countsDownToDeadline() {
        val running = PlayerState.Running(session, stepEndsAtMono = 10_000)

        assertEquals(4.seconds, running.remainingAt(6_000))
    }

    @Test
    fun remainingAt_runningPastDeadline_isZero() {
        val running = PlayerState.Running(session, stepEndsAtMono = 10_000)

        assertEquals(Duration.ZERO, running.remainingAt(12_000))
    }

    @Test
    fun remainingAt_paused_ignoresClock() {
        val paused = PlayerState.Paused(session, remaining = 7.seconds)

        assertEquals(7.seconds, paused.remainingAt(99_000))
    }

    @Test
    fun remainingAt_idleOrFinished_isZero() {
        val summary = RunSummary(
            RunId("run"), RoutineId("routine"), "Routine", Instant.DISTANT_PAST, Instant.DISTANT_PAST,
            RunOutcome.STOPPED, completedSteps = 0, skippedSteps = 0, totalSteps = 1,
        )

        assertEquals(Duration.ZERO, PlayerState.Idle.remainingAt(5_000))
        assertEquals(Duration.ZERO, PlayerState.Finished(summary).remainingAt(5_000))
    }
}
