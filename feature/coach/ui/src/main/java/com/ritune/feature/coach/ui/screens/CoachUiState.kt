package com.ritune.feature.coach.ui.screens

import com.ritune.core.models.coach.AppliedChange
import com.ritune.core.models.coach.CoachError
import com.ritune.core.models.coach.Proposal

sealed interface CoachUiState {
    data object Loading : CoachUiState

    data object Consent : CoachUiState

    data class Chat(
        val items: List<CoachItem> = emptyList(),
        val input: String = "",
        val answering: Boolean = false,
        val activeTool: String? = null,
        val error: CoachError? = null,
    ) : CoachUiState {
        val waitingForApproval: Boolean
            get() = items.any { it is ProposalCard && it.status == ProposalStatus.Pending }
    }
}

sealed interface CoachItem {
    val id: Long
}

data class CoachMessage(
    override val id: Long,
    val fromUser: Boolean,
    val text: String
) : CoachItem

data class ProposalCard(
    override val id: Long,
    val proposal: Proposal,
    val status: ProposalStatus,
) : CoachItem

sealed interface ProposalStatus {
    data object Pending : ProposalStatus
    data class Applied(val change: AppliedChange) : ProposalStatus
    data object Declined : ProposalStatus
    data object NotApplied : ProposalStatus
    data object Undone : ProposalStatus
}

sealed interface CoachUiEvent {
    data object TurnOn : CoachUiEvent
    data class InputChanged(val text: String) : CoachUiEvent
    data object Send : CoachUiEvent
    data class Ask(val question: String) : CoachUiEvent
    data object Stop : CoachUiEvent
    data object Retry : CoachUiEvent
    data object Apply : CoachUiEvent
    data object Decline : CoachUiEvent
    data class Undo(val cardId: Long) : CoachUiEvent
}
