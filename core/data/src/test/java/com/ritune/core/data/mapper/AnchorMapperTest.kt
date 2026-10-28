package com.ritune.core.data.mapper

import com.ritune.core.database.entities.AnchorEntity
import com.ritune.core.models.Anchor
import com.ritune.core.models.AnchorId
import kotlinx.datetime.LocalTime
import org.junit.Test
import kotlin.test.assertEquals

class AnchorMapperTest {

    @Test
    fun toEntity_storesTargetAsMinutesAndBack() {
        val anchor = Anchor(AnchorId("wake"), "Wake", LocalTime(7, 30))

        val entity = anchor.toEntity(sortOrder = 4)

        assertEquals(AnchorEntity("wake", "Wake", targetMinutes = 450, sortOrder = 4), entity)
        assertEquals(anchor, entity.toModel())
    }

    @Test
    fun toEntity_withoutTarget_storesNull() {
        val anchor = Anchor(AnchorId("move"), "Move", targetTime = null)

        assertEquals(anchor, anchor.toEntity(sortOrder = 0).toModel())
    }
}
