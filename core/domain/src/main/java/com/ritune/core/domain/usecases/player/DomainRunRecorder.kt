package com.ritune.core.domain.usecases.player

import com.ritune.core.domain.models.player.Session
import com.ritune.core.domain.ports.RunRecorder
import com.ritune.core.domain.repositories.RunHistoryRepository
import com.ritune.core.domain.usecases.anchor.LogAnchor
import com.ritune.core.models.EventSource
import com.ritune.core.models.RunSummary
import javax.inject.Inject

/** Glue between the player and the domain: starting a routine logs its anchor,
 * finishing saves the run. */
class DomainRunRecorder @Inject constructor(
    private val logAnchor: LogAnchor,
    private val historyRepository: RunHistoryRepository,
) : RunRecorder {
    override suspend fun onRunStarted(session: Session) {
        session.routine.anchorId?.let {
            logAnchor(
                it,
                EventSource.ROUTINE_START
            )
        }
    }

    override suspend fun onRunFinished(summary: RunSummary) =
        historyRepository.save(summary)
}