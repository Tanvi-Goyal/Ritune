package com.ritune.core.domain.usecases.coach

import com.ritune.core.domain.usecases.coach.tools.AnchorEventsTool
import com.ritune.core.domain.usecases.coach.tools.AnchorsTool
import com.ritune.core.domain.usecases.coach.tools.RoutinesTool
import com.ritune.core.domain.usecases.coach.tools.RunHistoryTool
import com.ritune.core.domain.usecases.coach.tools.WeekRhythmTool
import com.ritune.core.models.coach.CoachSetup

/** Everything the model is told about the coach: the instructions and every tool it may call. */
object CoachTools {
    val all = listOf(
        WeekRhythmTool.DEFINITION,
        AnchorsTool.DEFINITION,
        AnchorEventsTool.DEFINITION,
        RunHistoryTool.DEFINITION,
        RoutinesTool.DEFINITION,
    )

    val setup = CoachSetup(instructions = CoachInstructions.text, tools = all)
}
