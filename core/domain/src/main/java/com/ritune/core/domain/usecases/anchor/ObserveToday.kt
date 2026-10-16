package com.ritune.core.domain.usecases.anchor

import com.ritune.core.domain.repositories.AnchorEventRepository
import com.ritune.core.domain.repositories.AnchorRepository
import com.ritune.core.domain.models.anchor.AnchorStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.plus
import javax.inject.Inject

class ObserveToday @Inject constructor(
    private val anchorRepository: AnchorRepository,
    private val anchorEventRepository: AnchorEventRepository,
) {
    operator fun invoke(day: LocalDate): Flow<List<AnchorStatus>> {
        val tz = TimeZone.currentSystemDefault()
        val from = day.atStartOfDayIn(tz)
        val to = day.plus(1, DateTimeUnit.DAY).atStartOfDayIn(tz)
        return combine(
            anchorRepository.observeAll(),
            anchorEventRepository.observeBetween(from, to)
        ) { all, logged ->
            all.map { anchor ->
                AnchorStatus(
                    anchor,
                    logged.filter { it.anchorId == anchor.id }
                        .minOfOrNull { it.occurredAt })
            }
        }
    }
}
