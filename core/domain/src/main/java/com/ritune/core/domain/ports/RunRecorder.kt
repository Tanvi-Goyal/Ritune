package com.ritune.core.domain.ports

import com.ritune.core.models.RunSummary
import com.ritune.core.models.Session

interface RunRecorder {
    suspend fun onRunStarted(session: Session)
    suspend fun onRunFinished(summary: RunSummary)
}
