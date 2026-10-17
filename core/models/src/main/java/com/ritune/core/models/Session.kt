package com.ritune.core.models

import kotlin.time.Instant

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