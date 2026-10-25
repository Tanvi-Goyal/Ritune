package com.ritune.core.testing.ports

import com.ritune.core.domain.ports.RunRecorder
import com.ritune.core.models.RunSummary
import com.ritune.core.models.Session

class FakeRunRecorder : RunRecorder {

    val started = mutableListOf<Session>()
    val finished = mutableListOf<RunSummary>()

    override suspend fun onRunStarted(session: Session) {
        started += session
    }

    override suspend fun onRunFinished(summary: RunSummary) {
        finished += summary
    }
}
