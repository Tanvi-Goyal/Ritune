package com.ritune.core.models

sealed interface PlayerEvent {
    data class Start(val routine: Routine, val runId: RunId, val at: Moment) : PlayerEvent
    data class Pause(val at: Moment) : PlayerEvent
    data class Resume(val at: Moment) : PlayerEvent
    data class SkipStep(val at: Moment) : PlayerEvent
    data class StepElapsed(val at: Moment) : PlayerEvent
    data class Stop(val at: Moment) : PlayerEvent
    data object Reset : PlayerEvent
}