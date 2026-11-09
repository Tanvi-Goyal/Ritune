package com.ritune.ailab

import android.os.SystemClock
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.Firebase
import com.google.firebase.ai.Chat
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.GenerateContentResponse
import com.google.firebase.ai.type.GenerativeBackend
import com.google.firebase.ai.type.RequestOptions
import com.google.firebase.ai.type.ThinkingLevel
import com.google.firebase.ai.type.generationConfig
import com.google.firebase.ai.type.thinkingConfig
import com.google.firebase.ai.type.UsageMetadata
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class LabModel(val id: String) {
    FLASH_3_8("gemini-3.8-flash"),
    FLASH_3_5("gemini-3.5-flash"),
    FLASH_LITE_3_5("gemini-3.5-flash-lite"),
}

/** [level] null leaves thinking at the model's default. */
enum class LabThinking(val level: ThinkingLevel?) {
    DEFAULT(null),
    MINIMAL(ThinkingLevel.MINIMAL),
    LOW(ThinkingLevel.LOW),
    HIGH(ThinkingLevel.HIGH),
}

data class AiLabUiState(
    val model: LabModel = LabModel.FLASH_LITE_3_5,
    val thinking: LabThinking = LabThinking.DEFAULT,
    val input: String = "",
    val reply: String = "",
    val streaming: Boolean = false,
    val rememberChat: Boolean = false,
    val stats: String? = null,
    val error: String? = null,
)

class AiLabViewModel : ViewModel() {

    private fun buildModel(model: LabModel, thinking: LabThinking) =
        Firebase.ai(backend = GenerativeBackend.googleAI()).generativeModel(
            modelName = model.id,
            generationConfig = thinking.level?.let { level ->
                generationConfig { thinkingConfig = thinkingConfig { thinkingLevel = level } }
            },
            requestOptions = RequestOptions(timeoutInMillis = 60_000),
        )

    // A chat session resends the earlier messages with every request; the model itself keeps nothing.
    private var chat: Chat? = null
    private var job: Job? = null

    private val _state = MutableStateFlow(AiLabUiState())
    val state: StateFlow<AiLabUiState> = _state.asStateFlow()

    fun onInputChange(text: String) = _state.update { it.copy(input = text) }

    fun onModelChange(model: LabModel) {
        chat = null
        _state.update { it.copy(model = model) }
    }

    fun onThinkingChange(thinking: LabThinking) {
        chat = null
        _state.update { it.copy(thinking = thinking) }
    }

    fun onRememberChatChange(remember: Boolean) {
        chat = null
        _state.update { it.copy(rememberChat = remember) }
    }

    fun stop() {
        job?.cancel()
    }

    fun send() {
        val prompt = _state.value.input.trim()
        if (prompt.isEmpty() || _state.value.streaming) return
        job = viewModelScope.launch { sendOnce(prompt) }
    }

    /** Sends [prompt] [runs] times in a row, for timing runs started from adb. */
    fun runBatch(prompt: String, model: LabModel, thinking: LabThinking, runs: Int) {
        chat = null
        _state.update { it.copy(input = prompt, model = model, thinking = thinking, rememberChat = false) }
        job = viewModelScope.launch {
            repeat(runs) { sendOnce(prompt) }
            Log.i(TAG, "batch done: $runs runs")
        }
    }

    private suspend fun sendOnce(prompt: String) {
        _state.update { it.copy(reply = "", streaming = true, stats = null, error = null) }
        val start = SystemClock.elapsedRealtime()
        var firstChunkMs: Long? = null
        var usage: UsageMetadata? = null
        try {
            responses(prompt).collect { chunk ->
                if (firstChunkMs == null) firstChunkMs = SystemClock.elapsedRealtime() - start
                chunk.text?.let { text -> _state.update { it.copy(reply = it.reply + text) } }
                chunk.usageMetadata?.let { usage = it }
            }
            val stats = "${_state.value.model.id} · thinking ${_state.value.thinking.name.lowercase()} · " +
                "first chunk ${firstChunkMs ?: "-"} ms · total " +
                "${SystemClock.elapsedRealtime() - start} ms · tokens in " +
                "${usage?.promptTokenCount ?: "-"} / out ${usage?.candidatesTokenCount ?: "-"} / " +
                "thinking ${usage?.thoughtsTokenCount ?: "-"}"
            Log.i(TAG, stats)
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

    // Firebase reads its config from disk on first use, so build the model off the main thread.
    // The chat keeps its model, so a picker change starts a new chat (see onModelChange).
    private fun responses(prompt: String): Flow<GenerateContentResponse> = flow {
        val current = _state.value
        val session = if (current.rememberChat) {
            chat ?: buildModel(current.model, current.thinking).startChat().also { chat = it }
        } else {
            null
        }
        emitAll(
            session?.sendMessageStream(prompt)
                ?: buildModel(current.model, current.thinking).generateContentStream(prompt),
        )
    }.flowOn(Dispatchers.IO)

    private companion object {
        const val TAG = "AiLab"
    }
}
