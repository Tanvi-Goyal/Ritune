package com.ritune.core.models

import kotlin.time.Instant

data class RunSummary(
    val runId: RunId,
    val routineId: RoutineId,
    val startedAt: Instant,
    val finishedAt: Instant,
    val outcome: RunOutcome,
    val completedSteps: Int,
    val skippedSteps: Int,
    val totalSteps: Int,
)

enum class RunOutcome { COMPLETED, STOPPED }