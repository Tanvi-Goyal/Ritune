package com.ritune.feature.routine.ui.screens.routineeditor

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ritune.core.designsystem.R
import com.ritune.core.designsystem.theme.RituneTheme
import com.ritune.core.domain.usecases.routine.RoutineIssue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.minutes

@RunWith(AndroidJUnit4::class)
class RoutineEditorContentTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private val events = mutableListOf<RoutineEditorEvent>()

    private val content = RoutineEditorUiState.Content(
        isNew = false,
        name = "Morning",
        anchors = listOf(AnchorOption("wake", "Wake"), AnchorOption("lights-out", "Lights out")),
        selectedAnchorId = "wake",
        steps = listOf(
            StepRow("Stretch", 1.minutes, canMoveUp = false, canMoveDown = true),
            StepRow("Breathe", 2.minutes, canMoveUp = true, canMoveDown = false),
        ),
        issues = emptySet(),
        sheet = null,
        showDiscardDialog = false,
        isDirty = false,
    )

    private val stepSheet = EditorSheet.StepEditor(0, "Stretch", "1", "0", "", error = null)

    private fun string(id: Int, vararg args: Any) = composeTestRule.activity.getString(id, *args)

    private fun setContent(state: RoutineEditorUiState) {
        composeTestRule.setContent {
            RituneTheme { RoutineEditorContent(state, onEvent = { events += it }) }
        }
    }

    private fun setStepSheet(sheet: EditorSheet.StepEditor) {
        composeTestRule.setContent {
            RituneTheme { Column { StepEditorContent(sheet, onEvent = { events += it }) } }
        }
    }

    @Test
    fun title_dependsOnNewOrEdit() {
        setContent(content.copy(isNew = true))

        composeTestRule.onNodeWithText(string(R.string.editor_title_new)).assertIsDisplayed()
    }

    @Test
    fun form_showsSelectedAnchorAndSteps() {
        setContent(content)

        composeTestRule.onNodeWithText(string(R.string.editor_title_edit)).assertIsDisplayed()
        composeTestRule.onNodeWithText("Wake").assertIsDisplayed()
        composeTestRule.onNodeWithText("Stretch").assertIsDisplayed()
    }

    @Test
    fun moveButtons_disabledAtListEnds() {
        setContent(content)

        composeTestRule.onNodeWithContentDescription(string(R.string.editor_move_up, "Stretch")).assertIsNotEnabled()
        composeTestRule.onNodeWithContentDescription(string(R.string.editor_move_down, "Breathe")).assertIsNotEnabled()
        composeTestRule.onNodeWithContentDescription(string(R.string.editor_move_down, "Stretch")).performClick()

        assertEquals(listOf<RoutineEditorEvent>(RoutineEditorEvent.MoveStep(0, 1)), events)
    }

    @Test
    fun actions_sendTheirEvents() {
        setContent(content)

        composeTestRule.onNodeWithText(string(R.string.editor_save)).performClick()
        composeTestRule.onNodeWithText(string(R.string.editor_cancel)).performClick()
        composeTestRule.onNodeWithText("Breathe").performClick()
        composeTestRule.onNodeWithText(string(R.string.editor_add_step)).performClick()

        assertEquals(
            listOf(
                RoutineEditorEvent.SaveClicked,
                RoutineEditorEvent.CancelClicked,
                RoutineEditorEvent.EditStepClicked(1),
                RoutineEditorEvent.AddStepClicked,
            ),
            events,
        )
    }

    @Test
    fun issues_showNameAndStepMessages() {
        setContent(content.copy(name = "", steps = emptyList(), issues = setOf(RoutineIssue.NAME_BLANK, RoutineIssue.NO_STEPS)))

        composeTestRule.onNodeWithText(string(R.string.editor_issue_name_blank)).assertIsDisplayed()
        composeTestRule.onNodeWithText(string(R.string.editor_issue_no_steps)).assertIsDisplayed()
    }

    @Test
    fun discardDialog_confirmsOrKeepsEditing() {
        setContent(content.copy(showDiscardDialog = true, isDirty = true))

        composeTestRule.onNodeWithText(string(R.string.editor_keep_editing)).performClick()
        composeTestRule.onNodeWithText(string(R.string.editor_discard_confirm)).performClick()

        assertEquals(listOf(RoutineEditorEvent.DiscardDismissed, RoutineEditorEvent.DiscardConfirmed), events)
    }

    @Test
    fun anchorSheet_selectingAnchorSendsIt() {
        setContent(content.copy(sheet = EditorSheet.AnchorPicker))

        composeTestRule.onNodeWithText(string(R.string.editor_anchor_sheet_body)).assertIsDisplayed()
        composeTestRule.onNodeWithText("Lights out").performClick()

        assertEquals(listOf<RoutineEditorEvent>(RoutineEditorEvent.AnchorSelected("lights-out")), events)
    }

    @Test
    fun stepSheet_errorsShowTheirMessage() {
        setStepSheet(stepSheet.copy(error = StepError.TOO_SHORT))

        val tooShort = composeTestRule.activity.resources.getQuantityString(R.plurals.editor_step_error_short, 5, 5)
        composeTestRule.onNodeWithText(tooShort).assertIsDisplayed()
    }

    @Test
    fun stepSheet_blankTitle_showsTitleError() {
        setStepSheet(stepSheet.copy(title = "", error = StepError.TITLE_BLANK))

        composeTestRule.onNodeWithText(string(R.string.editor_step_error_title)).assertIsDisplayed()
    }

    @Test
    fun stepSheet_deleteOnlyWhenEditingExistingStep() {
        setStepSheet(stepSheet.copy(index = null))

        composeTestRule.onNodeWithText(string(R.string.editor_step_delete)).assertDoesNotExist()
    }

    @Test
    fun stepSheet_doneAndDelete_sendEvents() {
        setStepSheet(stepSheet)

        composeTestRule.onNodeWithText(string(R.string.editor_step_done)).performClick()
        composeTestRule.onNodeWithText(string(R.string.editor_step_delete)).performClick()

        assertEquals(listOf(RoutineEditorEvent.StepSheetDone, RoutineEditorEvent.DeleteStep(0)), events)
    }
}
