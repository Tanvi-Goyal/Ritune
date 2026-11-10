package com.ritune.feature.coach.ui.screens

import androidx.activity.ComponentActivity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ritune.core.designsystem.theme.RituneTheme
import com.ritune.core.domain.usecases.coach.tools.WeekRhythmTool
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

    private val answered = CoachUiState.Chat(messages = listOf(previewQuestion, previewAnswer))

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
            Coach(CoachUiState.Chat(messages = listOf(previewQuestion, partial), answering = true))
        }
    }

    @Test
    fun toolRunning() {
        composeTestRule.captureScreenVariants("coach_tool_running") {
            Coach(
                CoachUiState.Chat(
                    messages = listOf(previewQuestion),
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
            Coach(CoachUiState.Chat(messages = listOf(previewQuestion), error = CoachError.NETWORK))
        }
    }
}
