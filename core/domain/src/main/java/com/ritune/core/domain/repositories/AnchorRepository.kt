package com.ritune.core.domain.repositories

import com.ritune.core.models.Anchor
import com.ritune.core.models.AnchorId
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalTime

interface AnchorRepository {
    fun observeAll(): Flow<List<Anchor>>

    suspend fun updateTarget(id: AnchorId, target: LocalTime?)
}