package com.ritune.core.testing.repository

import com.ritune.core.domain.repositories.RoutineRepository
import com.ritune.core.models.DefaultCatalog
import com.ritune.core.models.Routine
import com.ritune.core.models.RoutineId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

class FakeRoutineRepository(
    routines: List<Routine> = DefaultCatalog.routines,
) : RoutineRepository {

    val routines = MutableStateFlow(routines)

    override fun observeAll(): Flow<List<Routine>> = routines

    override suspend fun get(id: RoutineId): Routine? =
        routines.value.firstOrNull { it.id == id }

    override suspend fun save(routine: Routine) {
        routines.update { all ->
            val existing = all.firstOrNull { it.id == routine.id }
            if (existing == null) {
                all + routine
            } else {
                all.map { if (it.id == routine.id) routine.copy(builtIn = existing.builtIn) else it }
            }
        }
    }

    override suspend fun delete(id: RoutineId) {
        routines.update { all -> all.filterNot { it.id == id } }
    }
}
