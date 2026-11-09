package com.ritune.ailab

import kotlinx.coroutines.flow.Flow

interface LabChatClient {

    fun stream(request: LabRequest): Flow<LabChunk>

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
