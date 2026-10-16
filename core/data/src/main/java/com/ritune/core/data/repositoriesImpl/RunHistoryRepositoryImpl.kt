package com.ritune.core.data.repositoriesImpl

import com.ritune.core.data.mapper.toEntity
import com.ritune.core.domain.repositories.RunHistoryRepository
import com.ritune.core.database.dao.RoutineDao
import com.ritune.core.database.dao.RunDao
import com.ritune.core.models.RunSummary
import javax.inject.Inject

class RunHistoryRepositoryImpl @Inject constructor(
    private val runs: RunDao,
    private val routines: RoutineDao
) : RunHistoryRepository {
    override suspend fun save(summary: RunSummary) {
        val name = routines.nameOf(summary.routineId.id).orEmpty()
        runs.save(summary.toEntity(routineName = name))
    }
}