package com.ritune.core.domain.repositories

import com.ritune.core.models.RunSummary

interface RunHistoryRepository {
    suspend fun save(summary: RunSummary)
}