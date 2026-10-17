package com.ritune.core.database.entities

import androidx.room3.Entity
import androidx.room3.ForeignKey
import androidx.room3.Index
import androidx.room3.PrimaryKey

/** Append-only fact: this anchor happened at this instant. Corrections are delete + new event. */
@Entity(
    tableName = "anchor_events",
    foreignKeys = [
        ForeignKey(
            entity = AnchorEntity::class,
            parentColumns = ["id"],
            childColumns = ["anchorId"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
    indices = [
        Index(value = ["anchorId", "occurredAt"]),
        Index(value = ["occurredAt"]),
    ],
)
data class AnchorEventEntity(
    @PrimaryKey val id: String,
    val anchorId: String,
    val occurredAt: Long,
    val utcOffsetSeconds: Int,
    val source: String,
)
