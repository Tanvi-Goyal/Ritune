package com.ritune.core.data.mapper

import com.ritune.core.database.entities.AnchorEntity
import com.ritune.core.models.Anchor
import com.ritune.core.models.AnchorId
import kotlinx.datetime.LocalTime

fun AnchorEntity.toModel() = Anchor(
    id = AnchorId(id),
    name = name,
    targetTime = targetMinutes?.let { LocalTime(it / 60, it % 60) },
)

fun Anchor.toEntity(sortOrder: Int) = AnchorEntity(
    id = id.value,
    name = name,
    targetMinutes = targetTime?.let { it.hour * 60 + it.minute },
    sortOrder = sortOrder,
)
