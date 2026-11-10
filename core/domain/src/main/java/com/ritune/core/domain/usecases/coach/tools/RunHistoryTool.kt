package com.ritune.core.domain.usecases.coach.tools

import com.ritune.core.domain.repositories.RunHistoryRepository
import com.ritune.core.models.Timekeeper
import com.ritune.core.models.coach.CoachTool
import com.ritune.core.models.coach.ToolCall
import com.ritune.core.models.coach.ToolParameter
import com.ritune.core.models.coach.ToolParameterType
import kotlinx.coroutines.flow.first
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.minus
import kotlinx.datetime.toLocalDateTime
import javax.inject.Inject

class RunHistoryTool @Inject constructor(
    private val history: RunHistoryRepository,
    private val timekeeper: Timekeeper,
) : ReadTool {

    override val definition = DEFINITION

    override suspend fun run(call: ToolCall): String {
        val days = call.intArgument(DAYS, 1..MAX_DAYS, default = 7)
        val zone = timekeeper.timeZone()
        val today = timekeeper.now().wall.toLocalDateTime(zone).date
        val from = today.minus(days - 1, DateTimeUnit.DAY).atStartOfDayIn(zone)
        val runs = history.observeSince(from).first()
        return toolJson.encodeToString(runs.toResult(days, zone, MAX_RUNS))
    }

    companion object {
        const val MAX_DAYS = 60

        // Keeps the result, and the tokens it costs, small for people who run many routines.
        const val MAX_RUNS = 50
        private const val DAYS = "days"

        val DEFINITION = CoachTool(
            name = "get_run_history",
            description = "Routines the user ran over the last few days, newest first: when, how " +
                "long, whether they finished or stopped early, and how many steps they completed " +
                "or skipped.",
            parameters = listOf(
                ToolParameter(
                    name = DAYS,
                    description = "How many days back to read, including today, 1 to $MAX_DAYS. " +
                        "Defaults to 7.",
                    type = ToolParameterType.INTEGER,
                    required = false,
                ),
            ),
        )
    }
}
