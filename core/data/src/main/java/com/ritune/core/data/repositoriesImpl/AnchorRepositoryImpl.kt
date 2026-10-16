package com.ritune.core.data.repositoriesImpl

import com.ritune.core.data.mapper.toAnchor
import com.ritune.core.domain.repositories.AnchorRepository
import com.ritune.core.database.dao.AnchorDao
import com.ritune.core.models.Anchor
import com.ritune.core.models.AnchorId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.LocalTime
import javax.inject.Inject

class AnchorRepositoryImpl @Inject constructor(
    private val dao: AnchorDao,
) : AnchorRepository {

    override fun observeAll(): Flow<List<Anchor>> {
        return dao.observeActiveAnchors().map { entities ->
            entities.map { it.toAnchor() }
        }
    }

    override suspend fun updateTarget(id: AnchorId, target: LocalTime?) =
        dao.updateTarget(id.id, target?.toMinutes())
}

private fun LocalTime?.toMinutes(): Int? {
    if (this == null) return null
    return hour * 60 + minute
}
