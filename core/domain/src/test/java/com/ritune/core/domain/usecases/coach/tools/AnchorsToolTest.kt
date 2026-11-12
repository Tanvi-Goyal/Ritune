package com.ritune.core.domain.usecases.coach.tools

import com.ritune.core.domain.usecases.anchor.ObserveAnchors
import com.ritune.core.models.DefaultCatalog
import com.ritune.core.models.coach.ToolCall
import com.ritune.core.testing.repository.FakeAnchorRepository
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.test.assertTrue

class AnchorsToolTest {

    private val anchors = FakeAnchorRepository()
    private val tool = AnchorsTool(ObserveAnchors(anchors))

    @Test
    fun listsEachAnchorWithIdAndTarget_andLeavesOutMissingTargets() = runTest {
        anchors.updateTarget(DefaultCatalog.lightsOut.id, null)

        val json = tool.run(ToolCall(id = "1", name = AnchorsTool.DEFINITION.name))

        assertTrue("""{"id":"wake","name":"Wake","target_time":"07:00"}""" in json)
        assertTrue("""{"id":"lights-out","name":"Lights out"}""" in json)
    }
}
