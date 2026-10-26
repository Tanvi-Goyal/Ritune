package com.ritune.core.testing.repository

import com.ritune.core.domain.repositories.AnchorEventRepository
import com.ritune.core.models.AnchorEvent
import com.ritune.core.models.AnchorId
import com.ritune.core.models.EventId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlin.time.Instant

class FakeAnchorEventRepository(
    events: List<AnchorEvent> = emptyList(),
) : AnchorEventRepository {

    val events = MutableStateFlow(events)

    override fun observeBetween(from: Instant, to: Instant): Flow<List<AnchorEvent>> =
        events.map { all ->
            all.filter { it.occurredAt >= from && it.occurredAt < to }
                .sortedBy { it.occurredAt }
        }

    override suspend fun latest(anchorId: AnchorId): AnchorEvent? =
        events.value.filter { it.anchorId == anchorId }.maxByOrNull { it.occurredAt }

    override suspend fun append(event: AnchorEvent) {
        events.update { it + event }
    }

    override suspend fun remove(id: EventId) {
        events.update { all -> all.filterNot { it.id == id } }
    }

    override suspend fun replace(id: EventId, event: AnchorEvent) {
        events.update { all ->
            all.map { if (it.id == id) event.copy(id = id) else it }
        }
    }
}
