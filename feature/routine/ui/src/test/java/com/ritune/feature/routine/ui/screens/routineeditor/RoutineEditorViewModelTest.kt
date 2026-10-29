package com.ritune.feature.routine.ui.screens.routineeditor

import androidx.lifecycle.SavedStateHandle
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.turbine.test
import com.ritune.core.domain.usecases.anchor.ObserveAnchors
import com.ritune.core.domain.usecases.routine.ObserveRoutine
import com.ritune.core.domain.usecases.routine.RoutineIssue
import com.ritune.core.domain.usecases.routine.SaveRoutine
import com.ritune.core.models.DefaultCatalog
import com.ritune.core.models.RoutineId
import com.ritune.core.models.StepId
import com.ritune.core.models.defaultStepCues
import com.ritune.core.testing.data.aRoutine
import com.ritune.core.testing.data.aStep
import com.ritune.core.testing.id.SequentialIdGenerator
import com.ritune.core.testing.repository.FakeAnchorRepository
import com.ritune.core.testing.repository.FakeRoutineRepository
import com.ritune.core.testing.rules.MainDispatcherRule
import com.ritune.feature.routine.ui.screens.routineeditor.RoutineEditorEvent.AddStepClicked
import com.ritune.feature.routine.ui.screens.routineeditor.RoutineEditorEvent.NameChanged
import com.ritune.feature.routine.ui.screens.routineeditor.RoutineEditorEvent.StepMinutesChanged
import com.ritune.feature.routine.ui.screens.routineeditor.RoutineEditorEvent.StepSecondsChanged
import com.ritune.feature.routine.ui.screens.routineeditor.RoutineEditorEvent.StepSheetDone
import com.ritune.feature.routine.ui.screens.routineeditor.RoutineEditorEvent.StepTitleChanged
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

// SavedStateHandle.toRoute() builds a Bundle, so these run on Robolectric.
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class RoutineEditorViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val existing = aRoutine(
        id = "existing",
        name = "Morning",
        anchorId = DefaultCatalog.wake.id,
        steps = listOf(
            aStep(id = "a", title = "Stretch", duration = 90.seconds, instruction = "Slowly"),
            aStep(id = "b", title = "Breathe", duration = 1.minutes),
            aStep(id = "c", title = "Walk", duration = 2.minutes),
        ),
    )
    private val routines = FakeRoutineRepository(listOf(existing))

    private fun TestScope.newViewModel(routineId: String? = null): RoutineEditorViewModel {
        val viewModel = RoutineEditorViewModel(
            SavedStateHandle(if (routineId == null) emptyMap() else mapOf("routineId" to routineId)),
            ObserveRoutine(routines),
            ObserveAnchors(FakeAnchorRepository()),
            SaveRoutine(routines, SequentialIdGenerator("new")),
        )
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.state.collect() }
        return viewModel
    }

    private fun RoutineEditorViewModel.content() = assertIs<RoutineEditorUiState.Content>(state.value)
    private fun RoutineEditorViewModel.stepSheet() = assertIs<EditorSheet.StepEditor>(content().sheet)

    private fun RoutineEditorViewModel.addStep(title: String, minutes: String = "1", seconds: String = "0") {
        onEvent(AddStepClicked)
        onEvent(StepTitleChanged(title))
        onEvent(StepMinutesChanged(minutes))
        onEvent(StepSecondsChanged(seconds))
        onEvent(StepSheetDone)
    }

    @Test
    fun newRoutine_startsEmptyAndClean() = runTest {
        val content = newViewModel().content()

        assertTrue(content.isNew)
        assertEquals("", content.name)
        assertEquals(emptyList(), content.steps)
        assertEquals(DefaultCatalog.anchors.map { it.name }, content.anchors.map { it.name })
        assertFalse(content.isDirty)
    }

    @Test
    fun existingRoutine_prefillsFormWithMoveLimits() = runTest {
        val content = newViewModel("existing").content()

        assertFalse(content.isNew)
        assertEquals("Morning", content.name)
        assertEquals("wake", content.selectedAnchorId)
        assertEquals(
            listOf(
                StepRow("Stretch", 90.seconds, canMoveUp = false, canMoveDown = true),
                StepRow("Breathe", 1.minutes, canMoveUp = true, canMoveDown = true),
                StepRow("Walk", 2.minutes, canMoveUp = true, canMoveDown = false),
            ),
            content.steps,
        )
    }

    @Test
    fun missingRoutine_finishesEditor() = runTest {
        val viewModel = newViewModel("missing")

        viewModel.effects.test {
            assertEquals(RoutineEditorEffect.Finished, awaitItem())
        }
        assertEquals(RoutineEditorUiState.Loading, viewModel.state.value)
    }

    @Test
    fun nameChanged_marksDirty() = runTest {
        val viewModel = newViewModel()

        viewModel.onEvent(NameChanged("Evening"))

        assertEquals("Evening", viewModel.content().name)
        assertTrue(viewModel.content().isDirty)
    }

    @Test
    fun anchorPicker_opensAndSelectionClosesIt() = runTest {
        val viewModel = newViewModel("existing")

        viewModel.onEvent(RoutineEditorEvent.AnchorClicked)
        assertEquals(EditorSheet.AnchorPicker, viewModel.content().sheet)

        viewModel.onEvent(RoutineEditorEvent.AnchorSelected(null))
        assertNull(viewModel.content().selectedAnchorId)
        assertNull(viewModel.content().sheet)
    }

    @Test
    fun addStep_opensSheetWithOneMinuteDefault() = runTest {
        val viewModel = newViewModel()

        viewModel.onEvent(AddStepClicked)

        assertEquals(EditorSheet.StepEditor(null, "", "1", "0", "", null), viewModel.stepSheet())
    }

    @Test
    fun stepSheetDone_validStep_appendsTrimmedStepAndCloses() = runTest {
        val viewModel = newViewModel()

        viewModel.addStep("  Stretch ", minutes = "1", seconds = "30")

        assertEquals(listOf(StepRow("Stretch", 90.seconds, false, false)), viewModel.content().steps)
        assertNull(viewModel.content().sheet)
    }

    @Test
    fun stepSheetDone_invalidStep_showsErrorAndKeepsSheet() = runTest {
        val viewModel = newViewModel()
        val cases = listOf(
            Triple(" ", "1", StepError.TITLE_BLANK),
            Triple("Stretch", "0", StepError.TOO_SHORT),
            Triple("Stretch", "61", StepError.TOO_LONG),
        )

        cases.forEach { (title, minutes, error) ->
            viewModel.addStep(title, minutes = minutes, seconds = if (minutes == "0") "4" else "0")

            assertEquals(error, viewModel.stepSheet().error)
            assertEquals(emptyList(), viewModel.content().steps)
            viewModel.onEvent(RoutineEditorEvent.DismissSheet)
        }
    }

    @Test
    fun editingStepSheet_clearsErrorAndKeepsOnlyDigits() = runTest {
        val viewModel = newViewModel()
        viewModel.addStep(" ")

        viewModel.onEvent(StepMinutesChanged("1a2345"))
        viewModel.onEvent(StepSecondsChanged("7x89"))

        val sheet = viewModel.stepSheet()
        assertNull(sheet.error)
        assertEquals("123", sheet.minutes)
        assertEquals("78", sheet.seconds)
    }

    @Test
    fun editStep_prefillsSheetAndReplacesStep() = runTest {
        val viewModel = newViewModel("existing")

        viewModel.onEvent(RoutineEditorEvent.EditStepClicked(0))
        assertEquals(EditorSheet.StepEditor(0, "Stretch", "1", "30", "Slowly", null), viewModel.stepSheet())

        viewModel.onEvent(StepTitleChanged("Reach"))
        viewModel.onEvent(StepSheetDone)

        assertEquals(listOf("Reach", "Breathe", "Walk"), viewModel.content().steps.map { it.title })
    }

    @Test
    fun editStep_unknownIndex_isIgnored() = runTest {
        val viewModel = newViewModel("existing")

        viewModel.onEvent(RoutineEditorEvent.EditStepClicked(5))

        assertNull(viewModel.content().sheet)
    }

    @Test
    fun deleteAndMoveSteps_reorderWithinBounds() = runTest {
        val viewModel = newViewModel("existing")
        fun titles() = viewModel.content().steps.map { it.title }

        viewModel.onEvent(RoutineEditorEvent.MoveStep(2, -1))
        assertEquals(listOf("Stretch", "Walk", "Breathe"), titles())

        viewModel.onEvent(RoutineEditorEvent.MoveStep(0, -1))
        viewModel.onEvent(RoutineEditorEvent.MoveStep(2, 1))
        assertEquals(listOf("Stretch", "Walk", "Breathe"), titles())

        viewModel.onEvent(RoutineEditorEvent.DeleteStep(0))
        assertEquals(listOf("Walk", "Breathe"), titles())
    }

    @Test
    fun save_invalidForm_showsIssuesOnlyAfterTrying() = runTest {
        val viewModel = newViewModel()
        assertEquals(emptySet(), viewModel.content().issues)

        viewModel.onEvent(RoutineEditorEvent.SaveClicked)

        assertEquals(setOf(RoutineIssue.NAME_BLANK, RoutineIssue.NO_STEPS), viewModel.content().issues)
        viewModel.onEvent(NameChanged("Evening"))
        assertEquals(setOf(RoutineIssue.NO_STEPS), viewModel.content().issues)
    }

    @Test
    fun save_newRoutine_savesWithDefaultCuesAndFinishes() = runTest {
        val viewModel = newViewModel()
        viewModel.onEvent(NameChanged("Evening"))
        viewModel.addStep("Stretch")

        viewModel.effects.test {
            viewModel.onEvent(RoutineEditorEvent.SaveClicked)

            assertEquals(RoutineEditorEffect.Finished, awaitItem())
        }
        val saved = routines.get(RoutineId("new-1"))!!
        assertEquals("Evening", saved.name)
        assertEquals(defaultStepCues("Stretch"), saved.steps.single().cues)
        assertNull(saved.steps.single().instruction)
    }

    @Test
    fun save_existingRoutine_keepsItsStepIds() = runTest {
        val viewModel = newViewModel("existing")
        viewModel.onEvent(NameChanged("Morning reset"))

        viewModel.effects.test {
            viewModel.onEvent(RoutineEditorEvent.SaveClicked)
            awaitItem()
        }
        val saved = routines.get(existing.id)!!
        assertEquals("Morning reset", saved.name)
        assertEquals(listOf(StepId("a"), StepId("b"), StepId("c")), saved.steps.map { it.id })
        assertEquals("Slowly", saved.steps.first().instruction)
    }

    @Test
    fun cancel_withoutChanges_finishes() = runTest {
        val viewModel = newViewModel("existing")

        viewModel.effects.test {
            viewModel.onEvent(RoutineEditorEvent.CancelClicked)

            assertEquals(RoutineEditorEffect.Finished, awaitItem())
        }
    }

    @Test
    fun cancel_withChanges_asksBeforeDiscarding() = runTest {
        val viewModel = newViewModel("existing")
        viewModel.onEvent(NameChanged("Changed"))

        viewModel.effects.test {
            viewModel.onEvent(RoutineEditorEvent.CancelClicked)
            assertTrue(viewModel.content().showDiscardDialog)
            expectNoEvents()

            viewModel.onEvent(RoutineEditorEvent.DiscardDismissed)
            assertFalse(viewModel.content().showDiscardDialog)

            viewModel.onEvent(RoutineEditorEvent.CancelClicked)
            viewModel.onEvent(RoutineEditorEvent.DiscardConfirmed)
            assertEquals(RoutineEditorEffect.Finished, awaitItem())
        }
        assertEquals("Morning", routines.get(existing.id)?.name)
    }
}
