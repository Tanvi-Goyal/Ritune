package com.ritune.feature.player.ui.screens.summary

import androidx.lifecycle.SavedStateHandle
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.turbine.test
import com.ritune.core.domain.usecases.history.ObserveRunSummary
import com.ritune.core.domain.usecases.player.DismissRun
import com.ritune.core.domain.usecases.routine.ObserveRoutine
import com.ritune.core.models.RunOutcome
import com.ritune.core.testing.data.aRoutine
import com.ritune.core.testing.data.aRunSummary
import com.ritune.core.testing.data.aStep
import com.ritune.core.testing.data.testInstant
import com.ritune.core.testing.ports.FakePlayerController
import com.ritune.core.testing.ports.FakePlayerController.Command
import com.ritune.core.testing.repository.FakeRoutineRepository
import com.ritune.core.testing.repository.FakeRunHistoryRepository
import com.ritune.core.testing.rules.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes

// SavedStateHandle.toRoute() builds a Bundle, so these run on Robolectric.
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class SummaryViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val routine = aRoutine(
        name = "Morning",
        steps = listOf(aStep(id = "a", duration = 1.minutes), aStep(id = "b", duration = 2.minutes)),
    )
    private val routines = FakeRoutineRepository(listOf(routine))
    private val history = FakeRunHistoryRepository()
    private val player = FakePlayerController()

    private fun TestScope.newViewModel(runId: String = "run-1"): SummaryViewModel {
        val viewModel = SummaryViewModel(
            SavedStateHandle(mapOf("runId" to runId)),
            ObserveRunSummary(history),
            ObserveRoutine(routines),
            DismissRun(player),
        )
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.state.collect() }
        return viewModel
    }

    @Test
    fun completedRun_showsNameStepsAndDuration() = runTest {
        history.summaries.value = mapOf(aRunSummary(routine).let { it.runId to it })

        assertEquals(
            SummaryUiState.Content(
                routineName = "Morning",
                completed = true,
                completedSteps = 2,
                skippedSteps = 0,
                totalSteps = 2,
                duration = 3.minutes,
                startedAt = testInstant,
            ),
            newViewModel().state.value,
        )
    }

    @Test
    fun stoppedRunOfDeletedRoutine_isNotCompletedAndHasNoName() = runTest {
        history.save(aRunSummary(routine, outcome = RunOutcome.STOPPED, completedSteps = 1))
        routines.routines.value = emptyList()

        val content = assertIs<SummaryUiState.Content>(newViewModel().state.value)

        assertEquals(false, content.completed)
        assertNull(content.routineName)
    }

    @Test
    fun unknownRun_isNotFoundAfterGracePeriod() = runTest {
        val viewModel = newViewModel("missing")

        advanceTimeBy(1_499.milliseconds)
        runCurrent()
        assertEquals(SummaryUiState.Loading, viewModel.state.value)

        advanceTimeBy(1.milliseconds)
        runCurrent()
        assertEquals(SummaryUiState.NotFound, viewModel.state.value)
    }

    @Test
    fun runSavedDuringGracePeriod_showsContent() = runTest {
        val viewModel = newViewModel()

        advanceTimeBy(1_000.milliseconds)
        history.save(aRunSummary(routine))
        advanceTimeBy(1_000.milliseconds)
        runCurrent()

        assertIs<SummaryUiState.Content>(viewModel.state.value)
    }

    @Test
    fun done_resetsPlayerAndCloses() = runTest {
        val viewModel = newViewModel()

        viewModel.effects.test {
            viewModel.onEvent(SummaryUiEvent.Done)

            assertEquals(SummaryUiEffect.Close, awaitItem())
        }
        assertEquals(listOf(Command.RESET), player.commands)
    }
}
