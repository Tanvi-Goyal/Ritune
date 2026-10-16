package com.ritune.feature.today.ui.navigation

import com.ritune.core.domain.models.anchor.AnchorStatus
import com.ritune.core.models.AnchorId
import com.ritune.core.models.Routine
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

/** Pure function from domain data to screen state. Easy to unit test, no Android or coroutines. */
internal fun buildContent(
    day: LocalDate,
    statuses: List<AnchorStatus>,
    routines: List<Routine>,
    zone: TimeZone,
): TodayUiState.Content {
    val routineByAnchor = routinesByAnchor(routines)

    val anchors = statuses.map { status ->
        AnchorItem(
            id = status.anchor.id.id,
            name = status.anchor.name,
            targetTime = status.anchor.targetTime,
            loggedAt = status.loggedAt?.toLocalTime(zone),
            routine = routineByAnchor[status.anchor.id]
                ?.let { RoutineRef(it.id.id, it.name) },
        )
    }
    val runningName = null

    return TodayUiState.Content(day = day, anchors = anchors, runningRoutineName = runningName)
}

internal fun routinesByAnchor(routines: List<Routine>): Map<AnchorId, Routine> = buildMap {
    routines.forEach { routine ->
        routine.anchorId?.let { anchorId ->
            put(anchorId, routine)
        }
    }
}

internal fun Instant.toLocalTime(zone: TimeZone) = toLocalDateTime(zone).time
