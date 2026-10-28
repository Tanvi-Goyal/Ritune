package com.ritune.core.database.dao

import androidx.room3.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ritune.core.database.RituneDatabase
import com.ritune.core.database.entities.RunEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals
import kotlin.test.assertNull

@RunWith(AndroidJUnit4::class)
class RunDaoTest {

    private lateinit var database: RituneDatabase
    private lateinit var dao: RunDao

    @Before
    fun createDatabase() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            RituneDatabase::class.java,
        ).build()
        dao = database.runDao()
    }

    @After
    fun closeDatabase() {
        database.close()
    }

    private val run = RunEntity(
        runId = "run-1", routineId = "r1", routineName = "Morning", startedAt = 100, finishedAt = 200,
        outcome = "COMPLETED", completedSteps = 2, skippedSteps = 0, totalSteps = 2,
    )

    @Test
    fun observe_unknownRun_isNull() = runTest {
        assertNull(dao.observe("missing").first())
    }

    @Test
    fun save_sameRunId_replacesRun() = runTest {
        dao.save(run)
        val stopped = run.copy(outcome = "STOPPED", completedSteps = 1)

        dao.save(stopped)

        assertEquals(stopped, dao.observe("run-1").first())
    }
}
