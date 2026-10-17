package com.ritune.feature.rhythm.ui.mapper

import com.ritune.core.domain.models.rhythm.WeekRhythm
import com.ritune.core.domain.usecases.rhythm.ConsistencyCalculator
import com.ritune.core.models.DefaultCatalog
import com.ritune.feature.rhythm.ui.screens.DayBar
import com.ritune.feature.rhythm.ui.screens.RhythmSummary
import com.ritune.feature.rhythm.ui.screens.RhythmUiState
import kotlinx.datetime.LocalDate
import kotlin.math.roundToInt

internal fun buildContent(
    weekEnd: LocalDate,
    rhythm: WeekRhythm
): RhythmUiState.Content {
    val consistency =
        rhythm.consistency // read once: a property from another module cannot be smart-cast
    val summary = when {
        !rhythm.hasEnoughData -> RhythmSummary.Early(
            rhythm.loggedDays,
            ConsistencyCalculator.MIN_DAYS
        )

        consistency == null -> RhythmSummary.Building
        else -> RhythmSummary.Scored(
            consistencyPercent = (consistency * 100).roundToInt(),
            wakeDriftMinutes = rhythm.anchors.firstOrNull {
                it.anchor.id == DefaultCatalog.wake.id
            }?.driftMinutes,
            steadiestName = rhythm.steadiest?.name,
        )
    }
    return RhythmUiState.Content(
        weekEnd = weekEnd,
        days = rhythm.days.map { DayBar(it.day, it.fraction) },
        summary = summary,
    )
}
