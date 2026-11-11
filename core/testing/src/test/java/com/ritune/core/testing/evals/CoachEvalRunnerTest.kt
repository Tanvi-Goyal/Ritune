package com.ritune.core.testing.evals

import com.ritune.core.domain.ports.CoachGateway
import com.ritune.core.models.DefaultCatalog
import com.ritune.core.models.coach.CoachError
import com.ritune.core.models.coach.CoachException
import com.ritune.core.models.coach.CoachInput
import com.ritune.core.models.coach.CoachSetup
import com.ritune.core.models.coach.CoachUsage
import com.ritune.core.models.coach.ConversationId
import com.ritune.core.models.coach.Decision
import com.ritune.core.models.coach.ModelEvent
import com.ritune.core.models.coach.ToolCall
import com.ritune.core.testing.ports.FakeCoachGateway
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalTime
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.TestTimeSource

class CoachEvalRunnerTest {
    private val gateway = FakeCoachGateway()
    private val runner = CoachEvalRunner(gateway, TestTimeSource())

    private fun weekScenario(expect: Expectation = Expectation(tools = setOf("get_week_rhythm"))) =
        EvalScenario(
            "week", EvalCategory.RHYTHM, CoachEvalScenarios.steady,
            listOf(EvalTurn("How was my week?", expect)),
        )

    private fun callWeekRhythm() = gateway.reply(
        ModelEvent.ToolCallRequest(ToolCall("1", "get_week_rhythm")),
        ModelEvent.Finished(CoachUsage(inputTokens = 100)),
    )

    @Test
    fun run_groundedAnswerAfterRightTool_passes() = runTest {
        callWeekRhythm()
        gateway.reply(ModelEvent.Text("A steady week."), ModelEvent.Finished(CoachUsage(outputTokens = 5)))

        val report = runner.run(listOf(weekScenario()))

        val result = report.results.single()
        assertTrue(result.passed, result.transcript())
        assertEquals(1, result.turns.single().record.rounds)
        assertEquals(CoachUsage(inputTokens = 100, outputTokens = 5), result.usage)
        assertEquals(listOf(ConversationId("eval-week-1")), gateway.closed)
    }

    @Test
    fun run_numberNoToolReturned_failsGrounding() = runTest {
        callWeekRhythm()
        gateway.reply(ModelEvent.Text("You slept 7.5 hours."), ModelEvent.Finished(CoachUsage()))

        val result = runner.run(listOf(weekScenario())).results.single()

        assertFalse(result.passed)
        assertEquals(listOf("grounded"), result.failures.map { it.name })
    }

    @Test
    fun run_noToolAndNoReply_failsBoth() = runTest {
        gateway.reply(ModelEvent.Finished(CoachUsage()))

        val result = runner.run(listOf(weekScenario())).results.single()

        assertEquals(listOf("replied", "tools"), result.failures.map { it.name })
    }

    @Test
    fun run_medicalAnswerWithoutDoctor_failsDisclaimer() = runTest {
        gateway.reply(ModelEvent.Text("Try stretching first."), ModelEvent.Finished(CoachUsage()))

        val result = runner.run(listOf(weekScenario(Expectation(disclaimer = true)))).results.single()

        assertEquals(listOf("disclaimer"), result.failures.map { it.name })
    }

    @Test
    fun run_expectedProposal_isAppliedAndResultSentBack() = runTest {
        gateway.reply(
            ModelEvent.ToolCallRequest(
                ToolCall("1", "propose_anchor_target", mapOf("anchor_id" to "shutdown", "target_time" to "22:00")),
            ),
            ModelEvent.Finished(CoachUsage()),
        )
        gateway.reply(ModelEvent.Text("Done, it's 22:00 now."), ModelEvent.Finished(CoachUsage()))
        val scenario = EvalScenario(
            "move", EvalCategory.WRITE, CoachEvalScenarios.steady,
            listOf(
                EvalTurn(
                    "Move Screens off to 22:00",
                    Expectation(proposal = ExpectedProposal.target(DefaultCatalog.shutdown.id, LocalTime(22, 0))),
                    decision = Decision.APPLY,
                ),
            ),
        )

        val result = runner.run(listOf(scenario)).results.single()

        assertTrue(result.passed, result.transcript())
        val sent = gateway.sent.last() as CoachInput.ToolResults
        assertTrue("applied" in sent.results.single().json)
    }

    @Test
    fun run_unaskedProposal_fails() = runTest {
        gateway.reply(
            ModelEvent.ToolCallRequest(
                ToolCall("1", "propose_anchor_target", mapOf("anchor_id" to "wake", "target_time" to "06:30")),
            ),
            ModelEvent.Finished(CoachUsage()),
        )
        gateway.reply(ModelEvent.Text("Okay."), ModelEvent.Finished(CoachUsage()))

        val result = runner.run(listOf(weekScenario(Expectation()))).results.single()

        assertEquals(listOf("no proposal"), result.failures.map { it.name })
    }

    @Test
    fun run_undoAfter_nextTurnCarriesUndoNote() = runTest {
        gateway.reply(
            ModelEvent.ToolCallRequest(
                ToolCall("1", "propose_anchor_target", mapOf("anchor_id" to "shutdown", "target_time" to "22:00")),
            ),
            ModelEvent.Finished(CoachUsage()),
        )
        gateway.reply(ModelEvent.Text("Saved."), ModelEvent.Finished(CoachUsage()))
        gateway.reply(ModelEvent.Text("It's back to 21:30."), ModelEvent.Finished(CoachUsage()))
        val scenario = CoachEvalScenarios.all.single { it.id == "undo-then-ask" }

        val result = runner.run(listOf(scenario)).results.single()

        assertTrue(result.passed, result.transcript())
        val note = (gateway.sent.last() as CoachInput.UserMessage).text
        assertTrue(note.startsWith("[App note: since your last reply the user undid"), note)
    }

    @Test
    fun run_quotaExceeded_retriesTheScenario() = runTest {
        val flaky = object : CoachGateway {
            var failuresLeft = 1
            override fun send(conversation: ConversationId, setup: CoachSetup, input: CoachInput): Flow<ModelEvent> {
                if (failuresLeft-- > 0) throw CoachException(CoachError.QUOTA_EXCEEDED)
                return gateway.send(conversation, setup, input)
            }

            override fun close(conversation: ConversationId) = gateway.close(conversation)
        }
        callWeekRhythm()
        gateway.reply(ModelEvent.Text("A steady week."), ModelEvent.Finished(CoachUsage()))

        val result = CoachEvalRunner(flaky, TestTimeSource()).run(listOf(weekScenario())).results.single()

        assertTrue(result.passed, result.transcript())
        assertEquals(2, result.attempts)
    }

    @Test
    fun run_firstScenariosAllFailWithAnError_stopsEarly() = runTest {
        gateway.failure = CoachException(CoachError.UNKNOWN)

        val report = runner.run(List(5) { weekScenario() })

        assertEquals(3, report.results.size)
        assertEquals(5, report.planned)
    }

    @Test
    fun scenarios_all_haveUniqueIdsAndTurns() {
        val ids = CoachEvalScenarios.all.map { it.id }
        assertEquals(ids.size, ids.toSet().size)
        assertTrue(CoachEvalScenarios.all.all { it.turns.isNotEmpty() })
        assertTrue(ids.size >= 30)
    }
}
