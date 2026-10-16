package com.ritune.core.database.dao

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import com.ritune.core.database.entities.AnchorEventEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AnchorEventDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(event: AnchorEventEntity): Long

    @Query("DELETE FROM anchor_events WHERE id = :id")
    suspend fun delete(id: String)

    /** Start inclusive, end exclusive, so adjacent days never share an event. */
    @Query("SELECT * FROM anchor_events WHERE occurredAt >= :fromMs AND occurredAt < :toMs ORDER BY occurredAt")
    fun observeBetween(fromMs: Long, toMs: Long): Flow<List<AnchorEventEntity>>

    @Query("SELECT * FROM anchor_events WHERE anchorId = :anchorId ORDER BY occurredAt DESC LIMIT 1")
    suspend fun latest(anchorId: String): AnchorEventEntity?

    @Query("UPDATE anchor_events SET anchorId = :anchorId, occurredAt = :occurredAt, utcOffsetSeconds = :utcOffsetSeconds, source = :source WHERE id = :id")
    suspend fun update(id: String, anchorId: String, occurredAt: Long, utcOffsetSeconds: Int, source: String)

    suspend fun update(id: String, event: AnchorEventEntity) {
        update(
            id = id,
            anchorId = event.anchorId,
            occurredAt = event.occurredAt,
            utcOffsetSeconds = event.utcOffsetSeconds,
            source = event.source,
        )
    }
}