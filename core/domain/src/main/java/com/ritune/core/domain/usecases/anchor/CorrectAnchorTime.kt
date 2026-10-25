package com.ritune.core.domain.usecases.anchor

import com.ritune.core.domain.repositories.AnchorEventRepository
import com.ritune.core.domain.models.anchor.CorrectResult
import com.ritune.core.models.AnchorEvent
import com.ritune.core.models.AnchorId
import com.ritune.core.models.EventId
import com.ritune.core.models.EventSource
import com.ritune.core.models.IdGenerator
import com.ritune.core.models.Timekeeper
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.atTime
import kotlinx.datetime.offsetAt
import kotlinx.datetime.toInstant
import javax.inject.Inject

class CorrectAnchorTime @Inject constructor(
    private val anchorEventRepository: AnchorEventRepository,
    private val timekeeper: Timekeeper,
    private val ids: IdGenerator,
) {
    suspend operator fun invoke(
        eventId: EventId,
        anchorId: AnchorId,
        day: LocalDate,
        time: LocalTime
    ): CorrectResult {
        val zone = timekeeper.timeZone()
        val at = day.atTime(time).toInstant(zone)
        if (at > timekeeper.now().wall) return CorrectResult.InFuture

        val event = AnchorEvent(
            EventId(ids.newId()),
            anchorId,
            at,
            zone.offsetAt(at).totalSeconds,
            EventSource.BACKFILL
        )

        anchorEventRepository.replace(eventId, event)
        return CorrectResult.Corrected(event)
    }
}