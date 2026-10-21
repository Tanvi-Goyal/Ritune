package com.ritune.feature.player.ui.screens

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.ritune.core.designsystem.theme.RituneTheme
import com.ritune.feature.player.ui.screens.player.PlayerContent
import com.ritune.feature.player.ui.screens.player.PlayerUiState
import com.ritune.feature.player.ui.screens.summary.SummaryContent
import com.ritune.feature.player.ui.screens.summary.SummaryUiState
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

private fun active(
    stepIndex: Int = 1,
    title: String = "Write tomorrow's top three",
    next: String? = "Light stretch",
    remaining: Duration = 134.seconds,
    paused: Boolean = false,
    instruction: String? = null,
    confirming: Boolean = false,
) = PlayerUiState.Active(
    routineName = "Evening reset",
    stepIndex = stepIndex,
    stepCount = 4,
    stepTitle = title,
    instruction = instruction,
    nextStepTitle = next,
    remaining = remaining,
    isPaused = paused,
    confirmingStop = confirming,
)

private val started = Instant.parse("2026-10-09T16:16:00Z")

private fun summary(
    completed: Boolean = true,
    completedSteps: Int = 4,
    skipped: Int = 0,
    total: Int = 4
) = SummaryUiState.Content(
    "Evening reset",
    completed,
    completedSteps,
    skipped,
    total,
    8.minutes + 2.seconds,
    started
)

@Composable
private fun Themed(content: @Composable () -> Unit) {
    RituneTheme {
        Surface(
            color = MaterialTheme.colorScheme.background,
            content = content
        )
    }
}

@PreviewLightDark
@Composable
private fun PlayerRunningPreview() = Themed {
    PlayerContent(
        active(),
        {})
}

@PreviewLightDark
@Composable
private fun PlayerPausedPreview() =
    Themed {
        PlayerContent(
            active(
                paused = true,
                remaining = 65.seconds
            ), {})
    }

@PreviewLightDark
@Composable
private fun PlayerWithInstructionPreview() = Themed {
    PlayerContent(
        active(
            stepIndex = 0,
            title = "Put screens away",
            instruction = "Close laptops and tablets.",
            next = "Write tomorrow's top three"
        ), {})
}

@PreviewLightDark
@Composable
private fun PlayerLastStepPreview() = Themed {
    PlayerContent(
        active(
            stepIndex = 3,
            title = "Slow breathing",
            next = null,
            remaining = 40.seconds
        ), {})
}

@PreviewLightDark
@Composable
private fun PlayerStopDialogPreview() = Themed {
    PlayerContent(
        active(confirming = true),
        {})
}

@PreviewLightDark
@Composable
private fun SummaryCompletedPreview() = Themed {
    SummaryContent(
        summary(),
        {})
}

@PreviewLightDark
@Composable
private fun SummaryWithSkippedPreview() =
    Themed {
        SummaryContent(
            summary(
                completedSteps = 3,
                skipped = 1
            ),
            {}
        )
    }

@PreviewLightDark
@Composable
private fun SummaryStoppedPreview() =
    Themed {
        SummaryContent(
            summary(
                completed = false,
                completedSteps = 1
            ),
            {})
    }

@PreviewLightDark
@Composable
private fun SummaryNotFoundPreview() = Themed {
    SummaryContent(
        SummaryUiState.NotFound,
        {}
    )
}