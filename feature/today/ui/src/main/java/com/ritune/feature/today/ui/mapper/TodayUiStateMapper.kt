package com.ritune.feature.today.ui.mapper

import com.ritune.core.domain.models.anchor.AnchorStatus
import com.ritune.core.models.AnchorId
import com.ritune.core.models.Routine
import com.ritune.core.models.player.PlayerState
import com.ritune.feature.today.ui.screens.AnchorItem
import com.ritune.feature.today.ui.screens.RoutineRef
import com.ritune.feature.today.ui.screens.TodayUiState
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

internal fun buildContent(
    day: LocalDate,
    statuses: List<AnchorStatus>,
    routines: List<Routine>,
    playerState: PlayerState,
    zone: TimeZone,
): TodayUiState.Content {
    val routineByAnchor = routinesByAnchor(routines)

    val anchors = statuses.map { status ->
        AnchorItem(
            id = status.anchor.id.value,
            name = status.anchor.name,
            targetTime = status.anchor.targetTime,
            loggedAt = status.loggedAt?.toLocalTime(zone),
            routine = routineByAnchor[status.anchor.id]
                ?.let {
                    RoutineRef(
                        it.id.value,
                        it.name
                    )
                },
        )
    }

    val runningRoutine = when (playerState) {
        is PlayerState.Running -> playerState.session.routine.name
        is PlayerState.Paused -> playerState.session.routine.name
        PlayerState.Idle, is PlayerState.Finished -> null
    }

    return TodayUiState.Content(
        day = day,
        anchors = anchors,
        runningRoutineName = runningRoutine
    )
}

internal fun routinesByAnchor(routines: List<Routine>): Map<AnchorId, Routine> = buildMap {
    routines.forEach { routine ->
        routine.anchorId?.let { anchorId ->
            put(anchorId, routine)
        }
    }
}

internal fun Instant.toLocalTime(zone: TimeZone) = toLocalDateTime(zone).time
