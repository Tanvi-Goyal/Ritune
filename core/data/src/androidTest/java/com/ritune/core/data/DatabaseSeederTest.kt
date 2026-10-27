package com.ritune.core.data

import androidx.room3.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ritune.core.data.repositoriesImpl.AnchorEventRepositoryImpl
import com.ritune.core.data.repositoriesImpl.AnchorRepositoryImpl
import com.ritune.core.data.repositoriesImpl.RoutineRepositoryImpl
import com.ritune.core.database.RituneDatabase
import com.ritune.core.models.DefaultCatalog
import com.ritune.core.testing.data.aRoutine
import com.ritune.core.testing.data.anAnchorEvent
import com.ritune.core.testing.data.testInstant
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.days

@RunWith(AndroidJUnit4::class)
class DatabaseSeederTest {

    private lateinit var database: RituneDatabase
    private lateinit var seeder: DatabaseSeeder
    private lateinit var routines: RoutineRepositoryImpl
    private lateinit var anchors: AnchorRepositoryImpl
    private lateinit var events: AnchorEventRepositoryImpl

    @Before
    fun createDatabase() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            RituneDatabase::class.java,
        ).build()
        seeder = DatabaseSeeder(database)
        routines = RoutineRepositoryImpl(database.routineDao())
        anchors = AnchorRepositoryImpl(database.anchorDao())
        events = AnchorEventRepositoryImpl(database.anchorEventDao())
    }

    @After
    fun closeDatabase() {
        database.close()
    }

    @Test
    fun seedIfEmpty_emptyDatabase_storesCatalogAsBuiltIn() = runTest {
        seeder.seedIfEmpty()

        assertEquals(DefaultCatalog.anchors, anchors.observeAll().first())
        val seeded = routines.observeAll().first()
        assertEquals(DefaultCatalog.routines.map { it.copy(builtIn = true) }, seeded)
    }

    @Test
    fun seedIfEmpty_runTwice_keepsUserChanges() = runTest {
        seeder.seedIfEmpty()
        val removed = DefaultCatalog.routines.first()
        routines.delete(removed.id)

        seeder.seedIfEmpty()

        val ids = routines.observeAll().first().map { it.id }
        assertEquals(DefaultCatalog.routines.size - 1, ids.size)
        assertTrue(removed.id !in ids)
    }

    @Test
    fun resetAll_clearsUserDataAndRestoresDefaults() = runTest {
        seeder.seedIfEmpty()
        routines.delete(DefaultCatalog.routines.first().id)
        routines.save(aRoutine(id = "custom"))
        events.append(anAnchorEvent())

        seeder.resetAll()

        assertEquals(
            DefaultCatalog.routines.map { it.id },
            routines.observeAll().first().map { it.id },
        )
        assertEquals(emptyList(), events.observeBetween(testInstant - 1.days, testInstant + 1.days).first())
    }
}
