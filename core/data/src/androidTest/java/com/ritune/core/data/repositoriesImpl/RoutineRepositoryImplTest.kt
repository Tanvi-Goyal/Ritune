package com.ritune.core.data.repositoriesImpl

import androidx.room3.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ritune.core.data.DatabaseSeeder
import com.ritune.core.database.RituneDatabase
import com.ritune.core.database.entities.RoutineEntity
import com.ritune.core.models.DefaultCatalog
import com.ritune.core.models.RoutineId
import com.ritune.core.testing.data.aRoutine
import com.ritune.core.testing.data.aStep
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

@RunWith(AndroidJUnit4::class)
class RoutineRepositoryImplTest {

    private lateinit var database: RituneDatabase
    private lateinit var repository: RoutineRepositoryImpl

    @Before
    fun createDatabase() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            RituneDatabase::class.java,
        ).build()
        repository = RoutineRepositoryImpl(database.routineDao())
    }

    @After
    fun closeDatabase() {
        database.close()
    }

    @Test
    fun save_newRoutine_goesAfterExistingOnes() = runTest {
        DatabaseSeeder(database).seedIfEmpty()

        repository.save(aRoutine(id = "custom"))

        assertEquals(
            DefaultCatalog.routines.map { it.id } + RoutineId("custom"),
            repository.observeAll().first().map { it.id },
        )
    }

    @Test
    fun save_editedBuiltIn_keepsPositionAndBuiltIn() = runTest {
        DatabaseSeeder(database).seedIfEmpty()
        val original = DefaultCatalog.routines.first()

        repository.save(original.copy(name = "Renamed", builtIn = false, steps = listOf(aStep())))

        val routines = repository.observeAll().first()
        val saved = routines.first()
        assertEquals(original.id, saved.id)
        assertEquals("Renamed", saved.name)
        assertEquals(listOf(aStep()), saved.steps)
        assertTrue(saved.builtIn)
    }

    @Test
    fun observeAll_routineWithoutSteps_isHidden() = runTest {
        database.routineDao().replace(RoutineEntity("empty", "Empty", null, 0, builtIn = false), emptyList())
        repository.save(aRoutine(id = "full"))

        assertEquals(listOf(RoutineId("full")), repository.observeAll().first().map { it.id })
        assertNull(repository.get(RoutineId("empty")))
    }
}
