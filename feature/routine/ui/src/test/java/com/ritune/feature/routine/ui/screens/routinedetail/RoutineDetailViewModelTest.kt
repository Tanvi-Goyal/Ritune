package com.ritune.feature.routine.ui.screens.routinedetail

import androidx.lifecycle.SavedStateHandle
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.turbine.test
import com.ritune.core.domain.usecases.anchor.ObserveAnchors
import com.ritune.core.domain.usecases.player.StartRoutine
import com.ritune.core.domain.usecases.routine.DeleteRoutine
import com.ritune.core.domain.usecases.routine.ObserveRoutine
import com.ritune.core.models.DefaultCatalog
import com.ritune.core.models.RoutineId
import com.ritune.core.testing.data.aRoutine
import com.ritune.core.testing.data.aStep
import com.ritune.core.testing.ports.FakePlayerController
import com.ritune.core.testing.repository.FakeAnchorRepository
import com.ritune.core.testing.repository.FakeRoutineRepository
import com.ritune.core.testing.rules.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.update
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
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

// SavedStateHandle.toRoute() builds a Bundle, so these run on Robolectric.
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class RoutineDetailViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val custom = aRoutine(
        id = "custom",
        name = "Morning",
        anchorId = DefaultCatalog.wake.id,
        steps = listOf(aStep(id = "a", title = "Stretch", duration = 1.minutes), aStep(id = "b", title = "Breathe", duration = 30.seconds)),
    )
    private val builtIn = aRoutine(id = "built-in", builtIn = true)
    private val routines = FakeRoutineRepository(listOf(custom, builtIn))
    private val player = FakePlayerController()

    private fun TestScope.newViewModel(id: String = "custom"): RoutineDetailViewModel {
        val viewModel = RoutineDetailViewModel(
            SavedStateHandle(mapOf("routineId" to id)),
            ObserveRoutine(routines),
            ObserveAnchors(FakeAnchorRepository()),
            StartRoutine(routines, player),
            DeleteRoutine(routines),
        )
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.state.collect() }
        return viewModel
    }

    private fun RoutineDetailViewModel.content() = assertIs<RoutineDetailUiState.Content>(state.value)

    @Test
    fun state_showsRoutineWithStepsAndAnchor() = runTest {
        assertEquals(
            RoutineDetailUiState.Content(
                id = "custom",
                name = "Morning",
                steps = listOf(StepItem("Stretch", 1.minutes), StepItem("Breathe", 30.seconds)),
                totalDuration = 90.seconds,
                anchorName = "Wake",
                builtIn = false,
                confirmingDelete = false,
            ),
            newViewModel().state.value,
        )
    }

    @Test
    fun state_unknownRoutine_isNotFound() = runTest {
        assertEquals(RoutineDetailUiState.NotFound, newViewModel("missing").state.value)
    }

    @Test
    fun state_routineDeletedElsewhere_becomesNotFound() = runTest {
        val viewModel = newViewModel()

        routines.routines.update { listOf(builtIn) }

        assertEquals(RoutineDetailUiState.NotFound, viewModel.state.value)
    }

    @Test
    fun start_opensPlayer() = runTest {
        val viewModel = newViewModel()

        viewModel.effects.test {
            viewModel.onEvent(RoutineDetailEvent.Start)

            assertEquals(RoutineDetailEffect.OpenPlayer, awaitItem())
            assertEquals(listOf(custom), player.startedRoutines)
        }
    }

    @Test
    fun start_routineGone_saysUnavailable() = runTest {
        val viewModel = newViewModel("missing")

        viewModel.effects.test {
            viewModel.onEvent(RoutineDetailEvent.Start)

            assertEquals(RoutineDetailEffect.RoutineUnavailable, awaitItem())
        }
    }

    @Test
    fun requestAndDismissDelete_togglesConfirmation() = runTest {
        val viewModel = newViewModel()

        viewModel.onEvent(RoutineDetailEvent.RequestDelete)
        assertTrue(viewModel.content().confirmingDelete)

        viewModel.onEvent(RoutineDetailEvent.DismissDelete)
        assertFalse(viewModel.content().confirmingDelete)
    }

    @Test
    fun confirmDelete_customRoutine_deletesAndCloses() = runTest {
        val viewModel = newViewModel()
        viewModel.onEvent(RoutineDetailEvent.RequestDelete)

        viewModel.effects.test {
            viewModel.onEvent(RoutineDetailEvent.ConfirmDelete)

            assertEquals(RoutineDetailEffect.Deleted, awaitItem())
        }
        assertEquals(null, routines.get(RoutineId("custom")))
    }

    @Test
    fun confirmDelete_builtInRoutine_keepsItAndStays() = runTest {
        val viewModel = newViewModel("built-in")
        viewModel.onEvent(RoutineDetailEvent.RequestDelete)

        viewModel.effects.test {
            viewModel.onEvent(RoutineDetailEvent.ConfirmDelete)

            expectNoEvents()
        }
        assertFalse(viewModel.content().confirmingDelete)
        assertEquals(builtIn, routines.get(builtIn.id))
    }
}
