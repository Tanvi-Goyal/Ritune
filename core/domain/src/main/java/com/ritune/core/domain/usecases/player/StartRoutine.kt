package com.ritune.core.domain.usecases.player

import com.ritune.core.domain.ports.PlayerController
import com.ritune.core.domain.ports.StartResult
import com.ritune.core.domain.repositories.RoutineRepository
import com.ritune.core.models.RoutineId
import javax.inject.Inject

class StartRoutine @Inject constructor(
    private val routines: RoutineRepository,
    private val player: PlayerController,
) {
    suspend operator fun invoke(id: RoutineId): StartResult {
        val routine = routines.get(id) ?: return StartResult.RoutineNotFound
        return player.start(routine)
    }
}