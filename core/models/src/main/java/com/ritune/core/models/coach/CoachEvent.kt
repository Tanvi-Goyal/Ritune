package com.ritune.core.models.coach

data class ConversationId(val value: String)

sealed interface CoachEvent {
    data class TextDelta(val text: String) : CoachEvent
    data class ToolRunning(val tool: String) : CoachEvent
    data class ProposalResolved(val outcome: ProposalOutcome) : CoachEvent
    data class Done(val usage: CoachUsage) : CoachEvent
    data class Failed(val error: CoachError) : CoachEvent
}

enum class CoachError {
    QUOTA_EXCEEDED,
    NETWORK,
    BLOCKED,
    TOO_MANY_TOOL_CALLS,
    UNKNOWN,
}

data class CoachUsage(
    val inputTokens: Int = 0,
    val outputTokens: Int = 0,
    val thinkingTokens: Int = 0,
) {
    operator fun plus(other: CoachUsage) = CoachUsage(
        inputTokens = inputTokens + other.inputTokens,
        outputTokens = outputTokens + other.outputTokens,
        thinkingTokens = thinkingTokens + other.thinkingTokens,
    )
}

class CoachException(
    val error: CoachError,
    cause: Throwable? = null
) : Exception(error.name, cause)
