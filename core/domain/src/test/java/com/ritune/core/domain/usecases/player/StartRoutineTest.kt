package com.ritune.core.domain.usecases.player

import com.ritune.core.domain.ports.StartResult
import com.ritune.core.models.RoutineId
import com.ritune.core.testing.data.aRoutine
import com.ritune.core.testing.ports.FakePlayerController
import com.ritune.core.testing.repository.FakeRoutineRepository
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.test.assertEquals

class StartRoutineTest {

    private val routine = aRoutine()
    private val player = FakePlayerController()
    private val startRoutine = StartRoutine(FakeRoutineRepository(listOf(routine)), player)

    @Test
    fun invoke_unknownRoutine_isNotFoundAndDoesNotStart() = runTest {
        assertEquals(StartResult.RoutineNotFound, startRoutine(RoutineId("missing")))
        assertEquals(emptyList(), player.startedRoutines)
    }

    @Test
    fun invoke_knownRoutine_startsItAndReturnsPlayerResult() = runTest {
        player.startResult = StartResult.AlreadyRunning

        assertEquals(StartResult.AlreadyRunning, startRoutine(routine.id))
        assertEquals(listOf(routine), player.startedRoutines)
    }
}
