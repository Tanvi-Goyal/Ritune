package com.ritune.feature.coach.ui.screens

import com.ritune.core.models.coach.CoachError

sealed interface CoachUiState {
    data object Loading : CoachUiState

    data object Consent : CoachUiState

    data class Chat(
        val messages: List<CoachMessage> = emptyList(),
        val input: String = "",
        val answering: Boolean = false,
        val activeTool: String? = null,
        val error: CoachError? = null,
    ) : CoachUiState
}

data class CoachMessage(
    val id: Long,
    val fromUser: Boolean,
    val text: String
)

sealed interface CoachUiEvent {
    data object TurnOn : CoachUiEvent
    data class InputChanged(val text: String) : CoachUiEvent
    data object Send : CoachUiEvent
    data class Ask(val question: String) : CoachUiEvent
    data object Stop : CoachUiEvent
    data object Retry : CoachUiEvent
}
