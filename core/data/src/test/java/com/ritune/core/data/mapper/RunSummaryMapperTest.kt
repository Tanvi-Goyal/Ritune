package com.ritune.core.data.mapper

import com.ritune.core.models.RunOutcome
import com.ritune.core.testing.data.aRoutine
import com.ritune.core.testing.data.aRunSummary
import org.junit.Test
import kotlin.test.assertEquals

class RunSummaryMapperTest {

    @Test
    fun toEntity_storesRoutineNameAndBack() {
        val summary = aRunSummary(
            aRoutine(name = "Morning"),
            outcome = RunOutcome.STOPPED,
            completedSteps = 0,
            skippedSteps = 1,
        )

        val entity = summary.toEntity()

        assertEquals("Morning", entity.routineName)
        assertEquals("STOPPED", entity.outcome)
        assertEquals(summary, entity.toModel())
    }
}
