package com.ritune.feature.coach.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ritune.core.domain.usecases.coach.EndCoachConversation
import com.ritune.core.domain.usecases.coach.RunCoachTurn
import com.ritune.core.domain.usecases.coach.UndoCoachChange
import com.ritune.core.domain.usecases.settings.ObserveSettings
import com.ritune.core.domain.usecases.settings.UpdateSettings
import com.ritune.core.models.IdGenerator
import com.ritune.core.models.coach.CoachEvent
import com.ritune.core.models.coach.ConversationId
import com.ritune.core.models.coach.Decision
import com.ritune.core.models.coach.Proposal
import com.ritune.core.models.coach.ProposalOutcome
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CoachViewModel @Inject constructor(
    observeSettings: ObserveSettings,
    private val updateSettings: UpdateSettings,
    private val runCoachTurn: RunCoachTurn,
    private val endConversation: EndCoachConversation,
    private val undoChange: UndoCoachChange,
    private val ids: IdGenerator,
) : ViewModel() {

    private val chat = MutableStateFlow(CoachUiState.Chat())

    val state: StateFlow<CoachUiState> =
        combine(observeSettings(), chat) { settings, chat ->
            if (settings.coachEnabled) chat else CoachUiState.Consent
        }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            CoachUiState.Loading,
        )

    private var conversation = ConversationId(ids.newId())
    private var nextItemId = 0L
    private var lastQuestion: String? = null
    private var turn: Job? = null
    private var decision: CompletableDeferred<Decision>? = null
    private val undone = mutableListOf<Proposal>()

    fun onEvent(event: CoachUiEvent) {
        when (event) {
            CoachUiEvent.TurnOn -> viewModelScope.launch {
                updateSettings { it.copy(coachEnabled = true) }
            }

            is CoachUiEvent.InputChanged -> chat.update { it.copy(input = event.text) }
            CoachUiEvent.Send -> ask(chat.value.input)
            is CoachUiEvent.Ask -> ask(event.question)
            CoachUiEvent.Stop -> stop()
            CoachUiEvent.Retry -> retry()
            CoachUiEvent.Apply -> decide(Decision.APPLY)
            CoachUiEvent.Decline -> decide(Decision.DECLINE)
            is CoachUiEvent.Undo -> undo(event.cardId)
        }
    }

    private fun ask(text: String) {
        val question = text.trim()
        if (question.isEmpty() || chat.value.answering) return
        lastQuestion = question
        chat.update {
            it.copy(
                items = it.items + message(fromUser = true, question),
                input = ""
            )
        }
        answer(question)
    }

    private fun retry() {
        val question = lastQuestion ?: return
        if (chat.value.answering) return
        answer(question)
    }

    private fun answer(question: String) {
        chat.update { it.copy(answering = true, error = null) }
        val notes = undone.toList()
        undone.clear()
        turn = viewModelScope.launch {
            try {
                runCoachTurn(
                    conversation,
                    question,
                    ::awaitDecision,
                    notes
                )
                    .collect(::onTurnEvent)
            } finally {
                decision = null
                chat.update {
                    it.copy(
                        answering = false,
                        activeTool = null,
                        items = it.items.resolvePending(ProposalStatus.Declined),
                    )
                }
            }
        }
    }

    private suspend fun awaitDecision(proposal: Proposal): Decision {
        val pending = CompletableDeferred<Decision>()
        decision = pending
        chat.update {
            it.copy(
                activeTool = null,
                items = it.items + ProposalCard(
                    nextItemId++,
                    proposal,
                    ProposalStatus.Pending
                ),
            )
        }
        return pending.await()
    }

    private fun decide(choice: Decision) {
        decision?.complete(choice)
        decision = null
    }

    private fun undo(cardId: Long) {
        val card = chat.value.items.firstOrNull { it.id == cardId } as? ProposalCard ?: return
        val applied = card.status as? ProposalStatus.Applied ?: return
        chat.update { state ->
            state.copy(items = state.items.map {
                if (it.id == cardId)
                    card.copy(status = ProposalStatus.Undone) else it
            })
        }
        undone += card.proposal
        viewModelScope.launch { undoChange(applied.change) }
    }

    private fun onTurnEvent(event: CoachEvent) {
        when (event) {
            is CoachEvent.TextDelta -> chat.update {
                it.copy(activeTool = null, items = it.items.appendToAnswer(event.text))
            }

            is CoachEvent.ProposalResolved -> chat.update {
                it.copy(items = it.items.resolvePending(event.outcome.toStatus()))
            }

            is CoachEvent.ToolRunning -> chat.update { it.copy(activeTool = event.tool) }
            is CoachEvent.Done -> Unit
            is CoachEvent.Failed -> {
                restartConversation()
                chat.update { it.copy(error = event.error) }
            }
        }
    }

    private fun stop() {
        turn?.cancel()
        restartConversation()
    }

    private fun restartConversation() {
        endConversation(conversation)
        conversation = ConversationId(ids.newId())
        undone.clear()
    }

    private fun message(fromUser: Boolean, text: String) =
        CoachMessage(id = nextItemId++, fromUser = fromUser, text = text)

    override fun onCleared() {
        endConversation(conversation)
    }

    private fun List<CoachItem>.appendToAnswer(text: String): List<CoachItem> {
        val answer = lastOrNull() as? CoachMessage
        return if (answer != null && !answer.fromUser) {
            dropLast(1) + answer.copy(text = answer.text + text)
        } else {
            val trimmed = text.trimStart()
            if (trimmed.isEmpty()) this else this + message(fromUser = false, trimmed)
        }
    }
}

private fun List<CoachItem>.resolvePending(status: ProposalStatus) = map {
    if (it is ProposalCard && it.status == ProposalStatus.Pending)
        it.copy(status = status) else it
}

private fun ProposalOutcome.toStatus() = when (this) {
    is ProposalOutcome.Applied -> ProposalStatus.Applied(change)
    ProposalOutcome.Declined -> ProposalStatus.Declined
    ProposalOutcome.NotApplied -> ProposalStatus.NotApplied
}
