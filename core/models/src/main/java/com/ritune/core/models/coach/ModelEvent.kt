package com.ritune.core.models.coach

/** What a gateway sends to the model in one request. */
sealed interface CoachInput {
    data class UserMessage(val text: String) : CoachInput
    data class ToolResults(val results: List<ToolResult>) : CoachInput
}

/** What a gateway streams back from one model request. */
sealed interface ModelEvent {
    data class Text(val text: String) : ModelEvent
    data class ToolCallRequest(val call: ToolCall) : ModelEvent
    data class Finished(val usage: CoachUsage) : ModelEvent
}
