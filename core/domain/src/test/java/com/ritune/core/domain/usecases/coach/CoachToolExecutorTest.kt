package com.ritune.core.domain.usecases.coach

import com.ritune.core.domain.usecases.coach.tools.AnchorEventsTool
import com.ritune.core.domain.usecases.coach.tools.AnchorsTool
import com.ritune.core.domain.usecases.coach.tools.ProposeAnchorTargetTool
import com.ritune.core.models.DefaultCatalog
import com.ritune.core.models.coach.Proposal
import com.ritune.core.models.coach.ProposalOutcome
import com.ritune.core.models.coach.ToolCall
import com.ritune.core.testing.coach.coachToolExecutor
import com.ritune.core.testing.time.FakeTimekeeper
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class CoachToolExecutorTest {

    private val executor = coachToolExecutor(FakeTimekeeper())

    @Test
    fun everyDeclaredTool_hasAnImplementation() = runTest {
        CoachTools.all.forEach { tool ->
            val arguments = mapOf("anchor_id" to DefaultCatalog.wake.id.value)
            val call = ToolCall(id = "1", name = tool.name, arguments = arguments)

            val outcome = executor.execute(call)

            val json = (outcome as? ToolOutcome.Done)?.result?.json.orEmpty()
            assertTrue("Unknown tool" !in json, "${tool.name} has no implementation")
        }
    }

    private suspend fun CoachToolExecutor.done(call: ToolCall) =
        assertIs<ToolOutcome.Done>(execute(call)).result

    @Test
    fun writeTool_needsApprovalInsteadOfSaving() = runTest {
        val call = ToolCall(
            id = "1",
            name = ProposeAnchorTargetTool.DEFINITION.name,
            arguments = mapOf("anchor_id" to DefaultCatalog.shutdown.id.value, "target_time" to "22:00"),
        )

        val outcome = assertIs<ToolOutcome.NeedsApproval>(executor.execute(call))

        assertEquals(call, outcome.call)
        assertIs<Proposal.AnchorTarget>(outcome.proposal)
    }

    @Test
    fun badWriteRequest_returnsTheMessageWithoutAskingTheUser() = runTest {
        val call = ToolCall(id = "1", name = ProposeAnchorTargetTool.DEFINITION.name)

        assertEquals("""{"error":"anchor_id is required"}""", executor.done(call).json)
    }

    @Test
    fun decision_tellsTheModelWhatTheUserChose() {
        val call = ToolCall(id = "1", name = ProposeAnchorTargetTool.DEFINITION.name)

        assertEquals(
            """{"status":"declined","note":"The user declined. Acknowledge it in one short sentence and don't propose it again unless they ask."}""",
            executor.decided(call, ProposalOutcome.Declined).json,
        )
        assertEquals(
            """{"status":"not_applied","note":"The app couldn't save it. Tell the user in one short sentence."}""",
            executor.decided(call, ProposalOutcome.NotApplied).json,
        )
    }

    @Test
    fun knownTool_returnsItsResultForTheSameCall() = runTest {
        val call = ToolCall(id = "call-7", name = AnchorsTool.DEFINITION.name)

        val result = executor.done(call)

        assertEquals(call, result.call)
        assertTrue(result.json.startsWith("""{"anchors":["""))
    }

    @Test
    fun badArgument_returnsTheMessageForTheModel() = runTest {
        val call = ToolCall(id = "1", name = AnchorEventsTool.DEFINITION.name)

        val result = executor.done(call)

        assertEquals("""{"error":"anchor_id is required"}""", result.json)
    }

    @Test
    fun unknownTool_returnsAnErrorTheModelCanRead() = runTest {
        val result = executor.done(ToolCall(id = "1", name = "delete_everything"))

        assertEquals("""{"error":"Unknown tool: delete_everything"}""", result.json)
    }
}
