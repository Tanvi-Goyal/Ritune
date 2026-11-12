package com.ritune.core.testing.evals

import com.ritune.core.models.Anchor
import com.ritune.core.models.AnchorEvent
import com.ritune.core.models.DefaultCatalog
import com.ritune.core.models.EventId
import com.ritune.core.models.EventSource
import com.ritune.core.models.Routine
import com.ritune.core.models.RunOutcome
import com.ritune.core.models.RunSummary
import com.ritune.core.testing.data.aRunSummary
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.toInstant
import kotlin.time.Duration

/** The synthetic data one scenario starts from. Everything is in UTC so runs are repeatable. */
data class EvalWorld(
    val now: LocalDateTime = EVAL_NOW,
    val anchors: List<Anchor> = DefaultCatalog.anchors,
    val events: List<AnchorEvent> = emptyList(),
    val routines: List<Routine> = DefaultCatalog.routines,
    val runs: List<RunSummary> = emptyList(),
)

/** A Wednesday morning, after the wake and morning light logs. */
val EVAL_NOW = LocalDateTime(2026, 11, 11, 8, 30)

private val jitter = listOf(0, 4, -3, 6, -5, 2, -2)

/** One log a day for [days] days, a few minutes either side of [usual]. */
fun steadyLogs(
    anchor: Anchor,
    usual: LocalTime = checkNotNull(anchor.targetTime),
    days: Int = 21,
    now: LocalDateTime = EVAL_NOW,
): List<AnchorEvent> = logs(anchor, days, now) { daysAgo ->
    usual.plusMinutes(jitter[daysAgo % jitter.size])
}

/** One log a day, at the time [at] returns for that many days ago, or none when it returns null. */
fun logs(
    anchor: Anchor,
    days: Int,
    now: LocalDateTime = EVAL_NOW,
    at: (daysAgo: Int) -> LocalTime?,
): List<AnchorEvent> = (0 until days).mapNotNull { daysAgo ->
    val date = now.date.minus(daysAgo, DateTimeUnit.DAY)
    val time = at(daysAgo) ?: return@mapNotNull null
    val local = LocalDateTime(date, time)
    if (local > now) return@mapNotNull null
    AnchorEvent(
        id = EventId("${anchor.id.value}-$date"),
        anchorId = anchor.id,
        occurredAt = local.toInstant(TimeZone.UTC),
        utcOffsetSeconds = 0,
        source = EventSource.MANUAL,
    )
}

fun aRun(
    routine: Routine,
    daysAgo: Int,
    at: LocalTime,
    finished: Boolean = true,
    stepsCompleted: Int = if (finished) routine.steps.size else routine.steps.size / 2,
    now: LocalDate = EVAL_NOW.date,
    length: Duration = if (finished) routine.totalDuration else routine.totalDuration / 2,
): RunSummary {
    val start = LocalDateTime(now.minus(daysAgo, DateTimeUnit.DAY), at).toInstant(TimeZone.UTC)
    return aRunSummary(
        routine = routine,
        runId = "${routine.id.value}-$daysAgo",
        startedAt = start,
        finishedAt = start + length,
        outcome = if (finished) RunOutcome.COMPLETED else RunOutcome.STOPPED,
        completedSteps = stepsCompleted,
        skippedSteps = 0,
    )
}

private fun LocalTime.plusMinutes(minutes: Int): LocalTime {
    val total = (hour * 60 + minute + minutes).mod(24 * 60)
    return LocalTime(total / 60, total % 60)
}
