package com.ritune.core.database

import androidx.room3.Database
import androidx.room3.RoomDatabase
import com.ritune.core.database.dao.AnchorDao
import com.ritune.core.database.dao.AnchorEventDao
import com.ritune.core.database.dao.RoutineDao
import com.ritune.core.database.dao.RunDao
import com.ritune.core.database.entities.AnchorEntity
import com.ritune.core.database.entities.AnchorEventEntity
import com.ritune.core.database.entities.RoutineEntity
import com.ritune.core.database.entities.RunEntity
import com.ritune.core.database.entities.StepCueEntity
import com.ritune.core.database.entities.StepEntity

@Database(
    entities = [
        AnchorEntity::class,
        AnchorEventEntity::class,
        RoutineEntity::class,
        StepEntity::class,
        StepCueEntity::class,
        RunEntity::class,
    ],
    version = 1,
)
abstract class RituneDatabase : RoomDatabase() {
    abstract fun anchorDao(): AnchorDao
    abstract fun anchorEventDao(): AnchorEventDao
    abstract fun routineDao(): RoutineDao
    abstract fun runDao(): RunDao
}