package com.ritune.feature.coach.ui.screens

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.ritune.core.designsystem.theme.RituneTheme
import com.ritune.core.domain.usecases.coach.tools.WeekRhythmTool
import com.ritune.core.models.AnchorEvent
import com.ritune.core.models.DefaultCatalog
import com.ritune.core.models.EventId
import com.ritune.core.models.EventSource
import com.ritune.core.models.coach.AppliedChange
import com.ritune.core.models.coach.CoachError
import com.ritune.core.models.coach.Proposal
import com.ritune.core.models.coach.ProposedStep
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

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

internal val previewTargetProposal = Proposal.AnchorTarget(
    anchorId = DefaultCatalog.shutdown.id,
    anchorName = "Screens off",
    from = LocalTime(21, 30),
    to = LocalTime(22, 0),
)
internal val previewRoutineProposal = Proposal.NewRoutine(
    name = "Wind down",
    anchorId = DefaultCatalog.shutdown.id,
    anchorName = "Screens off",
    steps = listOf(
        ProposedStep("Dim the lights", 1.minutes),
        ProposedStep("Read a book", 10.minutes),
        ProposedStep("Breathe slowly", 2.minutes + 30.seconds),
    ),
)
internal val previewLogProposal = Proposal.LogCorrection(
    original = AnchorEvent(
        id = EventId("log-1"),
        anchorId = DefaultCatalog.wake.id,
        occurredAt = Instant.parse("2026-11-10T07:40:00Z"),
        utcOffsetSeconds = 0,
        source = EventSource.MANUAL,
    ),
    anchorName = "Wake",
    date = LocalDate(2026, 11, 10),
    from = LocalTime(7, 40),
    to = LocalTime(7, 10),
)
internal val previewMoveQuestion = CoachMessage(id = 0, fromUser = true, text = "Move screens off to 22:00")
internal val previewRoutineQuestion =
    CoachMessage(id = 0, fromUser = true, text = "Build me a short wind-down routine for screens off")
internal val previewLogQuestion =
    CoachMessage(id = 0, fromUser = true, text = "I woke up at 7:10 yesterday, please fix that log")

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
        items = listOf(previewQuestion),
        answering = true,
        activeTool = WeekRhythmTool.DEFINITION.name,
    ),
)

@PreviewLightDark
@Composable
private fun AnsweredPreview() =
    Preview(CoachUiState.Chat(items = listOf(previewQuestion, previewAnswer)))

@PreviewLightDark
@Composable
private fun ErrorPreview() = Preview(
    CoachUiState.Chat(items = listOf(previewQuestion), error = CoachError.NETWORK),
)

@PreviewLightDark
@Composable
private fun TargetProposalPreview() = Preview(
    CoachUiState.Chat(
        items = listOf(previewMoveQuestion, ProposalCard(1, previewTargetProposal, ProposalStatus.Pending)),
        answering = true,
    ),
)

@PreviewLightDark
@Composable
private fun RoutineProposalPreview() = Preview(
    CoachUiState.Chat(
        items = listOf(previewRoutineQuestion, ProposalCard(1, previewRoutineProposal, ProposalStatus.Pending)),
        answering = true,
    ),
)

@PreviewLightDark
@Composable
private fun AppliedLogProposalPreview() = Preview(
    CoachUiState.Chat(
        items = listOf(
            previewLogQuestion,
            ProposalCard(1, previewLogProposal, ProposalStatus.Applied(AppliedChange.LogCorrected(previewLogProposal))),
            CoachMessage(id = 2, fromUser = false, text = "Done, your wake log on Monday now says 07:10."),
        ),
    ),
)
