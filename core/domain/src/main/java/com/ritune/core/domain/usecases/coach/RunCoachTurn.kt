package com.ritune.core.domain.usecases.coach

import com.ritune.core.domain.ports.CoachGateway
import com.ritune.core.models.coach.CoachError
import com.ritune.core.models.coach.CoachEvent
import com.ritune.core.models.coach.CoachException
import com.ritune.core.models.coach.CoachInput
import com.ritune.core.models.coach.CoachUsage
import com.ritune.core.models.coach.ConversationId
import com.ritune.core.models.coach.ModelEvent
import com.ritune.core.models.coach.ToolCall
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException

/**
 * One coach turn, the agent loop: send the message, run any tools the model asks for, send their
 * results back, and repeat until the model answers with text only.
 */
class RunCoachTurn @Inject constructor(
    private val gateway: CoachGateway,
    private val tools: CoachToolExecutor,
) {
    operator fun invoke(
        conversation: ConversationId,
        message: String
    ): Flow<CoachEvent> = flow {
        var input: CoachInput = CoachInput.UserMessage(message)
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
            val results = calls.map { call ->
                emit(CoachEvent.ToolRunning(call.name))
                tools.execute(call)
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
