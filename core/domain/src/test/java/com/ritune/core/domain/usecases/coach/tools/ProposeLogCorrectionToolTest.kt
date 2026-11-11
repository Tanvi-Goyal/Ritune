package com.ritune.core.domain.usecases.coach.tools

import com.ritune.core.domain.usecases.anchor.ObserveAnchors
import com.ritune.core.models.coach.Proposal
import com.ritune.core.models.coach.ToolCall
import com.ritune.core.testing.data.anAnchorEvent
import com.ritune.core.testing.repository.FakeAnchorEventRepository
import com.ritune.core.testing.repository.FakeAnchorRepository
import com.ritune.core.testing.time.FakeTimekeeper
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.time.Instant

class ProposeLogCorrectionToolTest {

    // The fake clock's "now" is Monday 2026-01-05 08:00 UTC.
    private val yesterday = anAnchorEvent(id = "log-1", occurredAt = Instant.parse("2026-01-04T07:40:00Z"))
    private val events = FakeAnchorEventRepository(listOf(yesterday))
    private val tool = ProposeLogCorrectionTool(ObserveAnchors(FakeAnchorRepository()), events, FakeTimekeeper())

    private suspend fun propose(vararg args: Pair<String, String>) =
        tool.propose(ToolCall(id = "1", name = tool.definition.name, arguments = args.toMap()))

    @Test
    fun knownLog_proposesTheNewTimeOnTheSameDay() = runTest {
        val proposal = propose("log_id" to "log-1", "time" to "07:10")

        assertEquals(
            Proposal.LogCorrection(
                original = yesterday,
                anchorName = "Wake",
                date = LocalDate(2026, 1, 4),
                from = LocalTime(7, 40),
                to = LocalTime(7, 10),
            ),
            proposal,
        )
        assertEquals(listOf(yesterday), events.events.value)
    }

    @Test
    fun unknownLog_asksTheModelToReadTheIds() = runTest {
        val error = assertFailsWith<ToolArgumentException> { propose("log_id" to "nope", "time" to "07:10") }
        assertEquals("Unknown log_id. Call get_anchor_events for the valid ids.", error.message)
    }

    @Test
    fun timeInTheFuture_isRejected() = runTest {
        val today = anAnchorEvent(id = "log-2", occurredAt = Instant.parse("2026-01-05T07:00:00Z"))
        events.events.value = listOf(today)

        assertFailsWith<ToolArgumentException> { propose("log_id" to "log-2", "time" to "09:00") }
    }
}
