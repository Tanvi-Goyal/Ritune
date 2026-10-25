package com.ritune.core.domain.usecases.anchor

import com.ritune.core.domain.repositories.AnchorEventRepository
import com.ritune.core.domain.models.anchor.LogResult
import com.ritune.core.models.AnchorEvent
import com.ritune.core.models.AnchorId
import com.ritune.core.models.EventId
import com.ritune.core.models.EventSource
import com.ritune.core.models.IdGenerator
import com.ritune.core.models.Timekeeper
import kotlinx.datetime.offsetAt
import javax.inject.Inject
import kotlin.time.Duration.Companion.seconds

/** The single entry point for "an anchor happened".
 * Screens, notifications, and routines all call this. */
class LogAnchor @Inject constructor(
    private val anchorEventRepository: AnchorEventRepository,
    private val timekeeper: Timekeeper,
    private val ids: IdGenerator,
) {
    private val dedupeWindow = 2.seconds

    suspend operator fun invoke(anchorId: AnchorId, source: EventSource): LogResult {
        val now = timekeeper.now().wall
        val last = anchorEventRepository.latest(anchorId)
        if (last != null && now - last.occurredAt < dedupeWindow)
            return LogResult.Duplicate(last)

        val event = AnchorEvent(
            id = EventId(ids.newId()),
            anchorId = anchorId,
            occurredAt = now,
            utcOffsetSeconds = timekeeper.timeZone().offsetAt(now).totalSeconds,
            source = source,
        )
        anchorEventRepository.append(event)
        return LogResult.Logged(event)
    }
}