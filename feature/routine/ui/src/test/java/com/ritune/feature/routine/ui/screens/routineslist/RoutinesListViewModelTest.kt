package com.ritune.feature.routine.ui.screens.routineslist

import app.cash.turbine.test
import com.ritune.core.domain.ports.StartResult
import com.ritune.core.domain.usecases.anchor.ObserveAnchors
import com.ritune.core.domain.usecases.player.StartRoutine
import com.ritune.core.domain.usecases.routine.ObserveRoutines
import com.ritune.core.models.DefaultCatalog
import com.ritune.core.testing.data.aRoutine
import com.ritune.core.testing.data.aStep
import com.ritune.core.testing.ports.FakePlayerController
import com.ritune.core.testing.repository.FakeAnchorRepository
import com.ritune.core.testing.repository.FakeRoutineRepository
import com.ritune.core.testing.rules.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalCoroutinesApi::class)
class RoutinesListViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val anchored = aRoutine(
        id = "anchored",
        name = "Morning",
        anchorId = DefaultCatalog.wake.id,
        steps = listOf(aStep(id = "a", duration = 1.minutes), aStep(id = "b", duration = 30.seconds)),
    )
    private val loose = aRoutine(id = "loose", name = "Focus")
    private val routines = FakeRoutineRepository(listOf(anchored, loose))
    private val player = FakePlayerController()

    private fun newViewModel() = RoutinesListViewModel(
        ObserveRoutines(routines),
        ObserveAnchors(FakeAnchorRepository()),
        StartRoutine(routines, player),
    )

    @Test
    fun state_isLoadingUntilCollected() {
        assertEquals(RoutinesUiState.Loading, newViewModel().state.value)
    }

    @Test
    fun state_listsRoutinesWithAnchorNames() = runTest {
        val viewModel = newViewModel()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.state.collect() }

        val content = assertIs<RoutinesUiState.Content>(viewModel.state.value)
        assertEquals(
            listOf(
                RoutineItem("anchored", "Morning", 90.seconds, stepCount = 2, anchorName = "Wake"),
                RoutineItem("loose", "Focus", 1.minutes, stepCount = 1, anchorName = null),
            ),
            content.routines,
        )
    }

    @Test
    fun start_knownRoutine_startsItAndOpensPlayer() = runTest {
        val viewModel = newViewModel()
        viewModel.effects.test {
            viewModel.onEvent(RoutinesEvent.Start("loose"))

            assertEquals(RoutinesEffect.OpenPlayer, awaitItem())
            assertEquals(listOf(loose), player.startedRoutines)
        }
    }

    @Test
    fun start_whileAnotherRuns_opensPlayer() = runTest {
        player.startResult = StartResult.AlreadyRunning

        val viewModel = newViewModel()
        viewModel.effects.test {
            viewModel.onEvent(RoutinesEvent.Start("loose"))

            assertEquals(RoutinesEffect.OpenPlayer, awaitItem())
        }
    }

    @Test
    fun start_unknownRoutine_saysUnavailable() = runTest {
        val viewModel = newViewModel()
        viewModel.effects.test {
            viewModel.onEvent(RoutinesEvent.Start("missing"))

            assertEquals(RoutinesEffect.RoutineUnavailable, awaitItem())
        }
    }
}
