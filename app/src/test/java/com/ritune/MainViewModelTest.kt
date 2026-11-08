package com.ritune

import com.ritune.core.domain.usecases.InitializeApp
import com.ritune.core.domain.usecases.anchor.ObserveToday
import com.ritune.core.domain.usecases.player.ObservePlayerState
import com.ritune.core.domain.usecases.rhythm.ObserveCurrentDay
import com.ritune.core.domain.usecases.routine.ObserveRoutines
import com.ritune.core.testing.ports.FakeAppInitializer
import com.ritune.core.testing.ports.FakePlayerController
import com.ritune.core.testing.repository.FakeAnchorEventRepository
import com.ritune.core.testing.repository.FakeAnchorRepository
import com.ritune.core.testing.repository.FakeRoutineRepository
import com.ritune.core.testing.rules.MainDispatcherRule
import com.ritune.core.testing.time.FakeTimekeeper
import com.ritune.feature.today.ui.navigation.TodayRoute
import com.ritune.feature.today.ui.screens.TodayStateSource
import com.ritune.feature.today.ui.screens.TodayUiState
import kotlinx.coroutines.CompletableDeferred
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

class MainViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val initializer = FakeAppInitializer()
    private val clock = FakeTimekeeper()
    private val todayState = TodayStateSource(
        observeToday = ObserveToday(FakeAnchorRepository(), FakeAnchorEventRepository(), clock),
        observeCurrentDay = ObserveCurrentDay(clock),
        observePlayerState = ObservePlayerState(FakePlayerController()),
        observeRoutines = ObserveRoutines(FakeRoutineRepository()),
        timekeeper = clock,
    )

    private fun newViewModel() =
        MainViewModel(
            AppStartup(InitializeApp(initializer), mainDispatcherRule.testDispatcher),
            todayState,
            mainDispatcherRule.testDispatcher,
        )

    @Test
    fun startDestination_staysUnsetUntilInitializationFinishes() {
        initializer.gate = CompletableDeferred()

        val viewModel = newViewModel()

        assertNull(viewModel.startDestination.value)
        initializer.gate!!.complete(Unit)
        assertEquals(TodayRoute, viewModel.startDestination.value)
    }

    @Test
    fun init_initializesTheAppOnce() {
        newViewModel()

        assertEquals(1, initializer.initializeCount)
    }

    @Test
    fun startDestination_comesWithTodaysStatePreloaded() {
        val viewModel = newViewModel()

        assertEquals(TodayRoute, viewModel.startDestination.value)
        assertIs<TodayUiState.Content>(todayState.takePreloaded())
    }
}
