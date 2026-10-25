package com.ritune.core.domain.usecases.rhythm

import com.ritune.core.models.Timekeeper
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import javax.inject.Inject
import kotlin.time.Duration.Companion.seconds

/**
 * Today's date, emitted now and again just after each midnight, so screens roll over by themselves.
 * A device clock or time zone change is not detected; the next midnight tick corrects it.
 */
class ObserveCurrentDay @Inject constructor(
    private val timekeeper: Timekeeper,
) {
    operator fun invoke(): Flow<LocalDate> = flow {
        while (true) {
            val now = timekeeper.now().wall
            val zone = timekeeper.timeZone()
            val today = now.toLocalDateTime(zone).date
            emit(today)
            val nextMidnight = today.plus(1, DateTimeUnit.DAY).atStartOfDayIn(zone)
            delay((nextMidnight - now).coerceAtLeast(1.seconds))
        }
    }.distinctUntilChanged()
}
