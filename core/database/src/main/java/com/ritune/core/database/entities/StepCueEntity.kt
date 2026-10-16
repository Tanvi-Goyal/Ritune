package com.ritune.core.database.entities

import androidx.room3.Entity
import androidx.room3.ForeignKey
import androidx.room3.Index
import androidx.room3.PrimaryKey

@Entity(
    tableName = "step_cues",
    foreignKeys = [
        ForeignKey(
            entity = StepEntity::class,
            parentColumns = ["rowId"],
            childColumns = ["stepRowId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["stepRowId", "position"], unique = true)],
)
data class StepCueEntity(
    @PrimaryKey(autoGenerate = true) val rowId: Long = 0,
    val stepRowId: Long,
    val position: Int,
    val type: String,
    val value: String,
)

object CueType {
    const val HAPTIC = "HAPTIC"
    const val SOUND = "SOUND"
    const val SPEECH = "SPEECH"
}