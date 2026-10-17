package com.ritune.feature.rhythm.ui.screens

import kotlinx.datetime.LocalDate

sealed interface RhythmUiState {
    data object Loading : RhythmUiState

    data class Content(
        val weekEnd: LocalDate,
        val days: List<DayBar>,
        val summary: RhythmSummary,
    ) : RhythmUiState
}

sealed interface RhythmSummary {
    data class Early(val loggedDays: Int, val requiredDays: Int) : RhythmSummary

    data object Building : RhythmSummary

    data class Scored(
        val consistencyPercent: Int,
        val wakeDriftMinutes: Int?,
        val steadiestName: String?,
    ) : RhythmSummary
}

data class DayBar(val day: LocalDate, val fraction: Float?)