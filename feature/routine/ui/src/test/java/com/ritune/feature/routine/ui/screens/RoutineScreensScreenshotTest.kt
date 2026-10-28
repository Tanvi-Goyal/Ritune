package com.ritune.feature.routine.ui.screens

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ritune.core.designsystem.theme.RituneTheme
import com.ritune.core.domain.usecases.routine.RoutineIssue
import com.ritune.core.screenshottesting.captureComponent
import com.ritune.core.screenshottesting.captureScreenSizes
import com.ritune.core.screenshottesting.captureScreenVariants
import com.ritune.feature.routine.ui.screens.routinedetail.RoutineDetailContent
import com.ritune.feature.routine.ui.screens.routinedetail.RoutineDetailUiState
import com.ritune.feature.routine.ui.screens.routinedetail.StepItem
import com.ritune.feature.routine.ui.screens.routineeditor.AnchorOption
import com.ritune.feature.routine.ui.screens.routineeditor.EditorSheet
import com.ritune.feature.routine.ui.screens.routineeditor.RoutineEditorContent
import com.ritune.feature.routine.ui.screens.routineeditor.RoutineEditorUiState
import com.ritune.feature.routine.ui.screens.routineeditor.StepEditorContent
import com.ritune.feature.routine.ui.screens.routineeditor.StepError
import com.ritune.feature.routine.ui.screens.routineeditor.StepRow
import com.ritune.feature.routine.ui.screens.routineslist.RoutineItem
import com.ritune.feature.routine.ui.screens.routineslist.RoutinesListContent
import com.ritune.feature.routine.ui.screens.routineslist.RoutinesUiState
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.GraphicsMode
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class RoutineScreensScreenshotTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private val list = RoutinesUiState.Content(
        listOf(
            RoutineItem("morning-start", "Morning start", 8.minutes, 4, "Wake"),
            RoutineItem("wind-down", "Wind down", 12.minutes + 30.seconds, 5, "Screens off"),
            RoutineItem("focus", "Focus block", 25.minutes, 1, null),
        ),
    )

    private val detail = RoutineDetailUiState.Content(
        id = "morning-start",
        name = "Morning start",
        steps = listOf(
            StepItem("Glass of water", 30.seconds),
            StepItem("Light stretch", 3.minutes),
            StepItem("Step outside", 2.minutes),
            StepItem("Plan the day", 2.minutes + 30.seconds),
        ),
        totalDuration = 8.minutes,
        anchorName = "Wake",
        builtIn = false,
        confirmingDelete = false,
    )

    private val editor = RoutineEditorUiState.Content(
        isNew = false,
        name = "Morning start",
        anchors = listOf(AnchorOption("wake", "Wake")),
        selectedAnchorId = "wake",
        steps = listOf(
            StepRow("Glass of water", 30.seconds, canMoveUp = false, canMoveDown = true),
            StepRow("Light stretch", 3.minutes, canMoveUp = true, canMoveDown = true),
            StepRow("Step outside", 2.minutes, canMoveUp = true, canMoveDown = false),
        ),
        issues = emptySet(),
        sheet = null,
        showDiscardDialog = false,
        isDirty = false,
    )

    @Composable
    private fun Screen(content: @Composable () -> Unit) {
        RituneTheme {
            Surface(color = MaterialTheme.colorScheme.background) { content() }
        }
    }

    @Composable
    private fun List(state: RoutinesUiState) = Screen {
        RoutinesListContent(state, SnackbarHostState(), {}, {}, {}, {})
    }

    @Composable
    private fun Detail(state: RoutineDetailUiState) = Screen {
        RoutineDetailContent(state, SnackbarHostState(), {}, {}, {})
    }

    @Composable
    private fun Editor(state: RoutineEditorUiState) = Screen { RoutineEditorContent(state, onEvent = {}) }

    @Test
    fun routines_acrossScreenSizes() {
        composeTestRule.captureScreenSizes("routines") { List(list) }
    }

    @Test
    fun routines_darkAndLargeFont() {
        composeTestRule.captureScreenVariants("routines") { List(list) }
    }

    @Test
    fun routines_empty() {
        composeTestRule.captureScreenVariants("routines_empty") { List(RoutinesUiState.Content(emptyList())) }
    }

    @Test
    fun detail_acrossScreenSizes() {
        composeTestRule.captureScreenSizes("routine_detail") { Detail(detail) }
    }

    @Test
    fun detail_darkAndLargeFont() {
        composeTestRule.captureScreenVariants("routine_detail") { Detail(detail) }
    }

    @Test
    fun detail_notFound() {
        composeTestRule.captureScreenVariants("routine_detail_not_found") { Detail(RoutineDetailUiState.NotFound) }
    }

    @Test
    fun editor_acrossScreenSizes() {
        composeTestRule.captureScreenSizes("routine_editor") { Editor(editor) }
    }

    @Test
    fun editor_darkAndLargeFont() {
        composeTestRule.captureScreenVariants("routine_editor") { Editor(editor) }
    }

    @Test
    fun editor_newWithIssues() {
        val invalid = editor.copy(
            isNew = true,
            name = "",
            selectedAnchorId = null,
            steps = emptyList(),
            issues = setOf(RoutineIssue.NAME_BLANK, RoutineIssue.NO_STEPS),
        )
        composeTestRule.captureScreenVariants("routine_editor_issues") { Editor(invalid) }
    }

    @Test
    fun editor_stepSheetWithError() {
        val sheet = EditorSheet.StepEditor(0, "Light stretch", "0", "3", "", StepError.TOO_SHORT)
        composeTestRule.captureComponent("routine_step_sheet") {
            RituneTheme {
                Surface {
                    Column(Modifier.padding(16.dp)) { StepEditorContent(sheet, onEvent = {}) }
                }
            }
        }
    }
}
