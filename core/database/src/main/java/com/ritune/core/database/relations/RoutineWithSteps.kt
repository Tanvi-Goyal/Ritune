package com.ritune.core.database.relations

import androidx.room3.Embedded
import androidx.room3.Relation
import com.ritune.core.database.entities.RoutineEntity
import com.ritune.core.database.entities.StepEntity
import com.ritune.core.models.AnchorId
import com.ritune.core.models.Routine
import com.ritune.core.models.RoutineId

data class RoutineWithSteps(
    @Embedded val routine: RoutineEntity,
    @Relation(
        entity = StepEntity::class,
        parentColumns = ["id"],
        entityColumns = ["routineId"]
    )
    val steps: List<StepWithCues>,
)

