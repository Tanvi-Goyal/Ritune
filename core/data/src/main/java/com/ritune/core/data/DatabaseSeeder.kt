package com.ritune.core.data

import androidx.room3.withWriteTransaction
import com.ritune.core.data.mapper.toDrafts
import com.ritune.core.data.mapper.toEntity
import com.ritune.core.database.RituneDatabase
import com.ritune.core.domain.initialization.AppInitializer
import com.ritune.core.models.DefaultCatalog
import javax.inject.Inject

class DatabaseSeeder @Inject constructor(
    private val db: RituneDatabase
) : AppInitializer {

    override suspend fun initialize() = seedIfEmpty()

    suspend fun seedIfEmpty() {
        db.withWriteTransaction {
            if (db.anchorDao().count() == 0) {
                db.anchorDao().insertAll(DefaultCatalog.anchors.mapIndexed { i, anchor ->
                    anchor.toEntity(i)
                })
            }
            if (db.routineDao().count() == 0) {
                listOf(DefaultCatalog.eveningReset).forEachIndexed { i, routine ->
                    db.routineDao().replace(
                        routine.toEntity(sortOrder = i, builtIn = true),
                        routine.toDrafts()
                    )
                }
            }
        }
    }

    /** "Delete all data" (S-3): wipes every table, then restores the defaults. */
    suspend fun resetAll() {
        db.clearAllTables()
        seedIfEmpty()
    }
}