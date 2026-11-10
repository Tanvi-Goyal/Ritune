package com.ritune.feature.coach.ui.screens

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ritune.core.designsystem.R
import com.ritune.core.designsystem.theme.RituneTheme
import com.ritune.core.domain.usecases.coach.tools.RoutinesTool
import com.ritune.core.models.coach.CoachError
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals

@RunWith(AndroidJUnit4::class)
class CoachContentTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private val events = mutableListOf<CoachUiEvent>()
    private var backs = 0

    private fun string(id: Int) = composeTestRule.activity.getString(id)

    private fun setContent(state: CoachUiState) {
        composeTestRule.setContent {
            RituneTheme { CoachContent(state, onEvent = { events += it }, onBack = { backs++ }) }
        }
    }

    @Test
    fun consent_turnOnAndNotNow() {
        setContent(CoachUiState.Consent)

        composeTestRule.onNodeWithText(string(R.string.coach_consent_use)).assertIsDisplayed()
        composeTestRule.onNodeWithText(string(R.string.coach_consent_turn_on)).performScrollTo()
            .performClick()
        composeTestRule.onNodeWithText(string(R.string.coach_consent_not_now)).performScrollTo()
            .performClick()

        assertEquals(listOf<CoachUiEvent>(CoachUiEvent.TurnOn), events)
        assertEquals(1, backs)
    }

    @Test
    fun emptyChat_starterAsksItsQuestion() {
        setContent(CoachUiState.Chat())

        composeTestRule.onNodeWithText(string(R.string.coach_starter_week)).performClick()

        assertEquals(
            listOf<CoachUiEvent>(CoachUiEvent.Ask(string(R.string.coach_starter_week))),
            events
        )
    }

    @Test
    fun blankInput_cannotBeSent() {
        setContent(CoachUiState.Chat(input = " "))

        composeTestRule.onNodeWithText(string(R.string.coach_send)).assertIsNotEnabled()
    }

    @Test
    fun answering_showsTheToolAndStop() {
        setContent(
            CoachUiState.Chat(
                messages = listOf(previewQuestion),
                answering = true,
                activeTool = RoutinesTool.DEFINITION.name,
            ),
        )

        composeTestRule.onNodeWithText(string(R.string.coach_tool_routines)).assertIsDisplayed()
        composeTestRule.onNodeWithText(string(R.string.coach_stop)).performClick()

        assertEquals(listOf<CoachUiEvent>(CoachUiEvent.Stop), events)
    }

    @Test
    fun error_showsItsMessageAndRetries() {
        setContent(
            CoachUiState.Chat(
                messages = listOf(previewQuestion),
                error = CoachError.QUOTA_EXCEEDED
            )
        )

        composeTestRule.onNodeWithText(string(R.string.coach_error_quota)).assertIsDisplayed()
        composeTestRule.onNodeWithText(string(R.string.coach_retry)).performClick()

        assertEquals(listOf<CoachUiEvent>(CoachUiEvent.Retry), events)
    }

    @Test
    fun answeringBeforeAnyText_showsThinking() {
        val emptyAnswer = CoachMessage(id = 1, fromUser = false, text = "")
        setContent(
            CoachUiState.Chat(
                messages = listOf(previewQuestion, emptyAnswer),
                answering = true
            )
        )

        composeTestRule.onNodeWithText(string(R.string.coach_thinking)).assertIsDisplayed()
    }

    @Test
    fun streamingText_hidesThinking() {
        setContent(
            CoachUiState.Chat(
                messages = listOf(previewQuestion, previewAnswer),
                answering = true
            )
        )

        composeTestRule.onNodeWithText(string(R.string.coach_thinking)).assertDoesNotExist()
    }
}
