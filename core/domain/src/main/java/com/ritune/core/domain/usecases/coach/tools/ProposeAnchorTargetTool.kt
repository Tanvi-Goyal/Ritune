package com.ritune.core.domain.usecases.coach.tools

import com.ritune.core.domain.usecases.anchor.ObserveAnchors
import com.ritune.core.models.coach.CoachTool
import com.ritune.core.models.coach.Proposal
import com.ritune.core.models.coach.ToolCall
import com.ritune.core.models.coach.ToolParameter
import com.ritune.core.models.coach.ToolParameterType
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class ProposeAnchorTargetTool @Inject constructor(
    private val observeAnchors: ObserveAnchors,
) : WriteTool {

    override val definition = DEFINITION

    override suspend fun propose(call: ToolCall): Proposal {
        val anchorId = call.stringArgument(ANCHOR_ID)
        val anchor = observeAnchors().first().firstOrNull { it.id.value == anchorId }
            ?: throw ToolArgumentException("Unknown $ANCHOR_ID. Call get_anchors for the valid ids.")
        val target = call.timeArgument(TARGET_TIME)
        if (target == anchor.targetTime) {
            throw ToolArgumentException("${anchor.name} already has the target $target.")
        }
        return Proposal.AnchorTarget(anchor.id,
            anchor.name,
            from = anchor.targetTime,
            to = target)
    }

    companion object {
        private const val ANCHOR_ID = "anchor_id"
        private const val TARGET_TIME = "target_time"

        val DEFINITION = CoachTool(
            name = "propose_anchor_target",
            description = "Proposes a new target time for one anchor. The user sees the change " +
                "and taps Apply or Decline; the result says which.",
            parameters = listOf(
                ToolParameter(
                    name = ANCHOR_ID,
                    description = "The anchor's id from get_anchors.",
                    type = ToolParameterType.STRING,
                    required = true,
                ),
                ToolParameter(
                    name = TARGET_TIME,
                    description = "The new target as a 24-hour time, for example 22:00.",
                    type = ToolParameterType.STRING,
                    required = true,
                ),
            ),
        )
    }
}
