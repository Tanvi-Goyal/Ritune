package com.ritune.core.database.dao

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import com.ritune.core.database.entities.RunEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RunDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun save(run: RunEntity)

    @Query("SELECT * FROM runs ORDER BY startedAt DESC LIMIT :limit")
    fun observeRecent(limit: Int): Flow<List<RunEntity>>

    @Query("SELECT * FROM runs WHERE startedAt >= :fromMs ORDER BY startedAt DESC")
    fun observeSince(fromMs: Long): Flow<List<RunEntity>>

    @Query("SELECT * FROM runs WHERE runId = :id")
    fun observe(id: String): Flow<RunEntity?>
}