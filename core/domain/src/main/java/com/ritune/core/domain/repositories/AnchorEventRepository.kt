package com.ritune.core.domain.repositories

import com.ritune.core.models.AnchorEvent
import com.ritune.core.models.AnchorId
import com.ritune.core.models.EventId
import kotlinx.coroutines.flow.Flow
import kotlin.time.Instant

interface AnchorEventRepository {
    fun observeBetween(from: Instant, to: Instant): Flow<List<AnchorEvent>>
    suspend fun latest(anchorId: AnchorId): AnchorEvent?
    suspend fun append(event: AnchorEvent)
    suspend fun remove(id: EventId)
    suspend fun replace(id: EventId, event: AnchorEvent)
}