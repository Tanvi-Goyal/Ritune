package com.ritune.core.database.entities

import androidx.room3.Entity
import androidx.room3.PrimaryKey

@Entity(
    tableName = "anchors"
)
data class AnchorEntity(
    @PrimaryKey val id: String,
    val name: String,
    val targetMinutes: Int?,
    val sortOrder: Int,
    val archived: Boolean = false,
)