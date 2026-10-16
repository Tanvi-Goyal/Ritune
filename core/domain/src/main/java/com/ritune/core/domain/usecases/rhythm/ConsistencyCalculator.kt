package com.ritune.core.domain.usecases.rhythm

import com.ritune.core.domain.models.rhythm.AnchorRhythm
import com.ritune.core.domain.models.rhythm.DayConsistency
import com.ritune.core.domain.models.rhythm.WeekRhythm
import com.ritune.core.models.Anchor
import com.ritune.core.models.AnchorEvent
import com.ritune.core.models.AnchorId
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.roundToInt

/**
 *
 * Window: the 21 days ending on the week's last day.
 * An anchor's usual time is the median of its logs in the window (needs 3 or more).
 * A log counts as steady if it is within 30 minutes of that, measured around the clock.
 *
 * This differs from the PRD wording ("previous 14 days"): that would show nothing for a new user's first three
 * weeks. Using the window that includes the current week gives a score as soon as an anchor has 3 logs.
 */
object ConsistencyCalculator {
    const val TOLERANCE_MINUTES = 30
    const val MIN_LOGS = 3
    const val MIN_DAYS = 3
    private const val DAY_MINUTES = 1_440

    private data class Log(val anchorId: AnchorId, val day: LocalDate, val minutes: Int)

    fun compute(anchors: List<Anchor>, events: List<AnchorEvent>, weekEnd: LocalDate): WeekRhythm {
        val weekStart = weekEnd.minus(6, DateTimeUnit.DAY)
        val windowStart = weekEnd.minus(20, DateTimeUnit.DAY)
        val logs = events.map { it.toLog() }.filter { it.day in windowStart..weekEnd }

        val rhythms = anchors.map { anchor ->
            val mine = logs.filter { it.anchorId == anchor.id }
            val median = if (mine.size >= MIN_LOGS)
                median(mine.map { it.minutes })
            else null
            val thisWeek = mine.filter { it.day in weekStart..weekEnd }
            val drift = if (median != null && thisWeek.isNotEmpty()) {
                thisWeek.map { distance(it.minutes, median) }
                    .average().roundToInt()
            } else {
                null
            }
            AnchorRhythm(
                anchor,
                median,
                drift,
                thisWeek.size
            )
        }

        val medians = rhythms.associate { it.anchor.id to it.medianMinutes }
        val scored = logs.filter { it.day in weekStart..weekEnd }.mapNotNull { log ->
            medians[log.anchorId]?.let { median ->
                log to (distance(
                    log.minutes,
                    median
                ) <= TOLERANCE_MINUTES)
            }
        }

        val days = (0..6).map { offset ->
            val day = weekStart.plus(offset, DateTimeUnit.DAY)
            val ofDay = scored.filter { it.first.day == day }
            DayConsistency(
                day,
                if (ofDay.isEmpty()) null
                else ofDay.count { it.second }.toFloat() / ofDay.size
            )
        }
        val loggedDays = logs.map { it.day }.distinct().size

        return WeekRhythm(
            days = days,
            consistency = if (scored.isEmpty()) null else scored.count { it.second }
                .toFloat() / scored.size,
            steadiest = rhythms.filter { it.driftMinutes != null }
                .minByOrNull { it.driftMinutes!! }?.anchor,
            anchors = rhythms,
            loggedDays = loggedDays,
            hasEnoughData = loggedDays >= MIN_DAYS,
        )
    }

    private fun AnchorEvent.toLog(): Log {
        val local =
            occurredAt.epochSeconds + utcOffsetSeconds

        return Log(
            anchorId = anchorId,
            day = LocalDate.fromEpochDays(local.floorDiv(86_400L).toInt()),
            minutes = (local.mod(86_400L) / 60).toInt(),
        )
    }

    private fun median(values: List<Int>): Int {
        val sorted = values.sorted()
        val n = sorted.size
        return if (n % 2 == 1) sorted[n / 2] else (sorted[n / 2 - 1] + sorted[n / 2]) / 2
    }

    private fun distance(a: Int, b: Int): Int {
        val d = abs(a - b)
        return min(d, DAY_MINUTES - d)
    }
}

