package com.ritune.core.domain.usecases.coach.tools

import com.ritune.core.domain.usecases.anchor.ObserveAnchors
import com.ritune.core.models.DefaultCatalog
import com.ritune.core.models.coach.Proposal
import com.ritune.core.models.coach.ToolCall
import com.ritune.core.testing.repository.FakeAnchorRepository
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalTime
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ProposeAnchorTargetToolTest {

    private val anchors = FakeAnchorRepository()
    private val tool = ProposeAnchorTargetTool(ObserveAnchors(anchors))
    private val shutdown = DefaultCatalog.shutdown

    private suspend fun propose(vararg args: Pair<String, String>) =
        tool.propose(ToolCall(id = "1", name = tool.definition.name, arguments = args.toMap()))

    @Test
    fun validRequest_describesTheChangeWithoutSavingIt() = runTest {
        val proposal = propose("anchor_id" to shutdown.id.value, "target_time" to "22:00")

        assertEquals(
            Proposal.AnchorTarget(shutdown.id, "Screens off", from = LocalTime(21, 30), to = LocalTime(22, 0)),
            proposal,
        )
        assertEquals(LocalTime(21, 30), anchors.anchors.value.first { it.id == shutdown.id }.targetTime)
    }

    @Test
    fun unknownAnchor_asksTheModelToReadTheIds() = runTest {
        val error = assertFailsWith<ToolArgumentException> {
            propose("anchor_id" to "nope", "target_time" to "22:00")
        }
        assertEquals("Unknown anchor_id. Call get_anchors for the valid ids.", error.message)
    }

    @Test
    fun timeThatIsNotHoursAndMinutes_isRejected() = runTest {
        listOf("10pm", "25:00", "22:00:30").forEach { time ->
            assertFailsWith<ToolArgumentException>(time) {
                propose("anchor_id" to shutdown.id.value, "target_time" to time)
            }
        }
    }

    @Test
    fun sameTargetAsNow_isRejected() = runTest {
        assertFailsWith<ToolArgumentException> {
            propose("anchor_id" to shutdown.id.value, "target_time" to "21:30")
        }
    }
}
