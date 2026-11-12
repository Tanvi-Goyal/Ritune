package com.ritune.core.domain.usecases.coach.tools

import com.ritune.core.models.Anchor
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class AnchorsResult(val anchors: List<AnchorItem>)

@Serializable
internal data class AnchorItem(
    val id: String,
    val name: String,
    @SerialName("target_time") val targetTime: String?,
)

internal fun List<Anchor>.toResult() = AnchorsResult(
    map { AnchorItem(it.id.value, it.name, it.targetTime?.toString()) },
)
