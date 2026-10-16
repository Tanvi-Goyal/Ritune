package com.ritune.feature.today.ui.navigation.player

import kotlin.time.Duration

sealed interface PlayerUiState {
    data object Loading : PlayerUiState

    data class Active(
        val routineName: String,
        val stepIndex: Int,
        val stepCount: Int,
        val stepTitle: String,
        val instruction: String?,
        val nextStepTitle: String?,
        val remaining: Duration,
        val isPaused: Boolean,
        val confirmingStop: Boolean,
    ) : PlayerUiState {
        val isLastStep: Boolean get() = nextStepTitle == null
    }
}

sealed interface PlayerUiEvent {
    data object TogglePause : PlayerUiEvent
    data object Skip : PlayerUiEvent
    data object RequestStop : PlayerUiEvent
    data object DismissStop : PlayerUiEvent
    data object ConfirmStop : PlayerUiEvent
}

sealed interface PlayerUiEffect {
    data object Close : PlayerUiEffect
    data class RunFinished(val runId: String) : PlayerUiEffect
}