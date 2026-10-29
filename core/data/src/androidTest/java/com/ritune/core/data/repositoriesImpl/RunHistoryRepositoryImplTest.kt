package com.ritune.core.data.repositoriesImpl

import androidx.room3.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ritune.core.database.RituneDatabase
import com.ritune.core.testing.data.aRoutine
import com.ritune.core.testing.data.aRunSummary
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals

@RunWith(AndroidJUnit4::class)
class RunHistoryRepositoryImplTest {

    private lateinit var database: RituneDatabase
    private lateinit var repository: RunHistoryRepositoryImpl

    @Before
    fun createDatabase() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            RituneDatabase::class.java,
        ).build()
        repository = RunHistoryRepositoryImpl(database.runDao())
    }

    @After
    fun closeDatabase() {
        database.close()
    }

    @Test
    fun save_routineNeverStored_keepsTheSummarysRoutineName() = runTest {
        val summary = aRunSummary(aRoutine(name = "Morning"))

        repository.save(summary)

        assertEquals(summary, repository.observe(summary.runId).first())
        assertEquals("Morning", database.runDao().observe(summary.runId.value).first()?.routineName)
    }
}
