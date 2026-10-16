package com.ritune.core.database.dao

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.Query
import androidx.room3.Transaction
import androidx.room3.Upsert
import com.ritune.core.database.entities.RoutineEntity
import com.ritune.core.database.entities.StepCueEntity
import com.ritune.core.database.entities.StepEntity
import com.ritune.core.database.relations.RoutineWithSteps
import com.ritune.core.database.relations.StepDraft
import kotlinx.coroutines.flow.Flow

@Dao
abstract class RoutineDao {
    @Transaction
    @Query("SELECT * FROM routines ORDER BY sortOrder")
    abstract fun observeAll(): Flow<List<RoutineWithSteps>>

    @Transaction
    @Query("SELECT * FROM routines WHERE id = :id")
    abstract suspend fun get(id: String): RoutineWithSteps?

    @Query("SELECT name FROM routines WHERE id = :id")
    abstract suspend fun nameOf(id: String): String?

    @Query("SELECT COUNT(*) FROM routines")
    abstract suspend fun count(): Int

    @Query("DELETE FROM routines WHERE id = :id")
    abstract suspend fun delete(id: String)

    @Upsert
    protected abstract suspend fun upsertRoutine(routine: RoutineEntity)

    @Query("DELETE FROM steps WHERE routineId = :routineId")
    protected abstract suspend fun deleteSteps(routineId: String)

    @Insert
    protected abstract suspend fun insertStep(step: StepEntity): Long

    @Insert
    protected abstract suspend fun insertCues(cues: List<StepCueEntity>)

    @Transaction
    open suspend fun replace(routine: RoutineEntity, steps: List<StepDraft>) {
        upsertRoutine(routine)
        deleteSteps(routine.id)
        for ((step, cues) in steps) {
            val rowId = insertStep(step)
            insertCues(cues.map { it.copy(stepRowId = rowId) })
        }
    }
}