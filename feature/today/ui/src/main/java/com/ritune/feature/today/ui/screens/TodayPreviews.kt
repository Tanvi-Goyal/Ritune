package com.ritune.feature.today.ui.screens

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.ritune.core.designsystem.theme.RituneTheme
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime

private val evening = RoutineRef("evening-reset", "Evening reset")

private fun item(
    id: String,
    name: String,
    target: LocalTime,
    logged: LocalTime? = null,
    routine: RoutineRef? = null
) =
    AnchorItem(id, name, target, logged, routine)

private fun content(logged: Int, running: String? = null): TodayUiState.Content {
    val times = listOf(
        LocalTime(6, 42),
        LocalTime(7, 5),
        LocalTime(12, 30),
        LocalTime(19, 28),
        LocalTime(21, 46)
    )
    val base = listOf(
        item("wake", "Wake", LocalTime(6, 45)),
        item("daylight", "Daylight", LocalTime(7, 15)),
        item("workout", "Workout", LocalTime(12, 30)),
        item("last-meal", "Last meal", LocalTime(19, 30)),
        item("shutdown", "Shutdown", LocalTime(22, 0), routine = evening),
    )
    val anchors = base.mapIndexed { i, a ->
        if (i < logged) a.copy(loggedAt = times[i]) else a
    }
    return TodayUiState.Content(
        LocalDate(2026, 10, 9),
        anchors,
        running
    )
}

@Composable
private fun Preview(state: TodayUiState) {
    RituneTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            TodayContent(
                state,
                remember { SnackbarHostState() },
                onEvent = {},
                onOpenSettings = {}
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun TodayPartlyDonePreview() = Preview(content(logged = 3))

@PreviewLightDark
@Composable
private fun TodayFreshDayPreview() = Preview(content(logged = 0))

@PreviewLightDark
@Composable
private fun TodayAllLoggedPreview() = Preview(content(logged = 5))

@PreviewLightDark
@Composable
private fun TodayRoutineRunningPreview() =
    Preview(content(logged = 4, running = "Evening reset"))

@PreviewLightDark
@Composable
private fun TodayLoadingPreview() = Preview(TodayUiState.Loading)