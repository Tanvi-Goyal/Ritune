package com.ritune.core.domain.usecases.routine

import com.ritune.core.domain.models.routine.SaveResult
import com.ritune.core.models.DefaultCatalog
import com.ritune.core.models.RoutineId
import com.ritune.core.models.StepId
import com.ritune.core.testing.id.SequentialIdGenerator
import com.ritune.core.testing.repository.FakeRoutineRepository
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.minutes

class SaveRoutineTest {

    private val repository = FakeRoutineRepository(emptyList())
    private val saveRoutine = SaveRoutine(repository, SequentialIdGenerator("id"))

    private fun step(title: String = "Stretch", instruction: String? = null, id: StepId? = null) =
        StepInput(id, title, 1.minutes, instruction, emptyList())

    @Test
    fun invoke_invalidInput_returnsIssuesWithoutSaving() = runTest {
        val result = saveRoutine(RoutineInput(null, " ", null, listOf(step())))

        assertEquals(SaveResult.Invalid(setOf(RoutineIssue.NAME_BLANK)), result)
        assertEquals(emptyList(), repository.routines.value)
    }

    @Test
    fun invoke_newRoutine_trimsTextAndGeneratesIds() = runTest {
        val input = RoutineInput(
            id = null,
            name = "  Morning ",
            anchorId = DefaultCatalog.wake.id,
            steps = listOf(step(" Stretch ", instruction = "  "), step("Breathe", instruction = " Slowly ")),
        )

        val saved = assertIs<SaveResult.Saved>(saveRoutine(input)).routine

        assertEquals(RoutineId("id-1"), saved.id)
        assertEquals("Morning", saved.name)
        assertEquals(listOf(StepId("id-2"), StepId("id-3")), saved.steps.map { it.id })
        assertEquals(listOf("Stretch", "Breathe"), saved.steps.map { it.title })
        assertNull(saved.steps[0].instruction)
        assertEquals("Slowly", saved.steps[1].instruction)
        assertEquals(listOf(saved), repository.routines.value)
    }

    @Test
    fun invoke_editedBuiltInRoutine_keepsIdsAndStaysBuiltIn() = runTest {
        val builtIn = DefaultCatalog.routines.first { it.builtIn }
        repository.routines.value = listOf(builtIn)
        val stepId = builtIn.steps.first().id

        val result = saveRoutine(RoutineInput(builtIn.id, "Renamed", null, listOf(step(id = stepId))))

        val saved = assertIs<SaveResult.Saved>(result).routine
        assertEquals(builtIn.id, saved.id)
        assertEquals(listOf(stepId), saved.steps.map { it.id })
        assertTrue(saved.builtIn)
    }
}
