package com.ritune.ailab

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.ritune.core.designsystem.theme.RituneTheme

@Composable
private fun Preview(state: AiLabUiState) {
    RituneTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            AiLabContent(state = state, onEvent = {})
        }
    }
}

@PreviewLightDark
@Composable
private fun EmptyPreview() = Preview(AiLabUiState())

@PreviewLightDark
@Composable
private fun StreamingPreview() = Preview(
    AiLabUiState(input = "How was my week?", reply = "Your bedtime moved", streaming = true),
)

@PreviewLightDark
@Composable
private fun AnsweredPreview() = Preview(
    AiLabUiState(
        input = "hi",
        reply = "Hello! How can I help you today?",
        stats = LabStats(
            firstChunkMillis = 892,
            totalMillis = 915,
            inputTokens = 2,
            outputTokens = 9,
            thinkingTokens = 0,
        ),
    ),
)

@PreviewLightDark
@Composable
private fun ErrorPreview() = Preview(
    AiLabUiState(
        input = "hi",
        error = "QuotaExceededException: You exceeded your current quota",
    ),
)
