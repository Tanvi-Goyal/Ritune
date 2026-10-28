package com.ritune.feature.routine.ui.screens.routineslist

import androidx.activity.ComponentActivity
import androidx.compose.material3.SnackbarHostState
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ritune.core.designsystem.R
import com.ritune.core.designsystem.theme.RituneTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.minutes

@RunWith(AndroidJUnit4::class)
class RoutinesListContentTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private val events = mutableListOf<RoutinesEvent>()
    private val opened = mutableListOf<String>()
    private var newRoutineClicks = 0

    private val morning = RoutineItem("morning", "Morning", 8.minutes, stepCount = 4, anchorName = "Wake")
    private val focus = RoutineItem("focus", "Focus", 25.minutes, stepCount = 1, anchorName = null)

    private fun string(id: Int, vararg args: Any) = composeTestRule.activity.getString(id, *args)

    private fun setContent(state: RoutinesUiState) {
        composeTestRule.setContent {
            RituneTheme {
                RoutinesListContent(
                    state = state,
                    snackbarHostState = SnackbarHostState(),
                    onEvent = { events += it },
                    onOpenRoutine = { opened += it },
                    onNewRoutine = { newRoutineClicks++ },
                    onOpenSettings = {},
                )
            }
        }
    }

    @Test
    fun noRoutines_showsEmptyMessageAndNewButton() {
        setContent(RoutinesUiState.Content(emptyList()))

        composeTestRule.onNodeWithText(string(R.string.routines_empty_title)).assertIsDisplayed()
        composeTestRule.onNodeWithText(string(R.string.routines_new)).performClick()
        assertEquals(1, newRoutineClicks)
    }

    @Test
    fun routines_showSummaryWithAnchor() {
        setContent(RoutinesUiState.Content(listOf(morning, focus)))

        composeTestRule.onNodeWithText("8 min · 4 steps · Starts Wake").assertIsDisplayed()
        composeTestRule.onNodeWithText("25 min · 1 step").assertIsDisplayed()
    }

    @Test
    fun tappingRoutine_opensIt() {
        setContent(RoutinesUiState.Content(listOf(morning)))

        composeTestRule.onNodeWithText("Morning", substring = true).performClick()

        assertEquals(listOf("morning"), opened)
    }

    @Test
    fun start_sendsStartForThatRoutine() {
        setContent(RoutinesUiState.Content(listOf(morning, focus)))

        composeTestRule.onAllNodesWithText(string(R.string.routines_start))[1].performClick()

        assertEquals(listOf<RoutinesEvent>(RoutinesEvent.Start("focus")), events)
    }

    @Test
    fun scrollPosition_survivesRecreation() {
        val many = (1..20).map { morning.copy(id = "r$it", name = "Routine $it") }
        val restorationTester = StateRestorationTester(composeTestRule)
        restorationTester.setContent {
            RituneTheme {
                RoutinesListContent(RoutinesUiState.Content(many), SnackbarHostState(), {}, {}, {}, {})
            }
        }
        composeTestRule.onNodeWithText("Routine 20").performScrollTo()

        restorationTester.emulateSavedInstanceStateRestore()

        composeTestRule.onNodeWithText("Routine 20").assertIsDisplayed()
    }

    @Test
    fun loading_showsNothing() {
        setContent(RoutinesUiState.Loading)

        composeTestRule.onNodeWithText(string(R.string.routines_title)).assertDoesNotExist()
    }
}
