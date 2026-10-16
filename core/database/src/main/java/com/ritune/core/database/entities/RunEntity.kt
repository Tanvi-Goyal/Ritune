package com.ritune.core.database.entities

import androidx.room3.Entity
import androidx.room3.Index
import androidx.room3.PrimaryKey

@Entity(
    tableName = "runs",
    indices = [Index(value = ["startedAt"]), Index(value = ["routineId"])],
)
data class RunEntity(
    @PrimaryKey val runId: String,
    val routineId: String,
    val routineName: String,
    val startedAt: Long,
    val finishedAt: Long,
    val outcome: String,
    val completedSteps: Int,
    val skippedSteps: Int,
    val totalSteps: Int,
)
