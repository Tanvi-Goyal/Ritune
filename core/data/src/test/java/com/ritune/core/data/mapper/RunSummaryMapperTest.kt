package com.ritune.core.data.mapper

import com.ritune.core.models.RunOutcome
import com.ritune.core.testing.data.aRunSummary
import org.junit.Test
import kotlin.test.assertEquals

class RunSummaryMapperTest {

    @Test
    fun toEntity_storesRoutineNameAndBack() {
        val summary = aRunSummary(outcome = RunOutcome.STOPPED, completedSteps = 0, skippedSteps = 1)

        val entity = summary.toEntity(routineName = "Morning")

        assertEquals("Morning", entity.routineName)
        assertEquals("STOPPED", entity.outcome)
        assertEquals(summary, entity.toModel())
    }
}
