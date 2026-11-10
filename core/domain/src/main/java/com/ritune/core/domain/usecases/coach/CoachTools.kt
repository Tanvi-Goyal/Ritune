package com.ritune.core.domain.usecases.coach

import com.ritune.core.models.coach.CoachSetup
import com.ritune.core.models.coach.CoachTool
import com.ritune.core.models.coach.ToolParameter
import com.ritune.core.models.coach.ToolParameterType

object CoachTools {
    const val MAX_WEEK_OFFSET = 12

    val weekRhythm = CoachTool(
        name = "get_week_rhythm",
        description = "The user's rhythm for one week: overall consistency, each anchor's usual " +
            "time, target and drift, and each day's share of steady logs. Numbers are computed " +
            "by the app; quote them as given.",
        parameters = listOf(
            ToolParameter(
                name = "week_offset",
                description = "0 for the week ending today, 1 for the week before, " +
                    "up to $MAX_WEEK_OFFSET.",
                type = ToolParameterType.INTEGER,
                required = false,
            ),
        ),
    )

    val all = listOf(weekRhythm)

    val setup = CoachSetup(instructions = CoachInstructions.text, tools = all)
}
