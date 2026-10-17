package com.ritune.core.data.mapper

import com.ritune.core.database.entities.AnchorEventEntity
import com.ritune.core.models.AnchorEvent
import com.ritune.core.models.AnchorId
import com.ritune.core.models.EventId
import com.ritune.core.models.EventSource
import kotlin.time.Instant

internal fun AnchorEventEntity.toModel() = AnchorEvent(
    id = EventId(id),
    anchorId = AnchorId(anchorId),
    occurredAt = Instant.fromEpochMilliseconds(occurredAt),
    utcOffsetSeconds = utcOffsetSeconds,
    source = EventSource.valueOf(source),
)

internal fun AnchorEvent.toEntity() = AnchorEventEntity(
    id = id.value,
    anchorId = anchorId.value,
    occurredAt = occurredAt.toEpochMilliseconds(),
    utcOffsetSeconds = utcOffsetSeconds,
    source = source.name,
)