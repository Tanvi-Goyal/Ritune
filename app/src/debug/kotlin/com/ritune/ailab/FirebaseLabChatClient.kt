package com.ritune.ailab

import com.google.firebase.Firebase
import com.google.firebase.ai.Chat
import com.google.firebase.ai.GenerativeModel
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.GenerateContentResponse
import com.google.firebase.ai.type.GenerativeBackend
import com.google.firebase.ai.type.RequestOptions
import com.google.firebase.ai.type.ThinkingLevel
import com.google.firebase.ai.type.generationConfig
import com.google.firebase.ai.type.thinkingConfig
import com.ritune.platform.android.di.Dispatcher
import com.ritune.platform.android.di.RituneDispatchers
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.emitAll
import javax.inject.Inject

/** Calls Gemini through Firebase AI Logic. The App Check token is attached by the SDK. */
class FirebaseLabChatClient @Inject constructor(
    @Dispatcher(RituneDispatchers.IO) private val io: CoroutineDispatcher,
) : LabChatClient {

    // A chat resends the earlier turns with every request; the model itself keeps nothing.
    private var chat: Chat? = null

    override fun stream(request: LabRequest): Flow<LabChunk> = flow {
        val responses = if (request.rememberChat) {
            val session = chat ?: model(request).startChat().also { chat = it }
            session.sendMessageStream(request.prompt)
        } else {
            model(request).generateContentStream(request.prompt)
        }
        emitAll(responses.map { it.toChunk() })
    }.flowOn(io)

    override fun clearHistory() {
        chat = null
    }

    private fun model(request: LabRequest): GenerativeModel =
        Firebase.ai(backend = GenerativeBackend.googleAI()).generativeModel(
            modelName = request.model.id,
            generationConfig = request.thinking.level()?.let { level ->
                generationConfig { thinkingConfig = thinkingConfig { thinkingLevel = level } }
            },
            requestOptions = RequestOptions(timeoutInMillis = TIMEOUT_MILLIS),
        )

    private companion object {
        const val TIMEOUT_MILLIS = 60_000L
    }
}

private fun LabThinking.level(): ThinkingLevel? = when (this) {
    LabThinking.DEFAULT -> null
    LabThinking.MINIMAL -> ThinkingLevel.MINIMAL
    LabThinking.LOW -> ThinkingLevel.LOW
    LabThinking.HIGH -> ThinkingLevel.HIGH
}

private fun GenerateContentResponse.toChunk() = LabChunk(
    text = text,
    usage = usageMetadata?.let {
        LabUsage(
            inputTokens = it.promptTokenCount,
            outputTokens = it.candidatesTokenCount,
            thinkingTokens = it.thoughtsTokenCount,
        )
    },
)
