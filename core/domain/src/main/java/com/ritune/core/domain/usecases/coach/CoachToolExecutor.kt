package com.ritune.core.domain.usecases.coach

import com.ritune.core.domain.usecases.coach.tools.AnchorEventsTool
import com.ritune.core.domain.usecases.coach.tools.AnchorsTool
import com.ritune.core.domain.usecases.coach.tools.ProposeAnchorTargetTool
import com.ritune.core.domain.usecases.coach.tools.ProposeLogCorrectionTool
import com.ritune.core.domain.usecases.coach.tools.ProposeRoutineTool
import com.ritune.core.domain.usecases.coach.tools.RoutinesTool
import com.ritune.core.domain.usecases.coach.tools.RunHistoryTool
import com.ritune.core.domain.usecases.coach.tools.ToolArgumentException
import com.ritune.core.domain.usecases.coach.tools.WeekRhythmTool
import com.ritune.core.models.coach.Proposal
import com.ritune.core.models.coach.ProposalOutcome
import com.ritune.core.models.coach.ToolCall
import com.ritune.core.models.coach.ToolResult
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import javax.inject.Inject

class CoachToolExecutor @Inject constructor(
    weekRhythm: WeekRhythmTool,
    anchors: AnchorsTool,
    anchorEvents: AnchorEventsTool,
    runHistory: RunHistoryTool,
    routines: RoutinesTool,
    proposeAnchorTarget: ProposeAnchorTargetTool,
    proposeRoutine: ProposeRoutineTool,
    proposeLogCorrection: ProposeLogCorrectionTool,
) {
    private val readTools = listOf(weekRhythm, anchors, anchorEvents, runHistory, routines)
        .associateBy { it.definition.name }
    private val writeTools = listOf(proposeAnchorTarget, proposeRoutine, proposeLogCorrection)
        .associateBy { it.definition.name }

    fun isWriteTool(name: String) = name in writeTools

    suspend fun execute(call: ToolCall): ToolOutcome {
        val write = writeTools[call.name]
        val read = readTools[call.name]
        return try {
            when {
                write != null -> ToolOutcome.NeedsApproval(call, write.propose(call))
                read != null -> ToolOutcome.Done(ToolResult(call, read.run(call)))
                else -> ToolOutcome.Done(error(call, "Unknown tool: ${call.name}"))
            }
        } catch (e: ToolArgumentException) {
            ToolOutcome.Done(error(call, e.message.orEmpty()))
        }
    }

    fun decided(call: ToolCall, outcome: ProposalOutcome) = ToolResult(
        call,
        Json.encodeToString(
            when (outcome) {
                is ProposalOutcome.Applied -> DecisionResult(
                    APPLIED,
                    "Saved. Confirm it to the user in one short sentence.",
                )

                ProposalOutcome.Declined -> DecisionResult(
                    DECLINED,
                    "The user declined. Acknowledge it in one short sentence and " +
                            "don't propose it again unless they ask.",
                )

                ProposalOutcome.NotApplied -> DecisionResult(
                    NOT_APPLIED,
                    "The app couldn't save it. Tell the user in one short sentence.",
                )
            },
        ),
    )

    fun oneAtATime(call: ToolCall) =
        error(call, "Propose one change at a time and wait for the user's answer.")

    private fun error(call: ToolCall, message: String) =
        ToolResult(call, Json.encodeToString(ErrorResult(message)))

    private companion object {
        const val APPLIED = "applied"
        const val DECLINED = "declined"
        const val NOT_APPLIED = "not_applied"
    }
}

sealed interface ToolOutcome {
    data class Done(val result: ToolResult) : ToolOutcome
    data class NeedsApproval(val call: ToolCall, val proposal: Proposal) : ToolOutcome
}

@Serializable
private data class ErrorResult(val error: String)

@Serializable
private data class DecisionResult(val status: String, val note: String)
