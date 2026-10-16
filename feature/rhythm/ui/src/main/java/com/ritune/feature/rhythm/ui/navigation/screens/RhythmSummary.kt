package com.ritune.feature.rhythm.ui.navigation.screens

sealed interface RhythmSummary {
    data class Early(val loggedDays: Int, val requiredDays: Int) : RhythmSummary

    data object Building : RhythmSummary

    data class Scored(
        val consistencyPercent: Int,
        val wakeDriftMinutes: Int?,
        val steadiestName: String?,
    ) : RhythmSummary
}
