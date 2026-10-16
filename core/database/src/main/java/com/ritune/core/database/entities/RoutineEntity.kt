package com.ritune.core.database.entities

import androidx.room3.Entity
import androidx.room3.ForeignKey
import androidx.room3.Index
import androidx.room3.PrimaryKey
import com.ritune.core.models.AnchorId
import com.ritune.core.models.Routine
import com.ritune.core.models.RoutineId

@Entity(
    tableName = "routines",
    foreignKeys = [
        ForeignKey(
            entity = AnchorEntity::class,
            parentColumns = ["id"],
            childColumns = ["anchorId"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [Index(value = ["anchorId"])],
)
data class RoutineEntity(
    @PrimaryKey val id: String,
    val name: String,
    val anchorId: String?,
    val sortOrder: Int,
    val builtIn: Boolean,
)