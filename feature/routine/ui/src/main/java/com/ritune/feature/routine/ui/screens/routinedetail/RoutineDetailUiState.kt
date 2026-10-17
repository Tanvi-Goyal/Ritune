package com.ritune.feature.routine.ui.screens.routinedetail

import kotlin.time.Duration

sealed interface RoutineDetailUiState {
    data object Loading : RoutineDetailUiState

    data object NotFound : RoutineDetailUiState

    data class Content(
        val id: String,
        val name: String,
        val steps: List<StepItem>,
        val totalDuration: Duration,
        val anchorName: String?,
        val builtIn: Boolean,
        val confirmingDelete: Boolean,
    ) : RoutineDetailUiState
}

data class StepItem(val title: String, val duration: Duration)

sealed interface RoutineDetailEvent {
    data object Start : RoutineDetailEvent
    data object RequestDelete : RoutineDetailEvent
    data object DismissDelete : RoutineDetailEvent
    data object ConfirmDelete : RoutineDetailEvent
}

sealed interface RoutineDetailEffect {
    data object OpenPlayer : RoutineDetailEffect
    data object RoutineUnavailable : RoutineDetailEffect
    data object Deleted : RoutineDetailEffect
}