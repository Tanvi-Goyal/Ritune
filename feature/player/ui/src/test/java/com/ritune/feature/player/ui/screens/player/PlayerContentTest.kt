package com.ritune.feature.player.ui.screens.player

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ritune.core.designsystem.R
import com.ritune.core.designsystem.theme.RituneTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.seconds

@RunWith(AndroidJUnit4::class)
class PlayerContentTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private val events = mutableListOf<PlayerUiEvent>()

    private val running = PlayerUiState.Active(
        routineName = "Evening reset",
        stepIndex = 1,
        stepCount = 4,
        stepTitle = "Light stretch",
        instruction = null,
        nextStepTitle = "Breathe",
        remaining = 134.seconds,
        isPaused = false,
        confirmingStop = false,
    )

    private fun string(id: Int, vararg args: Any) = composeTestRule.activity.getString(id, *args)

    private fun setContent(state: PlayerUiState) {
        composeTestRule.setContent {
            RituneTheme { PlayerContent(state, onEvent = { events += it }) }
        }
    }

    @Test
    fun running_showsStepCountdownAndNextStep() {
        setContent(running)

        composeTestRule.onNodeWithText(string(R.string.player_step_counter, 2, 4)).assertIsDisplayed()
        composeTestRule.onNodeWithText("Light stretch").assertIsDisplayed()
        composeTestRule.onNodeWithText("2:14").assertIsDisplayed()
        composeTestRule.onNodeWithText(string(R.string.player_next, "Breathe")).assertIsDisplayed()
        composeTestRule.onNodeWithText(string(R.string.player_pause)).assertIsDisplayed()
        composeTestRule.onNodeWithText(string(R.string.player_skip)).assertIsDisplayed()
    }

    @Test
    fun pausedLastStep_offersResumeAndFinish() {
        setContent(running.copy(isPaused = true, nextStepTitle = null))

        composeTestRule.onNodeWithText(string(R.string.player_paused)).assertIsDisplayed()
        composeTestRule.onNodeWithText(string(R.string.player_last_step)).assertIsDisplayed()
        composeTestRule.onNodeWithText(string(R.string.player_resume)).assertIsDisplayed()
        composeTestRule.onNodeWithText(string(R.string.player_finish)).assertIsDisplayed()
    }

    @Test
    fun instruction_showsOnlyWhenPresent() {
        setContent(running.copy(instruction = "Reach up slowly."))

        composeTestRule.onNodeWithText("Reach up slowly.").assertIsDisplayed()
    }

    @Test
    fun controls_sendTheirEvents() {
        setContent(running)

        composeTestRule.onNodeWithText(string(R.string.player_pause)).performClick()
        composeTestRule.onNodeWithText(string(R.string.player_skip)).performClick()
        composeTestRule.onNodeWithText(string(R.string.player_stop)).performClick()

        assertEquals(listOf(PlayerUiEvent.TogglePause, PlayerUiEvent.Skip, PlayerUiEvent.RequestStop), events)
    }

    @Test
    fun stopDialog_confirmsOrKeepsGoing() {
        setContent(running.copy(confirmingStop = true))

        composeTestRule.onNodeWithText(string(R.string.player_stop_body)).assertIsDisplayed()
        composeTestRule.onNodeWithText(string(R.string.player_keep_going)).performClick()
        composeTestRule.onAllNodesWithText(string(R.string.player_stop_confirm)).onLast().performClick()

        assertEquals(listOf(PlayerUiEvent.DismissStop, PlayerUiEvent.ConfirmStop), events)
    }

    @Test
    fun loading_showsNothing() {
        setContent(PlayerUiState.Loading)

        composeTestRule.onNodeWithText(string(R.string.player_stop)).assertDoesNotExist()
    }
}
