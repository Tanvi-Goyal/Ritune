package com.ritune.core.domain.usecases.coach.tools

import com.ritune.core.domain.models.rhythm.WeekRhythm
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.minus
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.math.roundToInt

// The JSON the model reads for get_week_rhythm. Snake case matches the tool's own names.

@Serializable
internal data class WeekRhythmResult(
    val week: String,
    @SerialName("has_enough_data") val hasEnoughData: Boolean,
    @SerialName("logged_days_last_21") val loggedDays: Int,
    @SerialName("consistency_percent") val consistencyPercent: Int?,
    @SerialName("steadiest_anchor") val steadiestAnchor: String?,
    val anchors: List<AnchorResult>,
    val days: List<DayResult>,
)

@Serializable
internal data class AnchorResult(
    val id: String,
    val name: String,
    @SerialName("usual_time") val usualTime: String?,
    @SerialName("target_time") val targetTime: String?,
    @SerialName("drift_minutes") val driftMinutes: Int?,
    @SerialName("logs_this_week") val logsThisWeek: Int,
)

@Serializable
internal data class DayResult(
    val date: String,
    @SerialName("steady_percent") val steadyPercent: Int?,
)

// Same rounding as the Rhythm screen, so both show the same numbers.
private fun Float.percent() = (this * 100).roundToInt()

internal fun WeekRhythm.toResult(weekEnd: LocalDate) = WeekRhythmResult(
    week = "${weekEnd.minus(6, DateTimeUnit.DAY)}..$weekEnd",
    hasEnoughData = hasEnoughData,
    loggedDays = loggedDays,
    consistencyPercent = consistency?.percent(),
    steadiestAnchor = steadiest?.name,
    anchors = anchors.map {
        AnchorResult(
            id = it.anchor.id.value,
            name = it.anchor.name,
            usualTime = it.medianMinutes?.let { minutes -> LocalTime(minutes / 60, minutes % 60) }
                ?.toString(),
            targetTime = it.anchor.targetTime?.toString(),
            driftMinutes = it.driftMinutes,
            logsThisWeek = it.logsThisWeek,
        )
    },
    days = days.map { DayResult(it.day.toString(), it.fraction?.percent()) },
)
