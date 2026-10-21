package com.ritune.feature.player.ui.screens.summary

import kotlin.time.Duration
import kotlin.time.Instant

sealed interface SummaryUiState {
    data object Loading : SummaryUiState

    data object NotFound : SummaryUiState

    data class Content(
        /** Null when the routine was deleted after the run. */
        val routineName: String?,
        val completed: Boolean,
        val completedSteps: Int,
        val skippedSteps: Int,
        val totalSteps: Int,
        val duration: Duration,
        val startedAt: Instant,
    ) : SummaryUiState
}

sealed interface SummaryUiEvent {
    data object Done : SummaryUiEvent
}

sealed interface SummaryUiEffect {
    data object Close : SummaryUiEffect
}
