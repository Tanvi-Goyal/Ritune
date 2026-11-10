package com.ritune.coach

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ritune.core.domain.usecases.coach.EndCoachConversation
import com.ritune.core.domain.usecases.coach.RunCoachTurn
import com.ritune.core.models.IdGenerator
import com.ritune.core.models.Timekeeper
import com.ritune.core.models.coach.CoachEvent
import com.ritune.core.models.coach.ConversationId
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CoachDebugViewModel @Inject constructor(
    private val runCoachTurn: RunCoachTurn,
    private val endConversation: EndCoachConversation,
    private val ids: IdGenerator,
    private val timekeeper: Timekeeper,
) : ViewModel() {

    private val _state = MutableStateFlow(CoachDebugUiState())
    val state: StateFlow<CoachDebugUiState> = _state.asStateFlow()

    private var conversation = ConversationId(ids.newId())
    private var job: Job? = null

    fun onEvent(event: CoachDebugEvent) {
        when (event) {
            is CoachDebugEvent.InputChanged -> _state.update { it.copy(input = event.text) }
            CoachDebugEvent.Send -> send(_state.value.input)
            is CoachDebugEvent.Ask -> send(event.text)
            // A turn stopped mid-way can leave a tool call with no answer in the history.
            CoachDebugEvent.Stop -> {
                job?.cancel()
                restartConversation()
            }

            CoachDebugEvent.NewConversation -> {
                job?.cancel()
                restartConversation()
                _state.value = CoachDebugUiState()
            }
        }
    }

    private fun send(text: String) {
        val message = text.trim()
        if (message.isEmpty() || _state.value.answering) return
        _state.update {
            it.copy(
                messages = it.messages + CoachMessage(fromUser = true, message) +
                        CoachMessage(fromUser = false, ""),
                input = "",
                answering = true,
                lastTurn = null,
                error = null,
            )
        }
        job = viewModelScope.launch {
            val start = timekeeper.now().monoMillis
            val tools = mutableListOf<String>()
            try {
                runCoachTurn(conversation, message).collect { event ->
                    when (event) {
                        is CoachEvent.TextDelta -> _state.update {
                            it.copy(
                                runningTool = null,
                                messages = it.messages.appendToLast(event.text)
                            )
                        }

                        is CoachEvent.ToolRunning -> {
                            tools += event.tool
                            _state.update { it.copy(runningTool = event.tool) }
                        }

                        is CoachEvent.Done -> {
                            val stats = CoachTurnStats(
                                timekeeper.now().monoMillis - start,
                                tools,
                                event.usage
                            )
                            // adb validation runs read the turn from logcat.
                            Log.i(TAG, "${stats.logLine()}\n${_state.value.messages.last().text}")
                            _state.update { it.copy(lastTurn = stats) }
                        }

                        is CoachEvent.Failed -> {
                            Log.w(TAG, "turn failed: ${event.error}")
                            restartConversation()
                            _state.update { it.copy(error = event.error) }
                        }
                    }
                }
            } finally {
                _state.update { it.copy(answering = false, runningTool = null) }
            }
        }
    }

    private fun restartConversation() {
        endConversation(conversation)
        conversation = ConversationId(ids.newId())
    }

    override fun onCleared() {
        endConversation(conversation)
    }

    private companion object {
        const val TAG = "Coach"
    }
}

private fun List<CoachMessage>.appendToLast(text: String) =
    dropLast(1) + last().let { it.copy(text = it.text + text) }

private fun CoachTurnStats.logLine() =
    "turn · tools $tools · total $totalMillis ms · " +
            "tokens in ${usage.inputTokens} / out ${usage.outputTokens} " +
            "/ thinking ${usage.thinkingTokens}"
