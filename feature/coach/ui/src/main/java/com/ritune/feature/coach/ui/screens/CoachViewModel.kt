package com.ritune.feature.coach.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ritune.core.domain.usecases.coach.EndCoachConversation
import com.ritune.core.domain.usecases.coach.RunCoachTurn
import com.ritune.core.domain.usecases.settings.ObserveSettings
import com.ritune.core.domain.usecases.settings.UpdateSettings
import com.ritune.core.models.IdGenerator
import com.ritune.core.models.coach.CoachEvent
import com.ritune.core.models.coach.ConversationId
import dagger.hilt.android.lifecycle.HiltViewModel
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

    // Each screen visit is one conversation, which keeps the history resent with every request short.
    private var conversation = ConversationId(ids.newId())
    private var nextMessageId = 0L
    private var lastQuestion: String? = null
    private var turn: Job? = null

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
        }
    }

    private fun ask(text: String) {
        val question = text.trim()
        if (question.isEmpty() || chat.value.answering) return
        lastQuestion = question
        chat.update {
            it.copy(messages = it.messages + message(fromUser = true, question), input = "")
        }
        answer(question)
    }

    private fun retry() {
        val question = lastQuestion ?: return
        if (chat.value.answering) return
        answer(question)
    }

    private fun answer(question: String) {
        chat.update {
            it.copy(
                messages = it.messages + message(fromUser = false, ""),
                answering = true,
                error = null,
            )
        }
        turn = viewModelScope.launch {
            try {
                runCoachTurn(conversation, question)
                    .collect(::onTurnEvent)
            } finally {
                chat.update {
                    it.copy(
                        answering = false,
                        activeTool = null,
                        messages = it.messages.dropEmptyAnswer()
                    )
                }
            }
        }
    }

    private fun onTurnEvent(event: CoachEvent) {
        when (event) {
            is CoachEvent.TextDelta -> chat.update {
                it.copy(activeTool = null, messages = it.messages.appendToAnswer(event.text))
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
    }

    private fun message(fromUser: Boolean, text: String) =
        CoachMessage(id = nextMessageId++, fromUser = fromUser, text = text)

    override fun onCleared() {
        endConversation(conversation)
    }
}

private fun List<CoachMessage>.appendToAnswer(text: String): List<CoachMessage> {
    val answer = last()
    val joined = if (answer.text.isEmpty()) text.trimStart() else answer.text + text
    return dropLast(1) + answer.copy(text = joined)
}

private fun List<CoachMessage>.dropEmptyAnswer() =
    if (lastOrNull()?.let { !it.fromUser && it.text.isBlank() } == true) dropLast(1) else this
