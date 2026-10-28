package com.ritune.core.database.dao

import androidx.room3.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ritune.core.database.RituneDatabase
import com.ritune.core.database.entities.AnchorEntity
import com.ritune.core.database.entities.AnchorEventEntity
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
class AnchorEventDaoTest {

    private lateinit var database: RituneDatabase
    private lateinit var dao: AnchorEventDao

    @Before
    fun createDatabase() = runTest {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            RituneDatabase::class.java,
        ).build()
        dao = database.anchorEventDao()
        database.anchorDao().insertAll(
            listOf("wake", "sleep").mapIndexed { i, id -> AnchorEntity(id, id, null, i) },
        )
    }

    @After
    fun closeDatabase() {
        database.close()
    }

    private fun event(id: String, at: Long, anchorId: String = "wake") =
        AnchorEventEntity(id, anchorId, occurredAt = at, utcOffsetSeconds = 0, source = "MANUAL")

    @Test
    fun observeBetween_includesStartExcludesEndInTimeOrder() = runTest {
        listOf(event("end", 200), event("late", 150), event("start", 100), event("before", 99))
            .forEach { dao.insert(it) }

        assertEquals(listOf("start", "late"), dao.observeBetween(100, 200).first().map { it.id })
    }

    @Test
    fun latest_returnsNewestForThatAnchorOnly() = runTest {
        dao.insert(event("old", 100))
        dao.insert(event("new", 300))
        dao.insert(event("other", 500, anchorId = "sleep"))

        assertEquals("new", dao.latest("wake")?.id)
        assertNull(dao.latest("missing"))
    }

    @Test
    fun update_replacesFieldsAndKeepsId() = runTest {
        dao.insert(event("e1", 100))

        dao.update("e1", event("ignored", 250, anchorId = "sleep").copy(source = "BACKFILL"))

        assertEquals(
            listOf(AnchorEventEntity("e1", "sleep", 250, 0, "BACKFILL")),
            dao.observeBetween(0, 1_000).first(),
        )
    }

    @Test
    fun delete_removesOnlyThatEvent() = runTest {
        dao.insert(event("keep", 100))
        dao.insert(event("drop", 200))

        dao.delete("drop")

        assertEquals(listOf("keep"), dao.observeBetween(0, 1_000).first().map { it.id })
    }

    @Test
    fun insert_unknownAnchor_fails() = runTest {
        assertFails { dao.insert(event("e1", 100, anchorId = "missing")) }
    }
}
