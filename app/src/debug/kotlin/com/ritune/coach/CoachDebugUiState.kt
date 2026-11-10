package com.ritune.coach

import com.ritune.core.models.coach.CoachError
import com.ritune.core.models.coach.CoachUsage

data class CoachMessage(val fromUser: Boolean, val text: String)

data class CoachTurnStats(
    val totalMillis: Long,
    val tools: List<String>,
    val usage: CoachUsage,
)

data class CoachDebugUiState(
    val messages: List<CoachMessage> = emptyList(),
    val input: String = "",
    val answering: Boolean = false,
    val runningTool: String? = null,
    val lastTurn: CoachTurnStats? = null,
    val error: CoachError? = null,
)

sealed interface CoachDebugEvent {
    data class InputChanged(val text: String) : CoachDebugEvent
    data object Send : CoachDebugEvent
    data object Stop : CoachDebugEvent
    data object NewConversation : CoachDebugEvent

    /** Sends [text] without typing it, for runs started from adb. */
    data class Ask(val text: String) : CoachDebugEvent
}
