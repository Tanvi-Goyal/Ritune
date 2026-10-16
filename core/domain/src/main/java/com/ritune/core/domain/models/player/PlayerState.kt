package com.ritune.core.domain.models.player

import com.ritune.core.models.Routine
import com.ritune.core.models.RunId
import com.ritune.core.models.RunSummary
import com.ritune.core.models.Step
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Instant

/**
 * The whole player as data. Time is stored as an absolute monotonic deadline while running
 * and as a remaining duration while paused. Nothing ever "ticks".
 */
sealed interface PlayerState {
    data object Idle : PlayerState
    data class Running(val session: Session, val stepEndsAtMono: Long) : PlayerState
    data class Paused(val session: Session, val remaining: Duration) : PlayerState
    data class Finished(val summary: RunSummary) : PlayerState
}

data class Session(
    val runId: RunId,
    val routine: Routine,
    val stepIndex: Int,
    val startedAt: Instant,
    val completedSteps: Int = 0,
    val skippedSteps: Int = 0,
) {
    val currentStep: Step get() = routine.steps[stepIndex]
    val isLastStep: Boolean get() = stepIndex == routine.steps.lastIndex
}

/** UI helper: call from a render loop with the current monotonic time. */
fun PlayerState.remainingAt(nowMono: Long): Duration = when (this) {
    is PlayerState.Running -> (stepEndsAtMono - nowMono)
        .coerceAtLeast(0).milliseconds

    is PlayerState.Paused -> remaining
    else -> Duration.ZERO
}