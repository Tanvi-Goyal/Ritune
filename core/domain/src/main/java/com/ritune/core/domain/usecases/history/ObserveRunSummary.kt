package com.ritune.core.domain.usecases.history

import com.ritune.core.domain.repositories.RunHistoryRepository
import com.ritune.core.models.RunId
import com.ritune.core.models.RunSummary
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveRunSummary @Inject constructor(
    private val runHistoryRepository: RunHistoryRepository
) {
    operator fun invoke(runId: RunId): Flow<RunSummary?> =
        runHistoryRepository.observe(runId)
}