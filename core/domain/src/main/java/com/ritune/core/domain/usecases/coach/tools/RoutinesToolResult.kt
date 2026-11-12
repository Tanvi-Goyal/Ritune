package com.ritune.core.domain.usecases.coach.tools

import com.ritune.core.models.AnchorId
import com.ritune.core.models.Routine
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class RoutinesResult(val routines: List<RoutineItem>)

@Serializable
internal data class RoutineItem(
    val name: String,
    val anchor: String?,
    @SerialName("total_duration") val totalDuration: String,
    @SerialName("built_in") val builtIn: Boolean,
    val steps: List<StepItem>,
)

@Serializable
internal data class StepItem(val title: String, val duration: String)

internal fun List<Routine>.toResult(anchorNames: Map<AnchorId, String>) = RoutinesResult(
    map { routine ->
        RoutineItem(
            name = routine.name,
            anchor = routine.anchorId?.let(anchorNames::get),
            totalDuration = routine.totalDuration.toToolText(),
            builtIn = routine.builtIn,
            steps = routine.steps.map { StepItem(it.title, it.duration.toToolText()) },
        )
    },
)
