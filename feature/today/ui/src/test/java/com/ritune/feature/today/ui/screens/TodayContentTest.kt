package com.ritune.feature.today.ui.screens

import androidx.activity.ComponentActivity
import androidx.compose.material3.SnackbarHostState
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ritune.core.designsystem.R
import com.ritune.core.designsystem.theme.RituneTheme
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals

@RunWith(AndroidJUnit4::class)
class TodayContentTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private val events = mutableListOf<TodayEvent>()

    private val wake = AnchorItem("wake", "Wake", LocalTime(7, 0), loggedAt = null, routine = null)
    private val lightsOut = AnchorItem(
        id = "lights-out",
        name = "Lights out",
        targetTime = LocalTime(22, 30),
        loggedAt = null,
        routine = RoutineRef("wind-down", "Wind down"),
    )

    private fun content(
        anchors: List<AnchorItem> = listOf(wake, lightsOut),
        runningRoutineName: String? = null,
    ) = TodayUiState.Content(LocalDate(2026, 1, 5), anchors, runningRoutineName)

    private fun string(id: Int, vararg args: Any) = composeTestRule.activity.getString(id, *args)

    private fun setContent(state: TodayUiState) {
        composeTestRule.setContent {
            RituneTheme {
                TodayContent(
                    state = state,
                    snackbarHostState = SnackbarHostState(),
                    onEvent = { events += it },
                    onOpenSettings = {},
                )
            }
        }
    }

    @Test
    fun nothingLogged_showsEmptyHeadline() {
        setContent(content())

        composeTestRule.onNodeWithText(string(R.string.today_title)).assertIsDisplayed()
        composeTestRule.onNodeWithText(string(R.string.today_headline_empty)).assertIsDisplayed()
    }

    @Test
    fun everythingLogged_showsDoneHeadline() {
        setContent(content(anchors = listOf(wake.copy(loggedAt = LocalTime(7, 5)))))

        composeTestRule.onNodeWithText(string(R.string.today_headline_done)).assertIsDisplayed()
    }

    @Test
    fun tappingAnchor_logsIt() {
        setContent(content())

        composeTestRule.onNodeWithText("Wake", substring = true).performClick()

        assertEquals(listOf<TodayEvent>(TodayEvent.LogAnchor("wake")), events)
    }

    @Test
    fun anchorWithoutRoutine_actionLogsIt() {
        setContent(content())

        composeTestRule.onNodeWithText(string(R.string.today_action_log)).performClick()

        assertEquals(listOf<TodayEvent>(TodayEvent.LogAnchor("wake")), events)
    }

    @Test
    fun anchorWithRoutine_actionStartsRoutine() {
        setContent(content())

        composeTestRule.onNodeWithText(string(R.string.today_action_start_routine)).performClick()

        assertEquals(listOf<TodayEvent>(TodayEvent.StartRoutine("wind-down")), events)
    }

    @Test
    fun runningBanner_opensPlayer() {
        setContent(content(runningRoutineName = "Wind down"))

        composeTestRule.onNodeWithText(string(R.string.today_running_banner, "Wind down")).performClick()

        assertEquals(listOf<TodayEvent>(TodayEvent.OpenRunningRoutine), events)
    }

    @Test
    fun scrollPosition_survivesRecreation() {
        val anchors = (1..20).map { wake.copy(id = "a$it", name = "Anchor $it") }
        val restorationTester = StateRestorationTester(composeTestRule)
        restorationTester.setContent {
            RituneTheme {
                TodayContent(content(anchors), SnackbarHostState(), onEvent = {}, onOpenSettings = {})
            }
        }
        composeTestRule.onNodeWithText("Anchor 20", substring = true).performScrollTo()

        restorationTester.emulateSavedInstanceStateRestore()

        composeTestRule.onNodeWithText("Anchor 20", substring = true).assertIsDisplayed()
    }
}
