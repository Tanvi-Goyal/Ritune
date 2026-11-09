package com.ritune.ailab

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ritune.core.models.Timekeeper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException

@HiltViewModel
class AiLabViewModel @Inject constructor(
    private val client: LabChatClient,
    private val timekeeper: Timekeeper,
) : ViewModel() {

    private val _state = MutableStateFlow(AiLabUiState())
    val state: StateFlow<AiLabUiState> = _state.asStateFlow()

    private var job: Job? = null

    fun onEvent(event: AiLabEvent) {
        when (event) {
            is AiLabEvent.InputChanged -> _state.update { it.copy(input = event.text) }
            is AiLabEvent.ModelSelected -> changeSettings { it.copy(model = event.model) }
            is AiLabEvent.ThinkingSelected -> changeSettings { it.copy(thinking = event.thinking) }
            is AiLabEvent.RememberChatChanged -> changeSettings {
                it.copy(rememberChat = event.remember)
            }

            AiLabEvent.Send -> send()
            AiLabEvent.Stop -> job?.cancel()
            is AiLabEvent.RunBatch -> runBatch(event)
        }
    }

    // A chat keeps the model it started with, so any settings change starts a new chat.
    private fun changeSettings(transform: (AiLabUiState) -> AiLabUiState) {
        client.clearHistory()
        _state.update(transform)
    }

    private fun send() {
        val prompt = _state.value.input.trim()
        if (prompt.isEmpty() || _state.value.streaming) return
        job = viewModelScope.launch { sendOnce(prompt) }
    }

    private fun runBatch(event: AiLabEvent.RunBatch) {
        job?.cancel()
        changeSettings {
            it.copy(
                input = event.prompt,
                model = event.model,
                thinking = event.thinking,
                rememberChat = false,
            )
        }
        job = viewModelScope.launch {
            repeat(event.runs) { sendOnce(event.prompt) }
            Log.i(TAG, "batch done: ${event.runs} runs")
        }
    }

    private suspend fun sendOnce(prompt: String) {
        val settings = _state.value
        _state.update { it.copy(reply = "", streaming = true, stats = null, error = null) }
        val start = timekeeper.now().monoMillis
        var firstChunkAt: Long? = null
        var usage: LabUsage? = null
        try {
            val request = LabRequest(prompt, settings.model, settings.thinking, settings.rememberChat)
            client.stream(request).collect { chunk ->
                if (firstChunkAt == null) firstChunkAt = timekeeper.now().monoMillis
                chunk.text?.let { text -> _state.update { it.copy(reply = it.reply + text) } }
                chunk.usage?.let { usage = it }
            }
            val stats = LabStats(
                firstChunkMillis = firstChunkAt?.minus(start),
                totalMillis = timekeeper.now().monoMillis - start,
                inputTokens = usage?.inputTokens,
                outputTokens = usage?.outputTokens,
                thinkingTokens = usage?.thinkingTokens,
            )
            // Timing runs from adb read these lines from logcat.
            Log.i(TAG, stats.logLine(settings))
            _state.update { it.copy(stats = stats) }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "Gemini call failed", e)
            _state.update { it.copy(error = "${e::class.simpleName}: ${e.message}") }
        } finally {
            _state.update { it.copy(streaming = false) }
        }
    }

    private companion object {
        const val TAG = "AiLab"
    }
}

private fun LabStats.logLine(settings: AiLabUiState) =
    "${settings.model.id} · thinking ${settings.thinking.name.lowercase()} · " +
        "first chunk ${firstChunkMillis ?: "-"} ms · total $totalMillis ms · " +
        "tokens in ${inputTokens ?: "-"} / out ${outputTokens ?: "-"} / " +
        "thinking ${thinkingTokens ?: "-"}"
