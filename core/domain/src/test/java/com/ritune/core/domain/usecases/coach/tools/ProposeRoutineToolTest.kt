package com.ritune.core.domain.usecases.coach.tools

import com.ritune.core.domain.usecases.anchor.ObserveAnchors
import com.ritune.core.models.DefaultCatalog
import com.ritune.core.models.coach.Proposal
import com.ritune.core.models.coach.ProposedStep
import com.ritune.core.models.coach.ToolCall
import com.ritune.core.testing.repository.FakeAnchorRepository
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

class ProposeRoutineToolTest {

    private val tool = ProposeRoutineTool(ObserveAnchors(FakeAnchorRepository()))
    private val shutdown = DefaultCatalog.shutdown

    private suspend fun propose(vararg args: Pair<String, String>) =
        tool.propose(ToolCall(id = "1", name = tool.definition.name, arguments = args.toMap()))

    @Test
    fun validRoutine_becomesAProposalWithItsAnchor() = runTest {
        val proposal = propose(
            "name" to " Wind down ",
            "anchor_id" to shutdown.id.value,
            // The model may send whole numbers as decimals.
            "steps" to """[{"title":"Dim lights","duration_seconds":60},{"title":"Read","duration_seconds":600.0}]""",
        )

        assertEquals(
            Proposal.NewRoutine(
                name = "Wind down",
                anchorId = shutdown.id,
                anchorName = "Screens off",
                steps = listOf(ProposedStep("Dim lights", 60.seconds), ProposedStep("Read", 10.minutes)),
            ),
            proposal,
        )
    }

    @Test
    fun routineTheValidatorRejects_tellsTheModelWhichRules() = runTest {
        val error = assertFailsWith<ToolArgumentException> {
            propose("name" to "Quick", "steps" to """[{"title":"Blink","duration_seconds":1}]""")
        }
        assertTrue("each step is at least 5 seconds" in error.message.orEmpty(), error.message)
    }

    @Test
    fun stepsThatAreNotAList_areRejected() = runTest {
        assertFailsWith<ToolArgumentException> {
            propose("name" to "Quick", "steps" to "Read for ten minutes")
        }
    }

    @Test
    fun unknownAnchor_isRejected() = runTest {
        assertFailsWith<ToolArgumentException> {
            propose(
                "name" to "Quick",
                "anchor_id" to "nope",
                "steps" to """[{"title":"Read","duration_seconds":60}]""",
            )
        }
    }
}
