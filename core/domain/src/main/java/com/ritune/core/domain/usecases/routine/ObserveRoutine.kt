package com.ritune.core.domain.usecases.routine

import com.ritune.core.domain.repositories.RoutineRepository
import com.ritune.core.models.Routine
import com.ritune.core.models.RoutineId
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveRoutine @Inject constructor(
    private val repository: RoutineRepository
) {
    operator fun invoke(id: RoutineId): Flow<Routine?> = repository.observe(id)
}
