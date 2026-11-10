package com.ritune.coach

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.ritune.core.designsystem.theme.RituneTheme
import com.ritune.core.models.coach.CoachError
import com.ritune.core.models.coach.CoachUsage

private val question = CoachMessage(fromUser = true, "How was my week?")

@Composable
private fun Preview(state: CoachDebugUiState) {
    RituneTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            CoachDebugContent(state = state, onEvent = {})
        }
    }
}

@PreviewLightDark
@Composable
private fun EmptyPreview() = Preview(CoachDebugUiState())

@PreviewLightDark
@Composable
private fun ToolRunningPreview() = Preview(
    CoachDebugUiState(
        messages = listOf(question, CoachMessage(fromUser = false, "")),
        answering = true,
        runningTool = "get_week_rhythm",
    ),
)

@PreviewLightDark
@Composable
private fun AnsweredPreview() = Preview(
    CoachDebugUiState(
        messages = listOf(
            question,
            CoachMessage(
                fromUser = false,
                "You were steady on 80% of days this week. Wake-up was your steadiest anchor.",
            ),
        ),
        lastTurn = CoachTurnStats(
            totalMillis = 2140,
            tools = listOf("get_week_rhythm"),
            usage = CoachUsage(inputTokens = 1210, outputTokens = 38, thinkingTokens = 0),
        ),
    ),
)

@PreviewLightDark
@Composable
private fun ErrorPreview() = Preview(
    CoachDebugUiState(messages = listOf(question), error = CoachError.QUOTA_EXCEEDED),
)
