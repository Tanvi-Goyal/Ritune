package com.ritune.core.domain.usecases.player

import com.ritune.core.models.Moment
import com.ritune.core.models.RunId
import com.ritune.core.models.RunOutcome
import com.ritune.core.models.player.PlayerEffect
import com.ritune.core.models.player.PlayerEvent
import com.ritune.core.models.player.PlayerState
import com.ritune.core.testing.data.aRoutine
import com.ritune.core.testing.data.aStep
import com.ritune.core.testing.data.testInstant
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertSame
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

class RoutinePlayerReducerTest {

    private val routine = aRoutine(
        steps = listOf(
            aStep(id = "a", duration = 10.seconds),
            aStep(id = "b", duration = 20.seconds),
        ),
    )

    private fun at(monoMillis: Long) = Moment(monoMillis, testInstant + monoMillis.milliseconds)

    private fun started(): PlayerState.Running {
        val transition = RoutinePlayerReducer.reduce(
            PlayerState.Idle,
            PlayerEvent.Start(routine, RunId("run-1"), at(0)),
        )
        return transition.state as PlayerState.Running
    }

    @Test
    fun start_whenIdle_runsFirstStepWithDeadline() {
        val transition = RoutinePlayerReducer.reduce(
            PlayerState.Idle,
            PlayerEvent.Start(routine, RunId("run-1"), at(1_000)),
        )

        val running = assertIs<PlayerState.Running>(transition.state)
        assertEquals(0, running.session.stepIndex)
        assertEquals(11_000, running.stepEndsAtMono)
        assertEquals(
            listOf(
                PlayerEffect.RunStarted(running.session),
                PlayerEffect.StepStarted(running.session),
                PlayerEffect.PlayCues(routine.steps[0].cues),
                PlayerEffect.ScheduleStepEnd(11_000),
            ),
            transition.effects,
        )
    }

    @Test
    fun start_whenAlreadyRunning_isIgnored() {
        val running = started()

        val transition = RoutinePlayerReducer.reduce(
            running,
            PlayerEvent.Start(routine, RunId("run-2"), at(500)),
        )

        assertSame(running, transition.state)
        assertEquals(emptyList(), transition.effects)
    }

    @Test
    fun pause_whenRunning_keepsRemainingTime() {
        val transition = RoutinePlayerReducer.reduce(started(), PlayerEvent.Pause(at(4_000)))

        val paused = assertIs<PlayerState.Paused>(transition.state)
        assertEquals(6.seconds, paused.remaining)
        assertEquals(listOf(PlayerEffect.CancelStepEnd), transition.effects)
    }

    @Test
    fun resume_afterPause_schedulesDeadlineFromRemainingTime() {
        val paused = RoutinePlayerReducer.reduce(started(), PlayerEvent.Pause(at(4_000))).state

        val transition = RoutinePlayerReducer.reduce(paused, PlayerEvent.Resume(at(60_000)))

        val running = assertIs<PlayerState.Running>(transition.state)
        assertEquals(66_000, running.stepEndsAtMono)
        assertEquals(listOf(PlayerEffect.ScheduleStepEnd(66_000)), transition.effects)
    }

    @Test
    fun stepElapsed_beforeDeadline_reschedulesSameDeadline() {
        val running = started()

        val transition = RoutinePlayerReducer.reduce(running, PlayerEvent.StepElapsed(at(9_999)))

        assertSame(running, transition.state)
        assertEquals(listOf(PlayerEffect.ScheduleStepEnd(10_000)), transition.effects)
    }

    @Test
    fun stepElapsed_atDeadline_advancesAndCountsCompletedStep() {
        val transition = RoutinePlayerReducer.reduce(started(), PlayerEvent.StepElapsed(at(10_000)))

        val running = assertIs<PlayerState.Running>(transition.state)
        assertEquals(1, running.session.stepIndex)
        assertEquals(1, running.session.completedSteps)
        assertEquals(30_000, running.stepEndsAtMono)
    }

    @Test
    fun stepElapsed_onLastStep_finishesWithCompletionCues() {
        val onLastStep = RoutinePlayerReducer.reduce(started(), PlayerEvent.StepElapsed(at(10_000))).state

        val transition = RoutinePlayerReducer.reduce(onLastStep, PlayerEvent.StepElapsed(at(30_000)))

        val finished = assertIs<PlayerState.Finished>(transition.state)
        assertEquals(RunOutcome.COMPLETED, finished.summary.outcome)
        assertEquals(2, finished.summary.completedSteps)
        assertEquals(
            listOf(
                PlayerEffect.CancelStepEnd,
                PlayerEffect.PlayCues(RoutinePlayerReducer.CompletionCues),
                PlayerEffect.RunFinished(finished.summary),
            ),
            transition.effects,
        )
    }

    @Test
    fun skipStep_whenPaused_staysPausedWithFullNextStep() {
        val paused = RoutinePlayerReducer.reduce(started(), PlayerEvent.Pause(at(4_000))).state

        val transition = RoutinePlayerReducer.reduce(paused, PlayerEvent.SkipStep(at(5_000)))

        val next = assertIs<PlayerState.Paused>(transition.state)
        assertEquals(1, next.session.stepIndex)
        assertEquals(1, next.session.skippedSteps)
        assertEquals(20.seconds, next.remaining)
    }

    @Test
    fun skipStep_whenRunning_startsNextStepNow() {
        val transition = RoutinePlayerReducer.reduce(started(), PlayerEvent.SkipStep(at(4_000)))

        val running = assertIs<PlayerState.Running>(transition.state)
        assertEquals(1, running.session.stepIndex)
        assertEquals(1, running.session.skippedSteps)
        assertEquals(0, running.session.completedSteps)
        assertEquals(24_000, running.stepEndsAtMono)
        assertEquals(
            listOf(
                PlayerEffect.StepStarted(running.session),
                PlayerEffect.PlayCues(routine.steps[1].cues),
                PlayerEffect.ScheduleStepEnd(24_000),
            ),
            transition.effects,
        )
    }

    @Test
    fun skipStep_onLastStep_finishesAsCompleted() {
        val onLastStep = RoutinePlayerReducer.reduce(started(), PlayerEvent.SkipStep(at(1_000))).state

        val transition = RoutinePlayerReducer.reduce(onLastStep, PlayerEvent.SkipStep(at(2_000)))

        val finished = assertIs<PlayerState.Finished>(transition.state)
        assertEquals(RunOutcome.COMPLETED, finished.summary.outcome)
        assertEquals(2, finished.summary.skippedSteps)
    }

    @Test
    fun stop_whenPaused_finishesAsStopped() {
        val paused = RoutinePlayerReducer.reduce(started(), PlayerEvent.Pause(at(4_000))).state

        val transition = RoutinePlayerReducer.reduce(paused, PlayerEvent.Stop(at(9_000)))

        val finished = assertIs<PlayerState.Finished>(transition.state)
        assertEquals(RunOutcome.STOPPED, finished.summary.outcome)
        assertEquals(testInstant + 9.seconds, finished.summary.finishedAt)
    }

    @Test
    fun commands_inWrongState_areIgnored() {
        val running = started()
        val paused = RoutinePlayerReducer.reduce(running, PlayerEvent.Pause(at(4_000))).state
        val ignored = listOf(
            running to PlayerEvent.Resume(at(5_000)),
            paused to PlayerEvent.Pause(at(5_000)),
            paused to PlayerEvent.StepElapsed(at(10_000)),
            PlayerState.Idle to PlayerEvent.Pause(at(5_000)),
            PlayerState.Idle to PlayerEvent.SkipStep(at(5_000)),
            PlayerState.Idle to PlayerEvent.Stop(at(5_000)),
        )

        ignored.forEach { (state, event) ->
            val transition = RoutinePlayerReducer.reduce(state, event)
            assertSame(state, transition.state, "$event in $state")
            assertEquals(emptyList(), transition.effects, "$event in $state")
        }
    }

    @Test
    fun stop_whenRunning_finishesAsStoppedWithoutCompletionCues() {
        val transition = RoutinePlayerReducer.reduce(started(), PlayerEvent.Stop(at(3_000)))

        val finished = assertIs<PlayerState.Finished>(transition.state)
        assertEquals(RunOutcome.STOPPED, finished.summary.outcome)
        assertEquals(testInstant + 3.seconds, finished.summary.finishedAt)
        assertEquals(routine.name, finished.summary.routineName)
        assertEquals(
            listOf(PlayerEffect.CancelStepEnd, PlayerEffect.RunFinished(finished.summary)),
            transition.effects,
        )
    }

    @Test
    fun reset_onlyLeavesFinished() {
        val finished = RoutinePlayerReducer.reduce(started(), PlayerEvent.Stop(at(3_000))).state
        val running = started()

        assertEquals(PlayerState.Idle, RoutinePlayerReducer.reduce(finished, PlayerEvent.Reset).state)
        assertSame(running, RoutinePlayerReducer.reduce(running, PlayerEvent.Reset).state)
    }
}
