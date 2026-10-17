package com.ritune.feature.routine.ui.navigation.screens.routineslist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ritune.core.domain.ports.StartResult
import com.ritune.core.domain.usecases.anchor.ObserveAnchors
import com.ritune.core.domain.usecases.player.StartRoutine
import com.ritune.core.domain.usecases.routine.ObserveRoutines
import com.ritune.core.models.Anchor
import com.ritune.core.models.Routine
import com.ritune.core.models.RoutineId
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RoutinesListViewModel @Inject constructor(
    observeRoutines: ObserveRoutines,
    observeAnchors: ObserveAnchors,
    private val startRoutine: StartRoutine,
) : ViewModel() {

    val state: StateFlow<RoutinesUiState> =
        combine(
            observeRoutines(),
            observeAnchors()
        ) { routines, anchors ->
            buildState(routines, anchors)
        }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            RoutinesUiState.Loading
        )

    private val _effects = Channel<RoutinesEffect>(Channel.BUFFERED)
    val effects: Flow<RoutinesEffect> = _effects.receiveAsFlow()

    fun onEvent(event: RoutinesEvent) {
        when (event) {
            is RoutinesEvent.Start -> viewModelScope.launch {
                val effect = when (startRoutine(RoutineId(event.routineId))) {
                    StartResult.Started, StartResult.AlreadyRunning -> RoutinesEffect.OpenPlayer
                    StartResult.RoutineNotFound -> RoutinesEffect.RoutineUnavailable
                }
                _effects.trySend(effect)
            }
        }
    }

    private fun buildState(routines: List<Routine>, anchors: List<Anchor>): RoutinesUiState {
        val names = anchors.associate { it.id to it.name }
        return RoutinesUiState.Content(routines.map { it.toItem(names) })
    }
}