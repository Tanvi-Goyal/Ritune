package com.ritune.core.models

import kotlin.time.Duration

data class Step(
    val id: StepId,
    val title: String,
    val duration: Duration,
    val instruction: String? = null,
    val cues: List<Cue> = emptyList(),
) {
    init {
        require(duration.isPositive()) { "Step duration must be positive" }
    }
}
