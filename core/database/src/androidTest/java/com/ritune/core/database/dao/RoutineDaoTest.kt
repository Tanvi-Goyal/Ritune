package com.ritune.core.database.dao

import androidx.room3.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ritune.core.database.RituneDatabase
import com.ritune.core.database.entities.CueType
import com.ritune.core.database.entities.RoutineEntity
import com.ritune.core.database.entities.StepCueEntity
import com.ritune.core.database.entities.StepEntity
import com.ritune.core.database.relations.StepDraft
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertNull

@RunWith(AndroidJUnit4::class)
class RoutineDaoTest {

    private lateinit var database: RituneDatabase
    private lateinit var dao: RoutineDao

    @Before
    fun createDatabase() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            RituneDatabase::class.java,
        ).build()
        dao = database.routineDao()
    }

    @After
    fun closeDatabase() {
        database.close()
    }

    private fun routine(id: String = "r1", sortOrder: Int = 0) =
        RoutineEntity(id, name = "Routine $id", anchorId = null, sortOrder = sortOrder, builtIn = false)

    private fun step(position: Int, vararg cues: String, routineId: String = "r1") = StepDraft(
        step = StepEntity(
            routineId = routineId,
            stepId = "s$position",
            position = position,
            title = "Step $position",
            durationMs = 60_000,
            instruction = null,
        ),
        cues = cues.mapIndexed { index, text ->
            StepCueEntity(stepRowId = 0, position = index, type = CueType.SPEECH, value = text)
        },
    )

    @Test
    fun replace_newRoutine_storesStepsAndCues() = runTest {
        dao.replace(routine(), listOf(step(0, "breathe"), step(1)))

        val stored = dao.get("r1")!!
        assertEquals(listOf("s0", "s1"), stored.steps.sortedBy { it.step.position }.map { it.step.stepId })
        assertEquals(listOf("breathe"), stored.steps.first { it.step.position == 0 }.cues.map { it.value })
    }

    @Test
    fun replace_existingRoutine_dropsOldSteps() = runTest {
        dao.replace(routine(), listOf(step(0), step(1), step(2)))

        dao.replace(routine().copy(name = "Renamed"), listOf(step(0, "new cue")))

        val stored = dao.get("r1")!!
        assertEquals("Renamed", stored.routine.name)
        assertEquals(1, stored.steps.size)
        assertEquals(listOf("new cue"), stored.steps.single().cues.map { it.value })
    }

    @Test
    fun replace_failingHalfway_rollsBackEverything() = runTest {
        dao.replace(routine(), listOf(step(0)))

        assertFails {
            dao.replace(routine().copy(name = "Broken"), listOf(step(0), step(0)))
        }

        val stored = dao.get("r1")!!
        assertEquals("Routine r1", stored.routine.name)
        assertEquals(1, stored.steps.size)
    }

    @Test
    fun delete_removesRoutineAndCascadesSteps() = runTest {
        dao.replace(routine(), listOf(step(0, "cue")))

        dao.delete("r1")

        assertNull(dao.get("r1"))
        dao.replace(routine(), listOf(step(0)))
        assertEquals(1, dao.get("r1")!!.steps.size)
    }

    @Test
    fun observeAll_ordersBySortOrder() = runTest {
        dao.replace(routine("late", sortOrder = 2), listOf(step(0, routineId = "late")))
        dao.replace(routine("early", sortOrder = 0), listOf(step(0, routineId = "early")))
        dao.replace(routine("middle", sortOrder = 1), listOf(step(0, routineId = "middle")))

        val ids = dao.observeAll().first().map { it.routine.id }

        assertEquals(listOf("early", "middle", "late"), ids)
    }
}
