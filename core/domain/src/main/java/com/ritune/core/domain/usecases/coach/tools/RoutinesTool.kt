package com.ritune.core.domain.usecases.coach.tools

import com.ritune.core.domain.usecases.anchor.ObserveAnchors
import com.ritune.core.domain.usecases.routine.ObserveRoutines
import com.ritune.core.models.coach.CoachTool
import com.ritune.core.models.coach.ToolCall
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class RoutinesTool @Inject constructor(
    private val observeRoutines: ObserveRoutines,
    private val observeAnchors: ObserveAnchors,
) : ReadTool {

    override val definition = DEFINITION

    override suspend fun run(call: ToolCall): String {
        val anchorNames = observeAnchors().first().associate { it.id to it.name }
        return toolJson.encodeToString(observeRoutines().first().toResult(anchorNames))
    }

    companion object {
        val DEFINITION = CoachTool(
            name = "get_routines",
            description = "The user's routines: each one's steps with their durations, the total " +
                "length, and the anchor it belongs to, if any.",
        )
    }
}
