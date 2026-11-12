package com.ritune.core.domain.usecases.coach

import com.ritune.core.domain.usecases.coach.tools.AnchorEventsTool
import com.ritune.core.domain.usecases.coach.tools.AnchorsTool
import com.ritune.core.domain.usecases.coach.tools.ProposeAnchorTargetTool
import com.ritune.core.domain.usecases.coach.tools.ProposeLogCorrectionTool
import com.ritune.core.domain.usecases.coach.tools.ProposeRoutineTool
import com.ritune.core.domain.usecases.coach.tools.RoutinesTool
import com.ritune.core.domain.usecases.coach.tools.RunHistoryTool
import com.ritune.core.domain.usecases.coach.tools.WeekRhythmTool
import com.ritune.core.models.coach.CoachSetup

object CoachTools {
    val all = listOf(
        WeekRhythmTool.DEFINITION,
        AnchorsTool.DEFINITION,
        AnchorEventsTool.DEFINITION,
        RunHistoryTool.DEFINITION,
        RoutinesTool.DEFINITION,
        ProposeAnchorTargetTool.DEFINITION,
        ProposeRoutineTool.DEFINITION,
        ProposeLogCorrectionTool.DEFINITION,
    )

    val setup = CoachSetup(instructions = CoachInstructions.text, tools = all)
}
