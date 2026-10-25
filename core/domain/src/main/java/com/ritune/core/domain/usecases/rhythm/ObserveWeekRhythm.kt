package com.ritune.core.domain.usecases.rhythm

import com.ritune.core.domain.models.rhythm.WeekRhythm
import com.ritune.core.domain.repositories.AnchorEventRepository
import com.ritune.core.domain.repositories.AnchorRepository
import com.ritune.core.models.Timekeeper
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import javax.inject.Inject

class ObserveWeekRhythm @Inject constructor(
    private val anchorRepository: AnchorRepository,
    private val anchorEventRepository: AnchorEventRepository,
    private val timekeeper: Timekeeper,
) {
    operator fun invoke(weekEnd: LocalDate): Flow<WeekRhythm> {
        val tz = timekeeper.timeZone()
        val from = weekEnd.minus(20, DateTimeUnit.DAY).atStartOfDayIn(tz)
        val to = weekEnd.plus(1, DateTimeUnit.DAY).atStartOfDayIn(tz)
        return combine(
            anchorRepository.observeAll(),
            anchorEventRepository.observeBetween(from, to)
        ) { all, logged ->
            ConsistencyCalculator.compute(all, logged, weekEnd)
        }
    }
}