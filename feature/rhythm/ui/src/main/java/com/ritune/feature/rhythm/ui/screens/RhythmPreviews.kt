package com.ritune.feature.rhythm.ui.screens

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.ritune.core.designsystem.theme.RituneTheme
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus

private val weekEnd = LocalDate(2026, 10, 9)

private fun bars(vararg fractions: Float?) = fractions.mapIndexed { i, f ->
    DayBar(
        weekEnd.minus(
            fractions.size - 1 - i,
            DateTimeUnit.DAY
        ), f
    )
}

private fun content(
    summary: RhythmSummary,
    days: List<DayBar> = bars(null, null, null, null, null, null, null)
) =
    RhythmUiState.Content(weekEnd, days, summary)

@Composable
private fun Preview(state: RhythmUiState) {
    RituneTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            RhythmContent(
                state,
                onOpenSettings = {})
        }
    }
}

@PreviewLightDark
@Composable
private fun RhythmScoredPreview() = Preview(
    content(
        RhythmSummary.Scored(
            consistencyPercent = 82,
            wakeDriftMinutes = 18,
            steadiestName = "Workout"
        ),
        bars(0.7f, 0.85f, 0.6f, 0.9f, 0.75f, 0.4f, 0.6f),
    ),
)

@PreviewLightDark
@Composable
private fun RhythmScoredWithGapsPreview() = Preview(
    content(
        RhythmSummary.Scored(
            consistencyPercent = 100,
            wakeDriftMinutes = 1,
            steadiestName = "Wake"
        ),
        bars(null, null, 1f, 1f, null, 1f, 1f),
    ),
)

@PreviewLightDark
@Composable
private fun RhythmNoDriftYetPreview() = Preview(
    content(
        RhythmSummary.Scored(
            consistencyPercent = 75,
            wakeDriftMinutes = null,
            steadiestName = null
        ),
        bars(null, null, null, 0.5f, 1f, 1f, 0.75f),
    ),
)

@PreviewLightDark
@Composable
private fun RhythmEarlyPreview() =
    Preview(content(RhythmSummary.Early(loggedDays = 1, requiredDays = 3)))

@PreviewLightDark
@Composable
private fun RhythmBuildingPreview() = Preview(content(RhythmSummary.Building))

@PreviewLightDark
@Composable
private fun RhythmLoadingPreview() = Preview(RhythmUiState.Loading)