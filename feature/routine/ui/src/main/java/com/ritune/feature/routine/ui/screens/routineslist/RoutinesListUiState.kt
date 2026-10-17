package com.ritune.feature.routine.ui.screens.routineslist

import com.ritune.core.models.AnchorId
import com.ritune.core.models.Routine
import kotlin.time.Duration

sealed interface RoutinesUiState {
    data object Loading : RoutinesUiState
    data class Content(val routines: List<RoutineItem>) : RoutinesUiState
}

data class RoutineItem(
    val id: String,
    val name: String,
    val totalDuration: Duration,
    val stepCount: Int,
    val anchorName: String?,
)

sealed interface RoutinesEvent {
    data class Start(val routineId: String) : RoutinesEvent
}

sealed interface RoutinesEffect {
    data object OpenPlayer : RoutinesEffect
    data object RoutineUnavailable : RoutinesEffect
}

internal fun Routine.toItem(anchorNames: Map<AnchorId, String>) = RoutineItem(
    id = id.value,
    name = name,
    totalDuration = totalDuration,
    stepCount = steps.size,
    anchorName = anchorId?.let(anchorNames::get),
)