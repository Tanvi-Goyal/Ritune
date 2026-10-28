package com.ritune.core.data.mapper

import com.ritune.core.database.entities.RunEntity
import com.ritune.core.models.RoutineId
import com.ritune.core.models.RunId
import com.ritune.core.models.RunOutcome
import com.ritune.core.models.RunSummary
import kotlin.time.Instant

fun RunEntity.toModel() = RunSummary(
    runId = RunId(runId),
    routineId = RoutineId(routineId),
    routineName = routineName,
    startedAt = Instant.fromEpochMilliseconds(startedAt),
    finishedAt = Instant.fromEpochMilliseconds(finishedAt),
    outcome = RunOutcome.valueOf(outcome),
    completedSteps = completedSteps,
    skippedSteps = skippedSteps,
    totalSteps = totalSteps
)

internal fun RunSummary.toEntity() = RunEntity(
    runId = runId.value,
    routineId = routineId.value,
    routineName = routineName,
    startedAt = startedAt.toEpochMilliseconds(),
    finishedAt = finishedAt.toEpochMilliseconds(),
    outcome = outcome.name,
    completedSteps = completedSteps,
    skippedSteps = skippedSteps,
    totalSteps = totalSteps,
)