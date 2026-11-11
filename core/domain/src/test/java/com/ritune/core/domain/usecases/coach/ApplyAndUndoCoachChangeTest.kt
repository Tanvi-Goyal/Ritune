package com.ritune.core.domain.usecases.coach

import com.ritune.core.models.DefaultCatalog
import com.ritune.core.models.coach.AppliedChange
import com.ritune.core.models.coach.Proposal
import com.ritune.core.models.coach.ProposalOutcome
import com.ritune.core.models.coach.ProposedStep
import com.ritune.core.testing.coach.applyCoachProposal
import com.ritune.core.testing.coach.undoCoachChange
import com.ritune.core.testing.data.anAnchorEvent
import com.ritune.core.testing.repository.FakeAnchorEventRepository
import com.ritune.core.testing.repository.FakeAnchorRepository
import com.ritune.core.testing.repository.FakeRoutineRepository
import com.ritune.core.testing.time.FakeTimekeeper
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant

class ApplyAndUndoCoachChangeTest {

    // The fake clock's "now" is Monday 2026-01-05 08:00 UTC.
    private val timekeeper = FakeTimekeeper()
    private val anchors = FakeAnchorRepository()
    private val events = FakeAnchorEventRepository()
    private val routines = FakeRoutineRepository(emptyList())
    private val apply = applyCoachProposal(timekeeper, anchors, events, routines)
    private val undo = undoCoachChange(anchors, events, routines)
    private val shutdown = DefaultCatalog.shutdown

    private fun target() = anchors.anchors.value.first { it.id == shutdown.id }.targetTime

    @Test
    fun anchorTarget_appliesThenUndoRestoresTheOldTarget() = runTest {
        val proposal =
            Proposal.AnchorTarget(shutdown.id, shutdown.name, LocalTime(21, 30), LocalTime(22, 0))

        val applied = assertIs<ProposalOutcome.Applied>(apply(proposal))
        assertEquals(LocalTime(22, 0), target())

        undo(applied.change)
        assertEquals(LocalTime(21, 30), target())
    }

    @Test
    fun newRoutine_isSavedThenUndoDeletesIt() = runTest {
        val proposal = Proposal.NewRoutine(
            "Wind down",
            shutdown.id,
            shutdown.name,
            listOf(ProposedStep("Read", 10.minutes))
        )

        val applied = assertIs<ProposalOutcome.Applied>(apply(proposal))
        val saved = routines.routines.value.single()
        assertEquals("Wind down", saved.name)
        assertEquals(shutdown.id, saved.anchorId)
        assertEquals(saved.id, assertIs<AppliedChange.RoutineCreated>(applied.change).routineId)

        undo(applied.change)
        assertEquals(emptyList(), routines.routines.value)
    }

    @Test
    fun logCorrection_movesTheLogThenUndoPutsTheOriginalBack() = runTest {
        val original =
            anAnchorEvent(id = "log-1", occurredAt = Instant.parse("2026-01-04T07:40:00Z"))
        events.events.value = listOf(original)
        val proposal = Proposal.LogCorrection(
            original,
            "Wake",
            LocalDate(2026, 1, 4),
            LocalTime(7, 40),
            LocalTime(7, 10)
        )

        val applied = assertIs<ProposalOutcome.Applied>(apply(proposal))
        assertEquals(Instant.parse("2026-01-04T07:10:00Z"), events.events.value.single().occurredAt)

        undo(applied.change)
        assertEquals(listOf(original), events.events.value)
    }

    @Test
    fun logCorrectionThatBecameFuture_isNotApplied() = runTest {
        val original =
            anAnchorEvent(id = "log-1", occurredAt = Instant.parse("2026-01-05T07:00:00Z"))
        events.events.value = listOf(original)
        val proposal = Proposal.LogCorrection(
            original,
            "Wake",
            LocalDate(2026, 1, 5),
            LocalTime(7, 0),
            LocalTime(9, 0)
        )

        assertEquals(ProposalOutcome.NotApplied, apply(proposal))
        assertEquals(listOf(original), events.events.value)
    }
}
