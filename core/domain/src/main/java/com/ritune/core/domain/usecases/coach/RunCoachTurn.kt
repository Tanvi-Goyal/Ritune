package com.ritune.core.domain.usecases.coach

import com.ritune.core.domain.ports.CoachGateway
import com.ritune.core.models.coach.CoachError
import com.ritune.core.models.coach.CoachEvent
import com.ritune.core.models.coach.CoachException
import com.ritune.core.models.coach.CoachInput
import com.ritune.core.models.coach.CoachUsage
import com.ritune.core.models.coach.ConversationId
import com.ritune.core.models.coach.Decision
import com.ritune.core.models.coach.ModelEvent
import com.ritune.core.models.coach.Proposal
import com.ritune.core.models.coach.ProposalOutcome
import com.ritune.core.models.coach.ToolCall
import com.ritune.core.models.coach.ToolResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException

class RunCoachTurn @Inject constructor(
    private val gateway: CoachGateway,
    private val tools: CoachToolExecutor,
    private val applyProposal: ApplyCoachProposal,
) {
    operator fun invoke(
        conversation: ConversationId,
        message: String,
        approve: suspend (Proposal) -> Decision,
        undone: List<Proposal> = emptyList(),
    ): Flow<CoachEvent> = flow {
        var input: CoachInput = CoachInput.UserMessage(
            CoachNotes.withUndone(message, undone)
        )
        var usage = CoachUsage()
        var toolRounds = 0
        while (true) {
            val calls = mutableListOf<ToolCall>()
            gateway.send(conversation, CoachTools.setup, input).collect { event ->
                when (event) {
                    is ModelEvent.Text -> emit(CoachEvent.TextDelta(event.text))
                    is ModelEvent.ToolCallRequest -> calls += event.call
                    is ModelEvent.Finished -> usage += event.usage
                }
            }
            if (calls.isEmpty()) {
                emit(CoachEvent.Done(usage))
                return@flow
            }

            if (toolRounds == MAX_TOOL_ROUNDS) {
                emit(CoachEvent.Failed(CoachError.TOO_MANY_TOOL_CALLS))
                return@flow
            }
            toolRounds++
            val results = mutableListOf<ToolResult>()
            var proposed = false
            for (call in calls) {
                val write = tools.isWriteTool(call.name)
                if (write && proposed) {
                    results += tools.oneAtATime(call)
                    continue
                }
                if (!write) emit(CoachEvent.ToolRunning(call.name))
                when (val outcome = tools.execute(call)) {
                    is ToolOutcome.Done -> results += outcome.result
                    is ToolOutcome.NeedsApproval -> {
                        proposed = true
                        val decided = when (approve(outcome.proposal)) {
                            Decision.APPLY -> applyProposal(outcome.proposal)
                            Decision.DECLINE -> ProposalOutcome.Declined
                        }
                        emit(CoachEvent.ProposalResolved(decided))
                        results += tools.decided(call, decided)
                    }
                }
            }
            input = CoachInput.ToolResults(results)
        }
    }.catch { e ->
        if (e is CancellationException) throw e
        emit(
            CoachEvent.Failed(
                (e as? CoachException)
                    ?.error ?: CoachError.UNKNOWN
            )
        )
    }

    companion object {
        const val MAX_TOOL_ROUNDS = 6
    }
}
