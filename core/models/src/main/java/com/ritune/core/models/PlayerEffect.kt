package com.ritune.core.models

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
