package com.ritune.core.domain.usecases.routine

import app.cash.turbine.test
import com.ritune.core.domain.models.routine.DeleteResult
import com.ritune.core.models.RoutineId
import com.ritune.core.testing.data.aRoutine
import com.ritune.core.testing.repository.FakeRoutineRepository
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class DeleteRoutineTest {

    private val custom = aRoutine(id = "custom")
    private val builtIn = aRoutine(id = "built-in", builtIn = true)
    private val repository = FakeRoutineRepository(listOf(custom, builtIn))
    private val deleteRoutine = DeleteRoutine(repository)

    @Test
    fun invoke_customRoutine_deletesIt() = runTest {
        assertEquals(DeleteResult.Deleted, deleteRoutine(custom.id))
        assertEquals(listOf(builtIn), repository.routines.value)
    }

    @Test
    fun invoke_builtInRoutine_keepsIt() = runTest {
        assertEquals(DeleteResult.BuiltIn, deleteRoutine(builtIn.id))
        assertEquals(listOf(custom, builtIn), repository.routines.value)
    }

    @Test
    fun invoke_unknownRoutine_isNotFound() = runTest {
        assertEquals(DeleteResult.NotFound, deleteRoutine(RoutineId("missing")))
    }

    @Test
    fun observeRoutine_afterDelete_emitsNull() = runTest {
        ObserveRoutine(repository)(custom.id).test {
            assertEquals(custom, awaitItem())

            deleteRoutine(custom.id)

            assertNull(awaitItem())
        }
    }
}
