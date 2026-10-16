package com.ritune.feature.routine.ui.navigation.screens.routinedetail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.ritune.core.domain.models.routine.DeleteResult
import com.ritune.core.domain.usecases.anchor.ObserveAnchors
import com.ritune.core.domain.usecases.routine.DeleteRoutine
import com.ritune.core.domain.usecases.routine.ObserveRoutine
import com.ritune.core.models.Anchor
import com.ritune.core.models.Routine
import com.ritune.core.models.RoutineId
import com.ritune.feature.routine.ui.navigation.RoutineDetailRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RoutineDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    observeRoutine: ObserveRoutine,
    observeAnchors: ObserveAnchors,
//    private val startRoutine: StartRoutine,
    private val deleteRoutine: DeleteRoutine,
) : ViewModel() {

    private val routineId = RoutineId(savedStateHandle.toRoute<RoutineDetailRoute>().routineId)
    private val confirmingDelete = MutableStateFlow(false)

    val state: StateFlow<RoutineDetailUiState> =
        combine(
            observeRoutine(routineId),
            observeAnchors(),
            confirmingDelete
        ) { routine, anchors, confirming ->
            buildState(routine, anchors, confirming)
        }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            RoutineDetailUiState.Loading
        )

    private val _effects = Channel<RoutineDetailEffect>(Channel.BUFFERED)
    val effects: Flow<RoutineDetailEffect> = _effects.receiveAsFlow()

    fun onEvent(event: RoutineDetailEvent) {
        when (event) {
            RoutineDetailEvent.Start -> viewModelScope.launch {
//                val effect = when (startRoutine(routineId)) {
//                    StartResult.Started, StartResult.AlreadyRunning ->
//                        RoutineDetailEffect.OpenPlayer
//
//                    StartResult.RoutineNotFound ->
//                        RoutineDetailEffect.RoutineUnavailable
//                }
//                _effects.trySend(effect)
            }

            RoutineDetailEvent.RequestDelete -> confirmingDelete.value = true
            RoutineDetailEvent.DismissDelete -> confirmingDelete.value = false
            RoutineDetailEvent.ConfirmDelete -> viewModelScope.launch {
                confirmingDelete.value = false
                when (deleteRoutine(routineId)) {
                    DeleteResult.Deleted, DeleteResult.NotFound ->
                        _effects.trySend(RoutineDetailEffect.Deleted)

                    DeleteResult.BuiltIn -> Unit
                }
            }
        }
    }

    private fun buildState(
        routine: Routine?,
        anchors: List<Anchor>,
        confirming: Boolean
    ): RoutineDetailUiState {
        if (routine == null) return RoutineDetailUiState.NotFound
        return RoutineDetailUiState.Content(
            id = routine.id.id,
            name = routine.name,
            steps = routine.steps.map { StepItem(it.title, it.duration) },
            totalDuration = routine.totalDuration,
            anchorName = routine.anchorId?.let { id ->
                anchors.firstOrNull { it.id == id }?.name
            },
            builtIn = routine.builtIn,
            confirmingDelete = confirming,
        )
    }
}