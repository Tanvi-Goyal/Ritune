package com.ritune.core.database.dao

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import androidx.room3.Upsert
import com.ritune.core.database.entities.AnchorEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AnchorDao {

    @Query("SELECT * FROM anchors WHERE archived = 0 ORDER BY sortOrder ASC")
    fun observeActiveAnchors(): Flow<List<AnchorEntity>>

    @Query("SELECT COUNT(*) FROM anchors")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(anchors: List<AnchorEntity>)

    @Upsert
    suspend fun upsert(anchor: AnchorEntity)

    @Query("UPDATE anchors SET archived = 1 WHERE id = :id")
    suspend fun archive(id: String)

    @Query("UPDATE anchors SET targetMinutes = :targetMinutes WHERE id = :id")
    suspend fun updateTarget(id: String, targetMinutes: Int?)
}