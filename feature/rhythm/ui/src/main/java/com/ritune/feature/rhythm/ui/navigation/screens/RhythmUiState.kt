package com.ritune.feature.rhythm.ui.navigation.screens

import kotlinx.datetime.LocalDate

sealed interface RhythmUiState {
    data object Loading : RhythmUiState

    data class Content(
        val weekEnd: LocalDate,
        val days: List<DayBar>,
        val summary: RhythmSummary,
    ) : RhythmUiState
}

data class DayBar(val day: LocalDate, val fraction: Float?)