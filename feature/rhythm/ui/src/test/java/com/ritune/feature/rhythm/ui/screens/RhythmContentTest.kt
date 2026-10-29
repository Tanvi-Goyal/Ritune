package com.ritune.feature.rhythm.ui.screens

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ritune.core.designsystem.R
import com.ritune.core.designsystem.theme.RituneTheme
import kotlinx.datetime.LocalDate
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import kotlin.test.assertEquals

@RunWith(AndroidJUnit4::class)
class RhythmContentTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    // 2026-01-05 is a Monday.
    private val days = listOf(
        DayBar(LocalDate(2026, 1, 4), null),
        DayBar(LocalDate(2026, 1, 5), 0.75f),
    )

    private fun content(summary: RhythmSummary) = RhythmUiState.Content(LocalDate(2026, 1, 5), days, summary)

    private fun string(id: Int, vararg args: Any) = composeTestRule.activity.getString(id, *args)

    private fun setContent(state: RhythmUiState, onOpenSettings: () -> Unit = {}) {
        composeTestRule.setContent {
            RituneTheme { RhythmContent(state, onOpenSettings) }
        }
    }

    @Test
    fun early_showsLoggedDayProgress() {
        setContent(content(RhythmSummary.Early(loggedDays = 1, requiredDays = 3)))

        composeTestRule.onNodeWithText(string(R.string.rhythm_early_title)).assertIsDisplayed()
        composeTestRule.onNodeWithText(string(R.string.rhythm_early_body, 1, 3)).assertIsDisplayed()
    }

    @Test
    fun building_showsKeepLoggingMessage() {
        setContent(content(RhythmSummary.Building))

        composeTestRule.onNodeWithText(string(R.string.rhythm_building_title)).assertIsDisplayed()
    }

    @Test
    @Config(qualifiers = "en")
    fun scored_showsPercentStatsAndDescribesEachDay() {
        setContent(content(RhythmSummary.Scored(82, wakeDriftMinutes = 12, steadiestName = "Lights out")))

        composeTestRule.onNodeWithText("82%").assertIsDisplayed()
        composeTestRule.onNodeWithText("12 minutes").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("Lights out").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Monday, 75 percent").assertExists()
        composeTestRule.onNodeWithContentDescription("Sunday, no data").assertExists()
    }

    @Test
    fun scored_withoutStats_saysNotYet() {
        setContent(content(RhythmSummary.Scored(50, wakeDriftMinutes = null, steadiestName = null)))

        assertEquals(
            2,
            composeTestRule.onAllNodesWithText(string(R.string.rhythm_value_none)).fetchSemanticsNodes().size,
        )
    }

    @Test
    fun settings_opensSettings() {
        var opened = false
        setContent(content(RhythmSummary.Building), onOpenSettings = { opened = true })

        composeTestRule.onNodeWithText(string(R.string.rhythm_settings)).performClick()

        assertEquals(true, opened)
    }

    @Test
    fun loading_showsNothing() {
        setContent(RhythmUiState.Loading)

        composeTestRule.onNodeWithText(string(R.string.rhythm_title)).assertDoesNotExist()
    }

    @Test
    @Config(qualifiers = "w320dp-h400dp")
    fun scrollPosition_survivesRecreation() {
        val restorationTester = StateRestorationTester(composeTestRule)
        restorationTester.setContent {
            RituneTheme { RhythmContent(content(RhythmSummary.Scored(82, 12, "Lights out")), onOpenSettings = {}) }
        }
        composeTestRule.onNodeWithText("Lights out").performScrollTo()

        restorationTester.emulateSavedInstanceStateRestore()

        composeTestRule.onNodeWithText("Lights out").assertIsDisplayed()
    }
}
