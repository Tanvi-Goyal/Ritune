package com.ritune.feature.routine.ui.screens.routinedetail

import androidx.activity.ComponentActivity
import androidx.compose.material3.SnackbarHostState
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ritune.core.designsystem.R
import com.ritune.core.designsystem.theme.RituneTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

@RunWith(AndroidJUnit4::class)
class RoutineDetailContentTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private val events = mutableListOf<RoutineDetailEvent>()
    private val edited = mutableListOf<String>()
    private var backClicks = 0

    private val content = RoutineDetailUiState.Content(
        id = "morning",
        name = "Morning",
        steps = listOf(StepItem("Stretch", 90.seconds), StepItem("Breathe", 1.minutes)),
        totalDuration = 150.seconds,
        anchorName = "Wake",
        builtIn = false,
        confirmingDelete = false,
    )

    private fun string(id: Int, vararg args: Any) = composeTestRule.activity.getString(id, *args)

    private fun setContent(state: RoutineDetailUiState) {
        composeTestRule.setContent {
            RituneTheme {
                RoutineDetailContent(
                    state = state,
                    snackbarHostState = SnackbarHostState(),
                    onEvent = { events += it },
                    onBack = { backClicks++ },
                    onEdit = { edited += it },
                )
            }
        }
    }

    @Test
    fun content_showsSummaryAndDescribesEachStep() {
        setContent(content)

        composeTestRule.onNodeWithText("2 min 30 sec · 2 steps · Logs Wake when you start").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Step 1, Stretch, 1 min 30 sec").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Step 2, Breathe, 1 min").assertIsDisplayed()
    }

    @Test
    fun startEditAndBack_reachTheirHandlers() {
        setContent(content)

        composeTestRule.onNodeWithText(string(R.string.detail_start)).performClick()
        composeTestRule.onNodeWithText(string(R.string.detail_edit)).performClick()
        composeTestRule.onNodeWithText(string(R.string.detail_back)).performClick()

        assertEquals(listOf<RoutineDetailEvent>(RoutineDetailEvent.Start), events)
        assertEquals(listOf("morning"), edited)
        assertEquals(1, backClicks)
    }

    @Test
    fun builtInRoutine_hidesDelete() {
        setContent(content.copy(builtIn = true))

        composeTestRule.onNodeWithText(string(R.string.detail_delete)).assertDoesNotExist()
    }

    @Test
    fun deleteFlow_asksBeforeDeleting() {
        setContent(content.copy(confirmingDelete = true))

        composeTestRule.onNodeWithText(string(R.string.detail_delete_body, "Morning")).assertIsDisplayed()
        composeTestRule.onNodeWithText(string(R.string.detail_delete_confirm)).performClick()
        composeTestRule.onNodeWithText(string(R.string.detail_cancel)).performClick()

        assertEquals(listOf(RoutineDetailEvent.ConfirmDelete, RoutineDetailEvent.DismissDelete), events)
    }

    @Test
    fun deleteButton_requestsConfirmation() {
        setContent(content)

        composeTestRule.onNodeWithText(string(R.string.detail_delete)).performClick()

        assertEquals(listOf<RoutineDetailEvent>(RoutineDetailEvent.RequestDelete), events)
    }

    @Test
    fun notFound_offersWayBack() {
        setContent(RoutineDetailUiState.NotFound)

        composeTestRule.onNodeWithText(string(R.string.detail_not_found_title)).assertIsDisplayed()
        composeTestRule.onNodeWithText(string(R.string.detail_back)).performClick()
        assertEquals(1, backClicks)
    }
}
