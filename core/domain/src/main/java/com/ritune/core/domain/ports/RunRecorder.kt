package com.ritune.core.domain.ports

import com.ritune.core.domain.models.player.Session
import com.ritune.core.models.RunSummary

interface RunRecorder {
    suspend fun onRunStarted(session: Session)
    suspend fun onRunFinished(summary: RunSummary)
}
