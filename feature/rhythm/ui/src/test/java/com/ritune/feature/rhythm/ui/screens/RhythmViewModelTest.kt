package com.ritune.feature.rhythm.ui.screens

import com.ritune.core.domain.usecases.rhythm.ObserveCurrentDay
import com.ritune.core.domain.usecases.rhythm.ObserveWeekRhythm
import com.ritune.core.models.DefaultCatalog
import com.ritune.core.testing.data.anAnchorEvent
import com.ritune.core.testing.repository.FakeAnchorEventRepository
import com.ritune.core.testing.repository.FakeAnchorRepository
import com.ritune.core.testing.rules.MainDispatcherRule
import com.ritune.core.testing.time.VirtualTimekeeper
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class RhythmViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val events = FakeAnchorEventRepository()

    // 2026-01-05 20:00 UTC, four hours before midnight.
    private val evening = Instant.parse("2026-01-05T20:00:00Z")

    private fun TestScope.newViewModel(): RhythmViewModel {
        val clock = VirtualTimekeeper(testScheduler, startWall = evening)
        val viewModel = RhythmViewModel(
            ObserveCurrentDay(clock),
            ObserveWeekRhythm(FakeAnchorRepository(), events, clock),
        )
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.state.collect() }
        return viewModel
    }

    private fun RhythmViewModel.content() = assertIs<RhythmUiState.Content>(state.value)

    @Test
    fun state_isLoadingUntilCollected() = runTest {
        val clock = VirtualTimekeeper(testScheduler)
        val viewModel = RhythmViewModel(
            ObserveCurrentDay(clock),
            ObserveWeekRhythm(FakeAnchorRepository(), events, clock),
        )

        assertEquals(RhythmUiState.Loading, viewModel.state.value)
    }

    @Test
    fun state_noLogs_isEarlyForTheWeekEndingToday() = runTest {
        val content = newViewModel().content()

        assertEquals(LocalDate(2026, 1, 5), content.weekEnd)
        assertEquals(RhythmSummary.Early(loggedDays = 0, requiredDays = 3), content.summary)
    }

    @Test
    fun state_steadyLogsOnThreeDays_isScored() = runTest {
        events.events.value = (0..2).map { day ->
            anAnchorEvent(id = "e$day", occurredAt = evening - (day + 1).days - 13.hours)
        }

        val summary = assertIs<RhythmSummary.Scored>(newViewModel().content().summary)

        assertEquals(100, summary.consistencyPercent)
        assertEquals(0, summary.wakeDriftMinutes)
        assertEquals(DefaultCatalog.wake.name, summary.steadiestName)
    }

    @Test
    fun state_newLog_updatesProgress() = runTest {
        val viewModel = newViewModel()

        events.events.update { it + anAnchorEvent(occurredAt = evening) }

        assertEquals(RhythmSummary.Early(loggedDays = 1, requiredDays = 3), viewModel.content().summary)
    }

    @Test
    fun state_afterMidnight_movesWeekToNewDay() = runTest {
        val viewModel = newViewModel()

        advanceTimeBy(4.hours)
        runCurrent()
        events.events.update { it + anAnchorEvent(occurredAt = evening + 5.hours) }

        val content = viewModel.content()
        assertEquals(LocalDate(2026, 1, 6), content.weekEnd)
        assertEquals(RhythmSummary.Early(loggedDays = 1, requiredDays = 3), content.summary)
    }
}
