package com.ritune.core.data.mapper

import com.ritune.core.database.entities.AnchorEventEntity
import com.ritune.core.models.EventSource
import com.ritune.core.testing.data.anAnchorEvent
import com.ritune.core.testing.data.testInstant
import org.junit.Test
import kotlin.test.assertEquals

class AnchorEventMapperTest {

    @Test
    fun toEntity_storesEpochMillisAndSourceNameAndBack() {
        val event = anAnchorEvent(utcOffsetSeconds = 19_800, source = EventSource.BACKFILL)

        val entity = event.toEntity()

        assertEquals(
            AnchorEventEntity("event-1", "wake", testInstant.toEpochMilliseconds(), 19_800, "BACKFILL"),
            entity,
        )
        assertEquals(event, entity.toModel())
    }
}
