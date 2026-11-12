package com.ritune.ailab

enum class LabModel(val id: String) {
    FLASH_3_8("gemini-3.8-flash"),
    FLASH_3_5("gemini-3.5-flash"),
    FLASH_LITE_3_5("gemini-3.5-flash-lite"),
}

enum class LabThinking { DEFAULT, MINIMAL, LOW, HIGH }

data class LabStats(
    val firstChunkMillis: Long?,
    val totalMillis: Long,
    val inputTokens: Int?,
    val outputTokens: Int?,
    val thinkingTokens: Int?,
)

data class AiLabUiState(
    val model: LabModel = LabModel.FLASH_LITE_3_5,
    val thinking: LabThinking = LabThinking.DEFAULT,
    val rememberChat: Boolean = false,
    val input: String = "",
    val reply: String = "",
    val streaming: Boolean = false,
    val stats: LabStats? = null,
    val error: String? = null,
)

sealed interface AiLabEvent {
    data class InputChanged(val text: String) : AiLabEvent
    data class ModelSelected(val model: LabModel) : AiLabEvent
    data class ThinkingSelected(val thinking: LabThinking) : AiLabEvent
    data class RememberChatChanged(val remember: Boolean) : AiLabEvent
    data object Send : AiLabEvent
    data object Stop : AiLabEvent

    data class RunBatch(
        val prompt: String,
        val model: LabModel,
        val thinking: LabThinking,
        val runs: Int,
    ) : AiLabEvent
}
