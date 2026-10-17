package com.ritune.core.data.repositoriesImpl

import com.ritune.core.data.mapper.toDrafts
import com.ritune.core.data.mapper.toEntity
import com.ritune.core.data.mapper.toRoutine
import com.ritune.core.database.dao.RoutineDao
import com.ritune.core.domain.repositories.RoutineRepository
import com.ritune.core.models.Routine
import com.ritune.core.models.RoutineId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class RoutineRepositoryImpl @Inject constructor(
    private val dao: RoutineDao
) : RoutineRepository {
    override fun observeAll(): Flow<List<Routine>> =
        dao.observeAll()
            .map { rows ->
                rows.filter { it.steps.isNotEmpty() }.map { it.toRoutine() }
            }

    override suspend fun get(id: RoutineId): Routine? =
        dao.get(id.value)?.takeIf { it.steps.isNotEmpty() }?.toRoutine()

    override suspend fun save(routine: Routine) {
        val existing = dao.get(routine.id.value)
        val sortOrder = existing?.routine?.sortOrder ?: dao.count()
        val builtIn = existing?.routine?.builtIn ?: routine.builtIn
        dao.replace(
            routine = routine.toEntity(sortOrder = sortOrder, builtIn = builtIn),
            steps = routine.toDrafts()
        )
    }

    override suspend fun delete(id: RoutineId) {
        dao.delete(id.value)
    }
}