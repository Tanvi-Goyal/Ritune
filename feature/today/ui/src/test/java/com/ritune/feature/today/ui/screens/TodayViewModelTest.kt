package com.ritune.feature.today.ui.screens

import app.cash.turbine.test
import com.ritune.core.domain.ports.StartResult
import com.ritune.core.domain.usecases.anchor.LogAnchor
import com.ritune.core.domain.usecases.anchor.ObserveToday
import com.ritune.core.domain.usecases.anchor.UndoLogAnchor
import com.ritune.core.domain.usecases.player.ObservePlayerState
import com.ritune.core.domain.usecases.player.StartRoutine
import com.ritune.core.domain.usecases.rhythm.ObserveCurrentDay
import com.ritune.core.domain.usecases.routine.ObserveRoutines
import com.ritune.core.models.DefaultCatalog
import com.ritune.core.models.player.PlayerState
import com.ritune.core.testing.data.aSession
import com.ritune.core.testing.id.SequentialIdGenerator
import com.ritune.core.testing.ports.FakePlayerController
import com.ritune.core.testing.repository.FakeAnchorEventRepository
import com.ritune.core.testing.repository.FakeAnchorRepository
import com.ritune.core.testing.repository.FakeRoutineRepository
import com.ritune.core.testing.rules.MainDispatcherRule
import com.ritune.core.testing.time.VirtualTimekeeper
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.hours

@OptIn(ExperimentalCoroutinesApi::class)
class TodayViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val anchors = FakeAnchorRepository()
    private val events = FakeAnchorEventRepository()
    private val routines = FakeRoutineRepository()
    private val player = FakePlayerController()

    private fun TestScope.newViewModel(): TodayViewModel {
        val clock = VirtualTimekeeper(testScheduler)
        return TodayViewModel(
            observeToday = ObserveToday(anchors, events, clock),
            observeCurrentDay = ObserveCurrentDay(clock),
            observePlayerState = ObservePlayerState(player),
            observeRoutines = ObserveRoutines(routines),
            logAnchor = LogAnchor(events, clock, SequentialIdGenerator("event")),
            undoLogAnchor = UndoLogAnchor(events),
            timekeeper = clock,
            startRoutine = StartRoutine(routines, player),
        )
    }

    private fun TestScope.collectState(viewModel: TodayViewModel) {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.state.collect() }
    }

    private fun TodayViewModel.content() = assertIs<TodayUiState.Content>(state.value)

    @Test
    fun state_isLoadingUntilCollected() = runTest {
        assertEquals(TodayUiState.Loading, newViewModel().state.value)
    }

    @Test
    fun state_showsAnchorsWithLinkedRoutines() = runTest {
        val viewModel = newViewModel()

        viewModel.state.test {
            val content = assertIs<TodayUiState.Content>(awaitItem())
            assertEquals(LocalDate(2026, 1, 5), content.day)
            assertEquals(DefaultCatalog.anchors.size, content.totalCount)
            assertEquals(0, content.loggedCount)
            assertEquals(
                RoutineRef(DefaultCatalog.morningStart.id.value, DefaultCatalog.morningStart.name),
                content.anchors.first { it.id == DefaultCatalog.wake.id.value }.routine,
            )
        }
    }

    @Test
    fun logAnchor_marksAnchorLoggedAndEmitsLoggedEffect() = runTest {
        val viewModel = newViewModel()
        collectState(viewModel)

        viewModel.effects.test {
            viewModel.onEvent(TodayEvent.LogAnchor(DefaultCatalog.wake.id.value))

            assertEquals(
                TodayEffect.Logged(anchorName = "Wake", loggedAt = LocalTime(8, 0), eventId = "event-1"),
                awaitItem(),
            )
        }
        val wake = viewModel.content().anchors.first { it.id == DefaultCatalog.wake.id.value }
        assertEquals(LocalTime(8, 0), wake.loggedAt)
    }

    @Test
    fun logAnchor_twiceWithinDedupeWindow_logsOnce() = runTest {
        val viewModel = newViewModel()
        collectState(viewModel)

        viewModel.effects.test {
            viewModel.onEvent(TodayEvent.LogAnchor(DefaultCatalog.wake.id.value))
            assertIs<TodayEffect.Logged>(awaitItem())
            viewModel.onEvent(TodayEvent.LogAnchor(DefaultCatalog.wake.id.value))

            expectNoEvents()
        }
        assertEquals(1, events.events.value.size)
        assertEquals(1, viewModel.content().loggedCount)
    }

    @Test
    fun undoLog_clearsTheLoggedTime() = runTest {
        val viewModel = newViewModel()
        collectState(viewModel)
        viewModel.onEvent(TodayEvent.LogAnchor(DefaultCatalog.wake.id.value))

        viewModel.onEvent(TodayEvent.UndoLog("event-1"))

        assertEquals(0, viewModel.content().loggedCount)
    }

    @Test
    fun startRoutine_unknownRoutine_emitsRoutineUnavailable() = runTest {
        val viewModel = newViewModel()

        viewModel.effects.test {
            viewModel.onEvent(TodayEvent.StartRoutine("missing"))

            assertEquals(TodayEffect.RoutineUnavailable, awaitItem())
        }
        assertTrue(player.startedRoutines.isEmpty())
    }

    @Test
    fun startRoutine_knownRoutine_startsPlayerAndOpensIt() = runTest {
        val viewModel = newViewModel()

        viewModel.effects.test {
            viewModel.onEvent(TodayEvent.StartRoutine(DefaultCatalog.morningStart.id.value))

            assertEquals(TodayEffect.OpenPlayer, awaitItem())
        }
        assertEquals(listOf(DefaultCatalog.morningStart), player.startedRoutines)
    }

    @Test
    fun startRoutine_anotherAlreadyRunning_opensPlayer() = runTest {
        player.startResult = StartResult.AlreadyRunning
        val viewModel = newViewModel()

        viewModel.effects.test {
            viewModel.onEvent(TodayEvent.StartRoutine(DefaultCatalog.morningStart.id.value))

            assertEquals(TodayEffect.OpenPlayer, awaitItem())
        }
    }

    @Test
    fun openRunningRoutine_opensPlayer() = runTest {
        val viewModel = newViewModel()

        viewModel.effects.test {
            viewModel.onEvent(TodayEvent.OpenRunningRoutine)

            assertEquals(TodayEffect.OpenPlayer, awaitItem())
        }
    }

    @Test
    fun runningRoutine_isShownByName() = runTest {
        val viewModel = newViewModel()
        collectState(viewModel)

        player.state.value = PlayerState.Running(aSession(DefaultCatalog.morningStart), stepEndsAtMono = 0)

        assertEquals(DefaultCatalog.morningStart.name, viewModel.content().runningRoutineName)
    }

    @Test
    fun midnight_rollsOverToAnEmptyNewDay() = runTest {
        val viewModel = newViewModel()
        collectState(viewModel)
        viewModel.onEvent(TodayEvent.LogAnchor(DefaultCatalog.wake.id.value))
        assertEquals(1, viewModel.content().loggedCount)

        advanceTimeBy(16.hours)
        runCurrent()

        assertEquals(LocalDate(2026, 1, 6), viewModel.content().day)
        assertEquals(0, viewModel.content().loggedCount)
        assertNull(viewModel.content().runningRoutineName)
    }
}
