package com.ritune.core.domain.usecases.player

import com.ritune.core.models.Moment
import com.ritune.core.models.player.PlayerEffect
import com.ritune.core.models.player.PlayerEvent
import com.ritune.core.models.player.PlayerState
import com.ritune.core.models.RunOutcome
import com.ritune.core.models.RunSummary
import com.ritune.core.models.Session
import com.ritune.core.models.player.Transition
import kotlin.time.Duration.Companion.milliseconds

object RoutinePlayerReducer {
    fun reduce(state: PlayerState, event: PlayerEvent): Transition {
        return when (event) {
            is PlayerEvent.Start -> start(state, event)
            is PlayerEvent.Pause -> pause(state, event)
            is PlayerEvent.Resume -> resume(state, event)
            is PlayerEvent.SkipStep -> skipStep(state, event)
            is PlayerEvent.StepElapsed -> stepElapsed(state, event)
            is PlayerEvent.Stop -> stop(state, event)
            is PlayerEvent.Reset -> reset(state)
        }
    }

    private fun ignore(state: PlayerState) = Transition(state)

    private fun start(state: PlayerState, event: PlayerEvent.Start): Transition {
        if (state is PlayerState.Running || state is PlayerState.Paused) return ignore(state)

        val session = Session(
            runId = event.runId,
            routine = event.routine,
            stepIndex = 0,
            startedAt = event.at.wall,
        )

        val deadline = event.at.monoMillis + session.currentStep.duration.inWholeMilliseconds

        return Transition(
            state = PlayerState.Running(session, deadline),
            effects = listOf(
                PlayerEffect.RunStarted(session),
                PlayerEffect.StepStarted(session),
                PlayerEffect.ScheduleStepEnd(deadline),
            )
        )
    }

    private fun pause(state: PlayerState, event: PlayerEvent.Pause): Transition {
        if (state !is PlayerState.Running) return ignore(state)

        val session = state.session
        val remaining = (state.stepEndsAtMono - event.at.monoMillis)
            .coerceAtLeast(0)
            .milliseconds

        return Transition(
            state = PlayerState.Paused(session, remaining),
            effects = listOf(
                PlayerEffect.CancelStepEnd,
            )
        )
    }

    private fun resume(state: PlayerState, event: PlayerEvent.Resume): Transition {
        if (state !is PlayerState.Paused) return ignore(state)

        val session = state.session
        val deadline = event.at.monoMillis + state.remaining.inWholeMilliseconds

        return Transition(
            state = PlayerState.Running(session, deadline),
            effects = listOf(
                PlayerEffect.ScheduleStepEnd(deadline),
            )
        )
    }

    private fun stepElapsed(state: PlayerState, event: PlayerEvent.StepElapsed): Transition {
        if (state !is PlayerState.Running) return ignore(state)

        if (event.at.monoMillis < state.stepEndsAtMono) {
            return Transition(
                state,
                listOf(PlayerEffect.ScheduleStepEnd(state.stepEndsAtMono))
            )
        }

        return advance(
            session = state.session.copy(
                completedSteps = state.session.completedSteps + 1,
            ),
            at = event.at,
            running = true
        )
    }

    private fun skipStep(state: PlayerState, event: PlayerEvent.SkipStep): Transition {
        return when (state) {
            is PlayerState.Running -> {
                val skippedSteps = state.session.skippedSteps + 1
                val session = state.session.copy(skippedSteps = skippedSteps)
                advance(session, event.at, true)
            }

            is PlayerState.Paused -> {
                val skippedSteps = state.session.skippedSteps + 1
                val session = state.session.copy(skippedSteps = skippedSteps)
                advance(session, event.at, false)
            }

            else -> ignore(state)
        }
    }

    private fun stop(state: PlayerState, event: PlayerEvent.Stop): Transition {
        return when (state) {
            is PlayerState.Running -> {
                finish(state.session, event.at, RunOutcome.STOPPED)
            }

            is PlayerState.Paused -> {
                finish(state.session, event.at, RunOutcome.STOPPED)
            }

            else -> ignore(state)
        }
    }

    private fun reset(state: PlayerState): Transition {
        return if (state is PlayerState.Finished) Transition(PlayerState.Idle)
        else ignore(state)
    }

    private fun advance(session: Session, at: Moment, running: Boolean): Transition {
        if (session.isLastStep) return finish(session, at, RunOutcome.COMPLETED)

        val next = session.copy(
            stepIndex = session.stepIndex + 1,
        )
        val duration = next.currentStep.duration

        return if (running) {
            val deadline = at.monoMillis + duration.inWholeMilliseconds
            Transition(
                state = PlayerState.Running(next, deadline),
                effects = listOf(
                    PlayerEffect.StepStarted(next),
                    PlayerEffect.ScheduleStepEnd(deadline)
                )
            )
        } else {
            Transition(
                state = PlayerState.Paused(next, duration)
            )
        }
    }

    private fun finish(session: Session, at: Moment, outcome: RunOutcome): Transition {
        val summary = RunSummary(
            runId = session.runId,
            routineId = session.routine.id,
            startedAt = session.startedAt,
            finishedAt = at.wall,
            outcome = outcome,
            completedSteps = session.completedSteps,
            skippedSteps = session.skippedSteps,
            totalSteps = session.routine.steps.size,
        )

        return Transition(
            state = PlayerState.Finished(summary),
            effects = listOf(
                PlayerEffect.CancelStepEnd,
                PlayerEffect.RunFinished(summary)
            )
        )
    }
}