package com.ritune.feature.today.ui.navigation.summary

import kotlin.time.Duration
import kotlin.time.Instant

sealed interface SummaryUiState {
    data object Loading : SummaryUiState

    data object NotFound : SummaryUiState

    data class Content(
        val routineName: String,
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
