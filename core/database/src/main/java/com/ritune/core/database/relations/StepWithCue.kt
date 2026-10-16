package com.ritune.core.database.relations

import androidx.room3.Embedded
import androidx.room3.Relation
import com.ritune.core.database.entities.CueType
import com.ritune.core.database.entities.StepCueEntity
import com.ritune.core.database.entities.StepEntity
import com.ritune.core.models.Cue
import com.ritune.core.models.HapticPattern
import com.ritune.core.models.SoundCue
import com.ritune.core.models.Step
import com.ritune.core.models.StepId
import kotlin.time.Duration.Companion.milliseconds

data class StepWithCues(
    @Embedded val step: StepEntity,
    @Relation(parentColumns = ["rowId"], entityColumns = ["stepRowId"])
    val cues: List<StepCueEntity>,
)