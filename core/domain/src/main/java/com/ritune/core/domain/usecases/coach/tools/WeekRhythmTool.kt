package com.ritune.core.domain.usecases.coach.tools

import com.ritune.core.domain.usecases.rhythm.ObserveCurrentDay
import com.ritune.core.domain.usecases.rhythm.ObserveWeekRhythm
import com.ritune.core.models.coach.CoachTool
import com.ritune.core.models.coach.ToolCall
import com.ritune.core.models.coach.ToolParameter
import com.ritune.core.models.coach.ToolParameterType
import kotlinx.coroutines.flow.first
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.minus
import javax.inject.Inject

/** Reads the same use case as the Rhythm screen, so the coach and the screen always agree. */
class WeekRhythmTool @Inject constructor(
    private val observeCurrentDay: ObserveCurrentDay,
    private val observeWeekRhythm: ObserveWeekRhythm,
) : ReadTool {

    override val definition = DEFINITION

    override suspend fun run(call: ToolCall): String {
        val offset = call.intArgument(
            WEEK_OFFSET,
            0..MAX_WEEK_OFFSET, default = 0
        )
        val weekEnd = observeCurrentDay().first()
            .minus(offset * 7, DateTimeUnit.DAY)
        val rhythm = observeWeekRhythm(weekEnd).first()
        return toolJson.encodeToString(rhythm.toResult(weekEnd))
    }

    companion object {
        const val MAX_WEEK_OFFSET = 12
        private const val WEEK_OFFSET = "week_offset"

        val DEFINITION = CoachTool(
            name = "get_week_rhythm",
            description = "The user's rhythm for one week: overall consistency, " +
                    "each anchor's usual time, target and drift, and each day's share of steady " +
                    "logs. Numbers are computed by the app; quote them as given.",
            parameters = listOf(
                ToolParameter(
                    name = WEEK_OFFSET,
                    description = "0 for the week ending today, 1 for the week before, " +
                            "up to $MAX_WEEK_OFFSET.",
                    type = ToolParameterType.INTEGER,
                    required = false,
                ),
            ),
        )
    }
}
