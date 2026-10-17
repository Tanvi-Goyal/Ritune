package com.ritune.core.models.player

import com.ritune.core.models.RunSummary
import com.ritune.core.models.Session

sealed interface PlayerEffect {
    data class RunStarted(val session: Session) : PlayerEffect
    data class StepStarted(val session: Session) : PlayerEffect
    data class ScheduleStepEnd(val deadlineMono: Long) : PlayerEffect
    data object CancelStepEnd : PlayerEffect
    data class RunFinished(val summary: RunSummary) : PlayerEffect
}

data class Transition(
    val state: PlayerState,
    val effects: List<PlayerEffect> = emptyList(),
)
