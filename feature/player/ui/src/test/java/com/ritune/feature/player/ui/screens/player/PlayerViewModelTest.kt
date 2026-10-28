package com.ritune.feature.player.ui.screens.player

import app.cash.turbine.test
import com.ritune.core.domain.usecases.player.ObservePlayerState
import com.ritune.core.domain.usecases.player.ObserveRunProgress
import com.ritune.core.domain.usecases.player.PauseRun
import com.ritune.core.domain.usecases.player.ResumeRun
import com.ritune.core.domain.usecases.player.SkipStep
import com.ritune.core.domain.usecases.player.StopRun
import com.ritune.core.models.player.PlayerState
import com.ritune.core.testing.data.aRoutine
import com.ritune.core.testing.data.aRunSummary
import com.ritune.core.testing.data.aSession
import com.ritune.core.testing.data.aStep
import com.ritune.core.testing.ports.FakePlayerController
import com.ritune.core.testing.ports.FakePlayerController.Command
import com.ritune.core.testing.rules.MainDispatcherRule
import com.ritune.core.testing.time.VirtualTimekeeper
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalCoroutinesApi::class)
class PlayerViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val routine = aRoutine(
        name = "Morning",
        steps = listOf(
            aStep(id = "a", title = "Stretch", duration = 1.minutes, instruction = "Slowly"),
            aStep(id = "b", title = "Breathe", duration = 2.minutes),
        ),
    )
    private val player = FakePlayerController()

    private fun TestScope.newViewModel(): PlayerViewModel {
        val viewModel = PlayerViewModel(
            ObserveRunProgress(player, VirtualTimekeeper(testScheduler)),
            ObservePlayerState(player),
            PauseRun(player),
            ResumeRun(player),
            SkipStep(player),
            StopRun(player),
        )
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.state.collect() }
        return viewModel
    }

    private fun PlayerViewModel.active() = assertIs<PlayerUiState.Active>(state.value)

    private fun running(stepIndex: Int = 0) =
        PlayerState.Running(aSession(routine, stepIndex = stepIndex), stepEndsAtMono = 45_000)

    @Test
    fun nothingRunning_closesPlayer() = runTest {
        val viewModel = newViewModel()

        viewModel.effects.test {
            assertEquals(PlayerUiEffect.Close, awaitItem())
        }
        assertEquals(PlayerUiState.Loading, viewModel.state.value)
    }

    @Test
    fun running_showsStepCountdownAndNextStep() = runTest {
        player.state.value = running()

        assertEquals(
            PlayerUiState.Active(
                routineName = "Morning",
                stepIndex = 0,
                stepCount = 2,
                stepTitle = "Stretch",
                instruction = "Slowly",
                nextStepTitle = "Breathe",
                remaining = 45.seconds,
                isPaused = false,
                confirmingStop = false,
            ),
            newViewModel().state.value,
        )
    }

    @Test
    fun lastStep_hasNoNextStep() = runTest {
        player.state.value = running(stepIndex = 1)

        assertTrue(newViewModel().active().isLastStep)
    }

    @Test
    fun paused_showsRemainingTime() = runTest {
        player.state.value = PlayerState.Paused(aSession(routine), remaining = 20.seconds)

        val active = newViewModel().active()

        assertTrue(active.isPaused)
        assertEquals(20.seconds, active.remaining)
    }

    @Test
    fun togglePause_pausesWhenRunningAndResumesWhenPaused() = runTest {
        player.state.value = running()
        val viewModel = newViewModel()

        viewModel.onEvent(PlayerUiEvent.TogglePause)
        player.state.value = PlayerState.Paused(aSession(routine), remaining = 20.seconds)
        viewModel.onEvent(PlayerUiEvent.TogglePause)

        assertEquals(listOf(Command.PAUSE, Command.RESUME), player.commands)
    }

    @Test
    fun skip_skipsStep() = runTest {
        player.state.value = running()
        val viewModel = newViewModel()

        viewModel.onEvent(PlayerUiEvent.Skip)

        assertEquals(listOf(Command.SKIP_STEP), player.commands)
    }

    @Test
    fun stop_asksFirstAndStopsOnConfirm() = runTest {
        player.state.value = running()
        val viewModel = newViewModel()

        viewModel.onEvent(PlayerUiEvent.RequestStop)
        assertTrue(viewModel.active().confirmingStop)
        viewModel.onEvent(PlayerUiEvent.DismissStop)
        assertFalse(viewModel.active().confirmingStop)
        assertEquals(emptyList(), player.commands)

        viewModel.onEvent(PlayerUiEvent.RequestStop)
        viewModel.onEvent(PlayerUiEvent.ConfirmStop)

        assertFalse(viewModel.active().confirmingStop)
        assertEquals(listOf(Command.STOP), player.commands)
    }

    @Test
    fun runFinished_opensSummaryForThatRun() = runTest {
        player.state.value = running()
        val viewModel = newViewModel()

        viewModel.effects.test {
            player.state.value = PlayerState.Finished(aRunSummary(routine, runId = "run-9"))

            assertEquals(PlayerUiEffect.RunFinished("run-9"), awaitItem())
        }
    }
}
