package com.ritune.core.domain.repositories

import com.ritune.core.models.RunId
import com.ritune.core.models.RunSummary
import kotlinx.coroutines.flow.Flow

interface RunHistoryRepository {

    fun observe(runId: RunId): Flow<RunSummary?>
    suspend fun save(summary: RunSummary)
}