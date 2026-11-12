package com.ritune.core.domain.usecases.coach

import com.ritune.core.domain.usecases.coach.tools.ProposeAnchorTargetTool
import com.ritune.core.domain.usecases.coach.tools.WeekRhythmTool
import com.ritune.core.models.DefaultCatalog
import com.ritune.core.models.coach.CoachError
import com.ritune.core.models.coach.CoachEvent
import com.ritune.core.models.coach.CoachException
import com.ritune.core.models.coach.CoachInput
import com.ritune.core.models.coach.CoachUsage
import com.ritune.core.models.coach.ConversationId
import com.ritune.core.models.coach.Decision
import com.ritune.core.models.coach.ModelEvent
import com.ritune.core.models.coach.Proposal
import com.ritune.core.models.coach.ProposalOutcome
import com.ritune.core.models.coach.ToolCall
import com.ritune.core.testing.coach.applyCoachProposal
import com.ritune.core.testing.coach.coachToolExecutor
import com.ritune.core.testing.ports.FakeCoachGateway
import com.ritune.core.testing.repository.FakeAnchorRepository
import com.ritune.core.testing.time.FakeTimekeeper
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalTime
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class RunCoachTurnTest {

    private val gateway = FakeCoachGateway()
    private val timekeeper = FakeTimekeeper()
    private val anchors = FakeAnchorRepository()
    private val executor = coachToolExecutor(timekeeper, anchors)
    private val runCoachTurn = RunCoachTurn(gateway, executor, applyCoachProposal(timekeeper, anchors))
    private val conversation = ConversationId("c-1")
    private val weekRhythmCall = ToolCall(id = "call-1", name = WeekRhythmTool.DEFINITION.name)
    private val shutdown = DefaultCatalog.shutdown

    private fun proposeTarget(id: String, time: String) = ToolCall(
        id = id,
        name = ProposeAnchorTargetTool.DEFINITION.name,
        arguments = mapOf("anchor_id" to shutdown.id.value, "target_time" to time),
    )

    private fun shutdownTarget() = anchors.anchors.value.first { it.id == shutdown.id }.targetTime

    private fun runCoachTurn(
        message: String,
        undone: List<Proposal> = emptyList(),
        approve: suspend (Proposal) -> Decision = { error("No proposal expected") },
    ) = runCoachTurn(conversation, message, approve, undone)

    @Test
    fun answerWithoutTools_streamsTextThenDone() = runTest {
        gateway.reply(ModelEvent.Text("Hi"), ModelEvent.Finished(CoachUsage(2, 3, 0)))

        val events = runCoachTurn("hello").toList()

        assertEquals(
            listOf<CoachEvent>(CoachEvent.TextDelta("Hi"), CoachEvent.Done(CoachUsage(2, 3, 0))),
            events,
        )
        assertEquals(listOf<CoachInput>(CoachInput.UserMessage("hello")), gateway.sent)
    }

    @Test
    fun toolRequest_runsTheToolAndSendsItsResultBack() = runTest {
        gateway.reply(
            ModelEvent.ToolCallRequest(weekRhythmCall),
            ModelEvent.Finished(CoachUsage(10, 5, 20)),
        )
        gateway.reply(ModelEvent.Text("Steady week."), ModelEvent.Finished(CoachUsage(30, 4, 0)))

        val events = runCoachTurn("How was my week?").toList()

        assertEquals(
            listOf<CoachEvent>(
                CoachEvent.ToolRunning(WeekRhythmTool.DEFINITION.name),
                CoachEvent.TextDelta("Steady week."),
                CoachEvent.Done(CoachUsage(40, 9, 20)),
            ),
            events,
        )
        val sentBack = assertIs<CoachInput.ToolResults>(gateway.sent[1]).results.single()
        assertEquals(weekRhythmCall, sentBack.call)
        assertTrue(sentBack.json.contains("\"week\""))
    }

    @Test
    fun modelKeepsAskingForTools_failsAfterTheCap() = runTest {
        repeat(RunCoachTurn.MAX_TOOL_ROUNDS + 1) {
            gateway.reply(ModelEvent.ToolCallRequest(weekRhythmCall), ModelEvent.Finished(CoachUsage()))
        }

        val events = runCoachTurn("loop").toList()

        assertEquals(CoachEvent.Failed(CoachError.TOO_MANY_TOOL_CALLS), events.last())
        assertEquals(RunCoachTurn.MAX_TOOL_ROUNDS + 1, gateway.sent.size)
    }

    @Test
    fun gatewayFailure_failsTheTurnWithItsError() = runTest {
        gateway.failure = CoachException(CoachError.QUOTA_EXCEEDED)

        val events = runCoachTurn("hi").toList()

        assertEquals(listOf<CoachEvent>(CoachEvent.Failed(CoachError.QUOTA_EXCEEDED)), events)
    }

    @Test
    fun unexpectedFailure_failsTheTurnAsUnknown() = runTest {
        val events = runCoachTurn("no reply scripted").toList()

        assertEquals(listOf<CoachEvent>(CoachEvent.Failed(CoachError.UNKNOWN)), events)
    }

    @Test
    fun writeTool_waitsForTheUserBeforeSavingOrReplying() = runTest {
        gateway.reply(ModelEvent.ToolCallRequest(proposeTarget("call-1", "22:00")), ModelEvent.Finished(CoachUsage()))
        gateway.reply(ModelEvent.Text("Done."), ModelEvent.Finished(CoachUsage()))
        val decision = CompletableDeferred<Decision>()
        val asked = mutableListOf<Proposal>()
        val events = mutableListOf<CoachEvent>()

        val turn = launch {
            runCoachTurn("Move screens off to 22:00") { asked += it; decision.await() }.toList(events)
        }
        advanceUntilIdle()

        assertEquals(LocalTime(22, 0), assertIs<Proposal.AnchorTarget>(asked.single()).to)
        assertEquals(LocalTime(21, 30), shutdownTarget())
        assertEquals(1, gateway.sent.size)

        decision.complete(Decision.APPLY)
        turn.join()

        assertEquals(LocalTime(22, 0), shutdownTarget())
        assertIs<ProposalOutcome.Applied>(assertIs<CoachEvent.ProposalResolved>(events[0]).outcome)
        val sentBack = assertIs<CoachInput.ToolResults>(gateway.sent[1]).results.single()
        assertTrue(sentBack.json.startsWith("""{"status":"applied""""))
    }

    @Test
    fun decline_tellsTheModelAndSavesNothing() = runTest {
        gateway.reply(ModelEvent.ToolCallRequest(proposeTarget("call-1", "22:00")), ModelEvent.Finished(CoachUsage()))
        gateway.reply(ModelEvent.Text("Okay."), ModelEvent.Finished(CoachUsage()))

        val events = runCoachTurn("Move screens off") { Decision.DECLINE }.toList()

        assertEquals(CoachEvent.ProposalResolved(ProposalOutcome.Declined), events[0])
        assertEquals(LocalTime(21, 30), shutdownTarget())
        val sentBack = assertIs<CoachInput.ToolResults>(gateway.sent[1]).results.single()
        assertTrue(sentBack.json.startsWith("""{"status":"declined""""))
    }

    @Test
    fun secondProposalInOneRound_isRefusedWithoutAskingTheUser() = runTest {
        gateway.reply(
            ModelEvent.ToolCallRequest(proposeTarget("call-1", "22:00")),
            ModelEvent.ToolCallRequest(proposeTarget("call-2", "22:15")),
            ModelEvent.Finished(CoachUsage()),
        )
        gateway.reply(ModelEvent.Text("Done."), ModelEvent.Finished(CoachUsage()))
        val asked = mutableListOf<Proposal>()

        runCoachTurn("Move screens off") { asked += it; Decision.APPLY }.toList()

        assertEquals(1, asked.size)
        val (first, second) = assertIs<CoachInput.ToolResults>(gateway.sent[1]).results
        assertTrue(first.json.startsWith("""{"status":"applied""""))
        assertEquals("call-2", second.call.id)
        assertTrue("one change at a time" in second.json)
        assertEquals(LocalTime(22, 0), shutdownTarget())
    }

    @Test
    fun undoneChanges_areNotedBeforeTheNextMessage() = runTest {
        gateway.reply(ModelEvent.Text("Noted."), ModelEvent.Finished(CoachUsage()))
        val undone = Proposal.AnchorTarget(shutdown.id, "Screens off", LocalTime(21, 30), LocalTime(22, 0))

        runCoachTurn("What now?", undone = listOf(undone)).toList()

        assertEquals(
            CoachInput.UserMessage(
                "[App note: since your last reply the user undid: the Screens off target change, " +
                    "so it is 21:30 again.]\n\nWhat now?",
            ),
            gateway.sent.single(),
        )
    }
}
