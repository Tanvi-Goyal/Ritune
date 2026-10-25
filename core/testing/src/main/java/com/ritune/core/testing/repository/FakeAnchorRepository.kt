package com.ritune.core.testing.repository

import com.ritune.core.domain.repositories.AnchorRepository
import com.ritune.core.models.Anchor
import com.ritune.core.models.AnchorId
import com.ritune.core.models.DefaultCatalog
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.datetime.LocalTime

class FakeAnchorRepository(
    anchors: List<Anchor> = DefaultCatalog.anchors,
) : AnchorRepository {

    val anchors = MutableStateFlow(anchors)

    override fun observeAll(): Flow<List<Anchor>> = anchors

    override suspend fun updateTarget(id: AnchorId, target: LocalTime?) {
        anchors.update { all ->
            all.map { if (it.id == id) it.copy(targetTime = target) else it }
        }
    }
}
