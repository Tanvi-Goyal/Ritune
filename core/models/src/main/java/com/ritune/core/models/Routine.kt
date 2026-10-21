package com.ritune.core.models

import kotlin.time.Duration

data class Routine(
    val id: RoutineId,
    val name: String,
    val steps: List<Step>,
    val anchorId: AnchorId? = null,
    val builtIn: Boolean = false,
) {
    init {
        require(steps.isNotEmpty()) { "A routine needs at least one step" }
    }

    val totalDuration: Duration
        get() = steps.fold(Duration.ZERO) { total, step -> total + step.duration }
}