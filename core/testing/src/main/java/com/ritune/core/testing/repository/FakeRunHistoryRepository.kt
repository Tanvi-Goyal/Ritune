package com.ritune.core.testing.repository

import com.ritune.core.domain.repositories.RunHistoryRepository
import com.ritune.core.models.RunId
import com.ritune.core.models.RunSummary
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

class FakeRunHistoryRepository(
    summaries: List<RunSummary> = emptyList(),
) : RunHistoryRepository {

    val summaries = MutableStateFlow(summaries.associateBy { it.runId })

    override fun observe(runId: RunId): Flow<RunSummary?> = summaries.map { it[runId] }

    override suspend fun save(summary: RunSummary) {
        summaries.update { it + (summary.runId to summary) }
    }
}
