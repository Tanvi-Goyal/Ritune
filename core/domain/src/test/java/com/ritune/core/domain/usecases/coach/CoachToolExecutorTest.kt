package com.ritune.core.domain.usecases.coach

import com.ritune.core.domain.usecases.coach.tools.AnchorEventsTool
import com.ritune.core.domain.usecases.coach.tools.AnchorsTool
import com.ritune.core.models.DefaultCatalog
import com.ritune.core.models.coach.ToolCall
import com.ritune.core.testing.coach.coachToolExecutor
import com.ritune.core.testing.time.FakeTimekeeper
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CoachToolExecutorTest {

    private val executor = coachToolExecutor(FakeTimekeeper())

    @Test
    fun everyDeclaredTool_hasAnImplementation() = runTest {
        CoachTools.all.forEach { tool ->
            val arguments = mapOf("anchor_id" to DefaultCatalog.wake.id.value)
            val call = ToolCall(id = "1", name = tool.name, arguments = arguments)

            val result = executor.execute(call)

            assertTrue("Unknown tool" !in result.json, "${tool.name} has no implementation")
        }
    }

    @Test
    fun knownTool_returnsItsResultForTheSameCall() = runTest {
        val call = ToolCall(id = "call-7", name = AnchorsTool.DEFINITION.name)

        val result = executor.execute(call)

        assertEquals(call, result.call)
        assertTrue(result.json.startsWith("""{"anchors":["""))
    }

    @Test
    fun badArgument_returnsTheMessageForTheModel() = runTest {
        val call = ToolCall(id = "1", name = AnchorEventsTool.DEFINITION.name)

        val result = executor.execute(call)

        assertEquals("""{"error":"anchor_id is required"}""", result.json)
    }

    @Test
    fun unknownTool_returnsAnErrorTheModelCanRead() = runTest {
        val result = executor.execute(ToolCall(id = "1", name = "delete_everything"))

        assertEquals("""{"error":"Unknown tool: delete_everything"}""", result.json)
    }
}
