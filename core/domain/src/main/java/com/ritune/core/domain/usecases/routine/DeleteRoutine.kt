package com.ritune.core.domain.usecases.routine

import com.ritune.core.domain.models.routine.DeleteResult
import com.ritune.core.domain.repositories.RoutineRepository
import com.ritune.core.models.RoutineId
import javax.inject.Inject

class DeleteRoutine @Inject constructor(
    private val repository: RoutineRepository,
) {
    suspend operator fun invoke(id: RoutineId): DeleteResult {
        val routine = repository.get(id) ?: return DeleteResult.NotFound
        if (routine.builtIn) return DeleteResult.BuiltIn
        repository.delete(id)
        return DeleteResult.Deleted
    }
}
