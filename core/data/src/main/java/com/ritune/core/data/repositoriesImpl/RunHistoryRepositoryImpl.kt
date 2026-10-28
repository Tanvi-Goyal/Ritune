package com.ritune.core.data.repositoriesImpl

import com.ritune.core.data.mapper.toEntity
import com.ritune.core.data.mapper.toModel
import com.ritune.core.database.dao.RunDao
import com.ritune.core.domain.repositories.RunHistoryRepository
import com.ritune.core.models.RunId
import com.ritune.core.models.RunSummary
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class RunHistoryRepositoryImpl @Inject constructor(
    private val runs: RunDao,
) : RunHistoryRepository {
    override suspend fun save(summary: RunSummary) {
        runs.save(summary.toEntity())
    }

    override fun observe(runId: RunId): Flow<RunSummary?> {
        val run = runs.observe(runId.value)
        return run.map { it?.toModel() }
    }
}
