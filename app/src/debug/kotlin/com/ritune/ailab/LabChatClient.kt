package com.ritune.ailab

import kotlinx.coroutines.flow.Flow

/** The lab's only view of the model provider, so the ViewModel is tested without Firebase. */
interface LabChatClient {

    /** Streams the reply. With [LabRequest.rememberChat], the earlier turns are sent too. */
    fun stream(request: LabRequest): Flow<LabChunk>

    /** Forgets the earlier turns, so the next remembered request starts a new chat. */
    fun clearHistory()
}

data class LabRequest(
    val prompt: String,
    val model: LabModel,
    val thinking: LabThinking,
    val rememberChat: Boolean,
)

data class LabChunk(
    val text: String?,
    val usage: LabUsage? = null,
)

data class LabUsage(
    val inputTokens: Int,
    val outputTokens: Int?,
    val thinkingTokens: Int?,
)
