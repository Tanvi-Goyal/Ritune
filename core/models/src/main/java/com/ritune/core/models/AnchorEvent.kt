package com.ritune.core.models

import kotlin.time.Instant

data class AnchorEvent(
    val id: EventId,
    val anchorId: AnchorId,
    val occurredAt: Instant,
    val utcOffsetSeconds: Int,
    val source: EventSource,
)

