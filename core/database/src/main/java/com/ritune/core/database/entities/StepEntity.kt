package com.ritune.core.database.entities

import androidx.room3.Entity
import androidx.room3.ForeignKey
import androidx.room3.Index
import androidx.room3.PrimaryKey

/**
 * A step of a routine. Uses a surrogate [rowId] so domain step ids only need to be unique
 * inside their routine, and so replacing a routine's steps never collides with another routine.
 */
@Entity(
    tableName = "steps",
    foreignKeys = [
        ForeignKey(
            entity = RoutineEntity::class,
            parentColumns = ["id"],
            childColumns = ["routineId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["routineId", "position"], unique = true)],
)
data class StepEntity(
    @PrimaryKey(autoGenerate = true) val rowId: Long = 0,
    val routineId: String,
    val stepId: String,
    val position: Int,
    val title: String,
    val durationMs: Long,
    val instruction: String?,
)