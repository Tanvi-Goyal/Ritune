package com.ritune.core.domain.usecases.coach.tools

import com.ritune.core.domain.usecases.anchor.ObserveAnchors
import com.ritune.core.models.coach.CoachTool
import com.ritune.core.models.coach.ToolCall
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class AnchorsTool @Inject constructor(
    private val observeAnchors: ObserveAnchors,
) : ReadTool {

    override val definition = DEFINITION

    override suspend fun run(call: ToolCall): String =
        toolJson.encodeToString(observeAnchors().first().toResult())

    companion object {
        val DEFINITION = CoachTool(
            name = "get_anchors",
            description = "The user's anchors (daily moments they log, such as waking up) with " +
                "their ids and target times. Use the ids with get_anchor_events.",
        )
    }
}
