package com.ritune.core.data.mapper

import com.ritune.core.database.entities.RunEntity
import com.ritune.core.models.RunSummary

internal fun RunSummary.toEntity(routineName: String) = RunEntity(
    runId = runId.id,
    routineId = routineId.id,
    routineName = routineName,
    startedAt = startedAt.toEpochMilliseconds(),
    finishedAt = finishedAt.toEpochMilliseconds(),
    outcome = outcome.name,
    completedSteps = completedSteps,
    skippedSteps = skippedSteps,
    totalSteps = totalSteps,
)