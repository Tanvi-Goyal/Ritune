package com.ritune.core.domain.usecases.coach.tools

import com.ritune.core.models.RunOutcome
import com.ritune.core.models.RunSummary
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class RunHistoryResult(
    val days: Int,
    @SerialName("total_runs") val totalRuns: Int,
    @SerialName("finished_runs") val finishedRuns: Int,
    @SerialName("stopped_runs") val stoppedRuns: Int,
    val runs: List<RunItem>,
)

@Serializable
internal data class RunItem(
    val routine: String,
    val date: String,
    @SerialName("start_time") val startTime: String,
    val duration: String,
    val finished: Boolean,
    @SerialName("steps_completed") val stepsCompleted: Int,
    @SerialName("steps_skipped") val stepsSkipped: Int,
    @SerialName("steps_total") val stepsTotal: Int,
)

internal fun List<RunSummary>.toResult(days: Int, zone: TimeZone, maxRuns: Int) = RunHistoryResult(
    days = days,
    totalRuns = size,
    finishedRuns = count { it.outcome == RunOutcome.COMPLETED },
    stoppedRuns = count { it.outcome == RunOutcome.STOPPED },
    runs = take(maxRuns).map { run ->
        val start = run.startedAt.toLocalDateTime(zone)
        RunItem(
            routine = run.routineName,
            date = start.date.toString(),
            startTime = start.time.toString().take(5),
            duration = (run.finishedAt - run.startedAt).toToolText(),
            finished = run.outcome == RunOutcome.COMPLETED,
            stepsCompleted = run.completedSteps,
            stepsSkipped = run.skippedSteps,
            stepsTotal = run.totalSteps,
        )
    },
)
