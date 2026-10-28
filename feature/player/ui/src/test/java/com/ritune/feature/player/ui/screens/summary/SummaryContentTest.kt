package com.ritune.feature.player.ui.screens.summary

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ritune.core.designsystem.R
import com.ritune.core.designsystem.theme.RituneTheme
import com.ritune.feature.player.ui.screens.assertFullyVisible
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

@RunWith(AndroidJUnit4::class)
class SummaryContentTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private val events = mutableListOf<SummaryUiEvent>()

    private val done = SummaryUiState.Content(
        routineName = "Morning",
        completed = true,
        completedSteps = 4,
        skippedSteps = 0,
        totalSteps = 4,
        duration = 8.minutes + 2.seconds,
        startedAt = Instant.parse("2026-01-05T08:00:00Z"),
    )

    private fun string(id: Int, vararg args: Any) = composeTestRule.activity.getString(id, *args)

    private fun setContent(state: SummaryUiState) {
        composeTestRule.setContent {
            RituneTheme { SummaryContent(state, onEvent = { events += it }) }
        }
    }

    @Test
    fun completedRun_showsDoneTitleStepsAndDuration() {
        setContent(done)

        composeTestRule.onNodeWithText("Morning done").assertIsDisplayed()
        composeTestRule.onNodeWithText("4 of 4 steps").assertIsDisplayed()
        composeTestRule.onNodeWithText("8:02").assertIsDisplayed()
    }

    @Test
    fun runWithSkips_countsThem() {
        setContent(done.copy(completedSteps = 3, skippedSteps = 1))

        composeTestRule.onNodeWithText("3 of 4 steps, 1 skipped").assertIsDisplayed()
    }

    @Test
    fun everyStepSkipped_saysSkipped() {
        setContent(done.copy(completedSteps = 0, skippedSteps = 4))

        composeTestRule.onNodeWithText("Morning skipped").assertIsDisplayed()
    }

    @Test
    fun stoppedRun_saysStoppedEarly() {
        setContent(done.copy(completed = false, completedSteps = 1))

        composeTestRule.onNodeWithText("Morning stopped").assertIsDisplayed()
        composeTestRule.onNodeWithText("1 of 4 steps before you stopped").assertIsDisplayed()
    }

    @Test
    fun deletedRoutine_usesFallbackName() {
        setContent(done.copy(routineName = null))

        composeTestRule.onNodeWithText(string(R.string.summary_title_done, string(R.string.summary_routine_fallback)))
            .assertIsDisplayed()
    }

    @Test
    fun notFound_explainsAndDoneCloses() {
        setContent(SummaryUiState.NotFound)

        composeTestRule.onNodeWithText(string(R.string.summary_not_found_title)).assertIsDisplayed()
        composeTestRule.onNodeWithText(string(R.string.summary_done)).performClick()

        assertEquals(listOf<SummaryUiEvent>(SummaryUiEvent.Done), events)
    }

    @Test
    @Config(qualifiers = "w900dp-h400dp")
    fun shortWindow_showsWholeStats() {
        setContent(done)

        assertFullyVisible(composeTestRule.onNodeWithText("8:02"))
    }

    @Test
    fun loading_hidesDone() {
        setContent(SummaryUiState.Loading)

        composeTestRule.onNodeWithText(string(R.string.summary_done)).assertDoesNotExist()
    }
}
