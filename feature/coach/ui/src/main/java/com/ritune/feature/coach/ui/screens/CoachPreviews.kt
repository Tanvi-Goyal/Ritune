package com.ritune.feature.coach.ui.screens

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.ritune.core.designsystem.theme.RituneTheme
import com.ritune.core.domain.usecases.coach.tools.WeekRhythmTool
import com.ritune.core.models.coach.CoachError

internal val previewQuestion = CoachMessage(
    id = 0,
    fromUser = true,
    text = "How was my week?"
)
internal val previewAnswer = CoachMessage(
    id = 1,
    fromUser = false,
    text = "You were steady on 82% of your logs this week, and Lights out was your steadiest " +
            "anchor. Your wake time drifted by 18 minutes, so a fixed alarm could help.",
)

@Composable
private fun Preview(state: CoachUiState) {
    RituneTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            CoachContent(state = state, onEvent = {}, onBack = {})
        }
    }
}

@PreviewLightDark
@Composable
private fun ConsentPreview() = Preview(CoachUiState.Consent)

@PreviewLightDark
@Composable
private fun EmptyPreview() = Preview(CoachUiState.Chat())

@PreviewLightDark
@Composable
private fun ToolRunningPreview() = Preview(
    CoachUiState.Chat(
        messages = listOf(previewQuestion),
        answering = true,
        activeTool = WeekRhythmTool.DEFINITION.name,
    ),
)

@PreviewLightDark
@Composable
private fun AnsweredPreview() =
    Preview(CoachUiState.Chat(messages = listOf(previewQuestion, previewAnswer)))

@PreviewLightDark
@Composable
private fun ErrorPreview() = Preview(
    CoachUiState.Chat(messages = listOf(previewQuestion), error = CoachError.NETWORK),
)
