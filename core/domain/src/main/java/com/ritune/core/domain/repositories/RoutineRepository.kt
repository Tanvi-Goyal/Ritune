package com.ritune.core.domain.repositories

import com.ritune.core.models.Routine
import com.ritune.core.models.RoutineId
import kotlinx.coroutines.flow.Flow

interface RoutineRepository {
    fun observeAll(): Flow<List<Routine>>
    suspend fun get(id: RoutineId): Routine?
    suspend fun save(routine: Routine)
    suspend fun delete(id: RoutineId)
}