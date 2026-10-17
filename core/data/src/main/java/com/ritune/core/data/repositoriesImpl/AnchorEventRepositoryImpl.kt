package com.ritune.core.data.repositoriesImpl

import com.ritune.core.data.mapper.toModel
import com.ritune.core.data.mapper.toEntity
import com.ritune.core.domain.repositories.AnchorEventRepository
import com.ritune.core.database.dao.AnchorEventDao
import com.ritune.core.models.AnchorEvent
import com.ritune.core.models.AnchorId
import com.ritune.core.models.EventId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import kotlin.time.Instant

class AnchorEventRepositoryImpl @Inject constructor(
    private val dao: AnchorEventDao
) : AnchorEventRepository {

    override fun observeBetween(from: Instant, to: Instant): Flow<List<AnchorEvent>> =
        dao.observeBetween(from.toEpochMilliseconds(), to.toEpochMilliseconds())
            .map { rows -> rows.map { it.toModel() } }

    override suspend fun latest(anchorId: AnchorId): AnchorEvent? =
        dao.latest(anchorId.value)?.toModel()

    override suspend fun append(event: AnchorEvent) {
        dao.insert(event.toEntity())
    }

    override suspend fun remove(id: EventId) {
        dao.delete(id.value)
    }

    override suspend fun replace(id: EventId, event: AnchorEvent) {
        dao.update(id.value, event.toEntity())
    }
}
