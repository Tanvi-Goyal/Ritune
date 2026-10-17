package com.ritune.feature.player.ui.screens.summary

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.ritune.core.domain.usecases.history.ObserveRunSummary
import com.ritune.core.domain.usecases.player.DismissRun
import com.ritune.core.models.RunId
import com.ritune.core.models.RunOutcome
import com.ritune.feature.player.ui.navigation.SummaryRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.transformLatest
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class SummaryViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    observeRunSummary: ObserveRunSummary,
    private val dismissRun: DismissRun,
) : ViewModel() {

    private val runId = RunId(savedStateHandle.toRoute<SummaryRoute>().runId)

    val state: StateFlow<SummaryUiState> = observeRunSummary(runId)
        .transformLatest { summary ->
            if (summary == null) {
                delay(NOT_FOUND_AFTER_MS.milliseconds)
                emit(SummaryUiState.NotFound)
            } else {
                emit(
                    SummaryUiState.Content(
                        routineName = summary.routineId.value,
                        completed = summary.outcome == RunOutcome.COMPLETED,
                        completedSteps = summary.completedSteps,
                        skippedSteps = summary.skippedSteps,
                        totalSteps = summary.totalSteps,
                        duration = summary.finishedAt - summary.startedAt,
                        startedAt = summary.startedAt,
                    ),
                )
            }
        }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            SummaryUiState.Loading
        )

    private val _effects = Channel<SummaryUiEffect>(Channel.BUFFERED)
    val effects: Flow<SummaryUiEffect> = _effects.receiveAsFlow()

    fun onEvent(event: SummaryUiEvent) {
        when (event) {
            SummaryUiEvent.Done -> viewModelScope.launch {
                dismissRun()
                _effects.trySend(SummaryUiEffect.Close)
            }
        }
    }

    private companion object {
        const val NOT_FOUND_AFTER_MS = 1_500L
    }
}
