package com.ritune.core.database.dao

import androidx.room3.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ritune.core.database.RituneDatabase
import com.ritune.core.database.entities.AnchorEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals
import kotlin.test.assertNull

@RunWith(AndroidJUnit4::class)
class AnchorDaoTest {

    private lateinit var database: RituneDatabase
    private lateinit var dao: AnchorDao

    @Before
    fun createDatabase() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            RituneDatabase::class.java,
        ).build()
        dao = database.anchorDao()
    }

    @After
    fun closeDatabase() {
        database.close()
    }

    private fun anchor(id: String, sortOrder: Int, archived: Boolean = false) =
        AnchorEntity(id, name = "Anchor $id", targetMinutes = 420, sortOrder = sortOrder, archived = archived)

    @Test
    fun observeActiveAnchors_skipsArchivedAndOrdersBySortOrder() = runTest {
        dao.insertAll(listOf(anchor("b", 1), anchor("archived", 0, archived = true), anchor("a", 0)))

        assertEquals(listOf("a", "b"), dao.observeActiveAnchors().first().map { it.id })
    }

    @Test
    fun insertAll_existingId_keepsOriginal() = runTest {
        dao.insertAll(listOf(anchor("a", 0)))

        dao.insertAll(listOf(anchor("a", 5).copy(name = "Renamed")))

        assertEquals(listOf(anchor("a", 0)), dao.observeActiveAnchors().first())
        assertEquals(1, dao.count())
    }

    @Test
    fun updateTarget_changesOnlyThatAnchor() = runTest {
        dao.insertAll(listOf(anchor("a", 0), anchor("b", 1)))

        dao.updateTarget("a", targetMinutes = null)

        val (a, b) = dao.observeActiveAnchors().first()
        assertNull(a.targetMinutes)
        assertEquals(420, b.targetMinutes)
    }
}
