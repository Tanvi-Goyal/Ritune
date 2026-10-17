package com.ritune.core.models.player

import com.ritune.core.models.RunSummary
import com.ritune.core.models.Session
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

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

fun PlayerState.remainingAt(nowMono: Long): Duration = when (this) {
    is PlayerState.Running -> (stepEndsAtMono - nowMono)
        .coerceAtLeast(0).milliseconds

    is PlayerState.Paused -> remaining
    else -> Duration.ZERO
}