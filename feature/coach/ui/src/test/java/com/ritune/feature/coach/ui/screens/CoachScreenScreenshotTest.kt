package com.ritune.feature.coach.ui.screens

import androidx.activity.ComponentActivity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ritune.core.designsystem.theme.RituneTheme
import com.ritune.core.domain.usecases.coach.tools.WeekRhythmTool
import com.ritune.core.models.coach.AppliedChange
import com.ritune.core.models.coach.CoachError
import com.ritune.core.screenshottesting.captureScreenSizes
import com.ritune.core.screenshottesting.captureScreenVariants
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class CoachScreenScreenshotTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private val answered = CoachUiState.Chat(items = listOf(previewQuestion, previewAnswer))

    @Composable
    private fun Coach(state: CoachUiState) {
        RituneTheme {
            Surface(color = MaterialTheme.colorScheme.background) {
                CoachContent(state, onEvent = {}, onBack = {})
            }
        }
    }

    @Test
    fun consent() {
        composeTestRule.captureScreenSizes("coach_consent") { Coach(CoachUiState.Consent) }
    }

    @Test
    fun consent_darkAndLargeFont() {
        composeTestRule.captureScreenVariants("coach_consent") { Coach(CoachUiState.Consent) }
    }

    @Test
    fun empty() {
        composeTestRule.captureScreenVariants("coach_empty") { Coach(CoachUiState.Chat()) }
    }

    @Test
    fun streaming() {
        composeTestRule.captureScreenVariants("coach_streaming") {
            val partial = previewAnswer.copy(text = "You were steady on 82% of")
            Coach(CoachUiState.Chat(items = listOf(previewQuestion, partial), answering = true))
        }
    }

    @Test
    fun toolRunning() {
        composeTestRule.captureScreenVariants("coach_tool_running") {
            Coach(
                CoachUiState.Chat(
                    items = listOf(previewQuestion),
                    answering = true,
                    activeTool = WeekRhythmTool.DEFINITION.name,
                ),
            )
        }
    }

    @Test
    fun answered() {
        composeTestRule.captureScreenSizes("coach_answered") { Coach(answered) }
    }

    @Test
    fun error() {
        composeTestRule.captureScreenVariants("coach_error") {
            Coach(CoachUiState.Chat(items = listOf(previewQuestion), error = CoachError.NETWORK))
        }
    }

    @Test
    fun targetProposal() {
        composeTestRule.captureScreenVariants("coach_proposal_target") {
            Coach(
                CoachUiState.Chat(
                    items = listOf(previewMoveQuestion, ProposalCard(1, previewTargetProposal, ProposalStatus.Pending)),
                    answering = true,
                ),
            )
        }
    }

    @Test
    fun routineProposal() {
        composeTestRule.captureScreenSizes("coach_proposal_routine") {
            Coach(
                CoachUiState.Chat(
                    items = listOf(previewRoutineQuestion, ProposalCard(1, previewRoutineProposal, ProposalStatus.Pending)),
                    answering = true,
                ),
            )
        }
    }

    @Test
    fun appliedLogProposal() {
        composeTestRule.captureScreenVariants("coach_proposal_applied") {
            val applied = ProposalStatus.Applied(AppliedChange.LogCorrected(previewLogProposal))
            Coach(CoachUiState.Chat(items = listOf(previewLogQuestion, ProposalCard(1, previewLogProposal, applied))))
        }
    }

    @Test
    fun appliedTargetWithReply() {
        composeTestRule.captureScreenSizes("coach_proposal_target_applied") {
            val applied = ProposalStatus.Applied(AppliedChange.TargetChanged(previewTargetProposal))
            Coach(
                CoachUiState.Chat(
                    items = listOf(
                        previewMoveQuestion,
                        ProposalCard(1, previewTargetProposal, applied),
                        CoachMessage(id = 2, fromUser = false, text = "Done. Screens off is now set for 22:00."),
                    ),
                ),
            )
        }
    }
}
