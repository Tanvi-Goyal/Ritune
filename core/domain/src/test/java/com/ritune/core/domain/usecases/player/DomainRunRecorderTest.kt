package com.ritune.core.domain.usecases.player

import com.ritune.core.domain.usecases.anchor.LogAnchor
import com.ritune.core.models.DefaultCatalog
import com.ritune.core.models.EventSource
import com.ritune.core.testing.data.aRoutine
import com.ritune.core.testing.data.aRunSummary
import com.ritune.core.testing.data.aSession
import com.ritune.core.testing.id.SequentialIdGenerator
import com.ritune.core.testing.repository.FakeAnchorEventRepository
import com.ritune.core.testing.repository.FakeRunHistoryRepository
import com.ritune.core.testing.time.FakeTimekeeper
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.test.assertEquals

class DomainRunRecorderTest {

    private val events = FakeAnchorEventRepository()
    private val history = FakeRunHistoryRepository()
    private val recorder = DomainRunRecorder(
        LogAnchor(events, FakeTimekeeper(), SequentialIdGenerator("event")),
        history,
    )

    @Test
    fun onRunStarted_routineWithAnchor_logsAnchorAsRoutineStart() = runTest {
        recorder.onRunStarted(aSession(aRoutine(anchorId = DefaultCatalog.wake.id)))

        val logged = events.events.value.single()
        assertEquals(DefaultCatalog.wake.id, logged.anchorId)
        assertEquals(EventSource.ROUTINE_START, logged.source)
    }

    @Test
    fun onRunStarted_routineWithoutAnchor_logsNothing() = runTest {
        recorder.onRunStarted(aSession(aRoutine(anchorId = null)))

        assertEquals(emptyList(), events.events.value)
    }

    @Test
    fun onRunFinished_savesSummary() = runTest {
        val summary = aRunSummary()

        recorder.onRunFinished(summary)

        assertEquals(summary, history.observe(summary.runId).first())
    }
}
