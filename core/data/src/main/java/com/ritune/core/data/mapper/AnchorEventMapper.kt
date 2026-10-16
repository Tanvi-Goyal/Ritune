package com.ritune.core.data.mapper

import com.ritune.core.database.entities.AnchorEventEntity
import com.ritune.core.models.AnchorEvent
import com.ritune.core.models.AnchorId
import com.ritune.core.models.EventId
import com.ritune.core.models.EventSource
import kotlin.time.Instant

internal fun AnchorEventEntity.toAnchorEvent() = AnchorEvent(
    id = EventId(id),
    anchorId = AnchorId(anchorId),
    occurredAt = Instant.fromEpochMilliseconds(occurredAt),
    utcOffsetSeconds = utcOffsetSeconds,
    source = EventSource.valueOf(source),
)

internal fun AnchorEvent.toEntity() = AnchorEventEntity(
    id = id.id,
    anchorId = anchorId.id,
    occurredAt = occurredAt.toEpochMilliseconds(),
    utcOffsetSeconds = utcOffsetSeconds,
    source = source.name,
)