package com.ritune.core.data.mapper

import com.ritune.core.database.entities.RunEntity
import com.ritune.core.models.RoutineId
import com.ritune.core.models.RunId
import com.ritune.core.models.RunSummary
import kotlin.time.Instant

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

fun RunEntity.toModel() = RunSummary(
    runId = RunId(runId),
    routineId = RoutineId(routineId),
    startedAt = Instant.fromEpochMilliseconds(startedAt),
    finishedAt = Instant.fromEpochMilliseconds(finishedAt),
    outcome = com.ritune.core.models.RunOutcome.valueOf(outcome),
    completedSteps = completedSteps,
    skippedSteps = skippedSteps,
    totalSteps = totalSteps
)