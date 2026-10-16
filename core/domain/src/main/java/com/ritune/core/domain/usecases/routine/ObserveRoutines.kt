package com.ritune.core.domain.usecases.routine

import com.ritune.core.domain.repositories.RoutineRepository
import com.ritune.core.models.Routine
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveRoutines @Inject constructor(
    private val routineRepository: RoutineRepository,
) {
    operator fun invoke(): Flow<List<Routine>> {
        return routineRepository.observeAll()
    }
}
