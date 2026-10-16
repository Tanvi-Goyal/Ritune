package com.ritune.feature.today.ui.navigation

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime

sealed interface TodayUiState {
    data object Loading : TodayUiState

    data class Content(
        val day: LocalDate,
        val anchors: List<AnchorItem>,
        val runningRoutineName: String?,
    ) : TodayUiState {

        val loggedCount: Int get() = anchors.count { it.logged }
        val totalCount: Int get() = anchors.size
    }
}

data class AnchorItem(
    val id: String,
    val name: String,
    val targetTime: LocalTime?,
    val loggedAt: LocalTime?,
    val routine: RoutineRef?,
) {
    val logged: Boolean get() = loggedAt != null
}

data class RoutineRef(val id: String, val name: String)

sealed interface TodayEvent {
    data class LogAnchor(val anchorId: String) : TodayEvent
    data class StartRoutine(val routineId: String) : TodayEvent
    data class UndoLog(val eventId: String) : TodayEvent
    data object OpenRunningRoutine : TodayEvent
}

sealed interface TodayEffect {
    data class Logged(
        val anchorName: String,
        val loggedAt: LocalTime,
        val eventId: String
    ) : TodayEffect

    data object OpenPlayer : TodayEffect
    data object RoutineUnavailable : TodayEffect
}