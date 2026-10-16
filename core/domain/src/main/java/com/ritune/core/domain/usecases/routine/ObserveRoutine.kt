package com.ritune.core.domain.usecases.routine

import com.ritune.core.domain.repositories.RoutineRepository
import com.ritune.core.models.Routine
import com.ritune.core.models.RoutineId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class ObserveRoutine @Inject constructor(
    private val repository: RoutineRepository
) {
    operator fun invoke(id: RoutineId): Flow<Routine?> =
        repository.observeAll().map { list -> list.firstOrNull { it.id == id } }
}
