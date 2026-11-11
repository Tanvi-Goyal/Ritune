package com.ritune.core.domain.usecases.coach.tools

import com.ritune.core.models.Anchor
import com.ritune.core.models.AnchorEvent
import kotlinx.datetime.UtcOffset
import kotlinx.datetime.asTimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.Serializable

@Serializable
internal data class AnchorEventsResult(
    val anchor: String,
    val days: Int,
    val logs: List<LogItem>,
)

@Serializable
internal data class LogItem(val id: String, val date: String, val time: String)

internal fun List<AnchorEvent>.toResult(anchor: Anchor, days: Int) = AnchorEventsResult(
    anchor = anchor.name,
    days = days,
    logs = sortedBy { it.occurredAt }.map { event ->
        val local = event.occurredAt.toLocalDateTime(
            UtcOffset(
                seconds = event.utcOffsetSeconds
            ).asTimeZone()
        )

        LogItem(event.id.value,
            local.date.toString(),
            local.time.toString().take(5))
    },
)
