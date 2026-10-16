package com.ritune.core.database.relations

import com.ritune.core.database.entities.StepCueEntity
import com.ritune.core.database.entities.StepEntity

data class StepDraft(
    val step: StepEntity,
    val cues: List<StepCueEntity>,
)