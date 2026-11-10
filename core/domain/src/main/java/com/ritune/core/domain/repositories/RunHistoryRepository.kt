package com.ritune.core.domain.repositories

import com.ritune.core.models.RunId
import com.ritune.core.models.RunSummary
import kotlinx.coroutines.flow.Flow
import kotlin.time.Instant

interface RunHistoryRepository {

    fun observe(runId: RunId): Flow<RunSummary?>

    fun observeSince(from: Instant): Flow<List<RunSummary>>

    suspend fun save(summary: RunSummary)
}