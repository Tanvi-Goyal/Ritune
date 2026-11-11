package com.ritune.core.domain.usecases.coach.tools

import com.ritune.core.domain.usecases.anchor.ObserveAnchors
import com.ritune.core.models.DefaultCatalog
import com.ritune.core.models.coach.ToolCall
import com.ritune.core.testing.data.anAnchorEvent
import com.ritune.core.testing.repository.FakeAnchorEventRepository
import com.ritune.core.testing.repository.FakeAnchorRepository
import com.ritune.core.testing.time.FakeTimekeeper
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.time.Instant

class AnchorEventsToolTest {

    // The fake clock's "now" is Monday 2026-01-05 08:00 UTC.
    private val events = FakeAnchorEventRepository()
    private val tool =
        AnchorEventsTool(ObserveAnchors(FakeAnchorRepository()), events, FakeTimekeeper())
    private val wakeId = DefaultCatalog.wake.id.value

    private suspend fun anchorEvents(vararg args: Pair<String, String>): JsonObject {
        val call =
            ToolCall(id = "1", name = AnchorEventsTool.DEFINITION.name, arguments = args.toMap())
        return Json.parseToJsonElement(tool.run(call)).jsonObject
    }

    private fun JsonObject.logs() = this["logs"]!!.jsonArray.map {
        val log = it.jsonObject
        "${log["date"]!!.jsonPrimitive.content} ${log["time"]!!.jsonPrimitive.content}"
    }

    @Test
    fun eachLog_carriesItsIdForCorrections() = runTest {
        events.events.value = listOf(
            anAnchorEvent(
                id = "log-1",
                occurredAt = Instant.parse("2026-01-05T06:55:00Z")
            )
        )

        val log = anchorEvents("anchor_id" to wakeId)["logs"]!!.jsonArray.single().jsonObject

        assertEquals("log-1", log["id"]!!.jsonPrimitive.content)
    }

    @Test
    fun returnsOnlyThatAnchorsLogsInTheWindow_oldestFirst() = runTest {
        events.events.value = listOf(
            anAnchorEvent(id = "today", occurredAt = Instant.parse("2026-01-05T06:55:00Z")),
            anAnchorEvent(id = "first-day", occurredAt = Instant.parse("2025-12-30T07:10:00Z")),
            anAnchorEvent(id = "too-old", occurredAt = Instant.parse("2025-12-29T07:00:00Z")),
            anAnchorEvent(
                id = "other-anchor",
                anchorId = DefaultCatalog.anchors.last().id,
                occurredAt = Instant.parse("2026-01-04T22:00:00Z"),
            ),
        )

        val result = anchorEvents("anchor_id" to wakeId)

        assertEquals(DefaultCatalog.wake.name, result["anchor"]!!.jsonPrimitive.content)
        assertEquals(listOf("2025-12-30 07:10", "2026-01-05 06:55"), result.logs())
    }

    @Test
    fun logsShowTheTimeZoneTheyWereLoggedIn() = runTest {
        events.events.value = listOf(
            anAnchorEvent(
                occurredAt = Instant.parse("2026-01-04T23:30:00Z"),
                utcOffsetSeconds = 5 * 3600 + 1800
            ),
        )

        val result = anchorEvents("anchor_id" to wakeId, "days" to "2")

        assertEquals(listOf("2026-01-05 05:00"), result.logs())
    }

    @Test
    fun unknownAnchor_pointsTheModelToGetAnchors() = runTest {
        val error = assertFailsWith<ToolArgumentException> { anchorEvents("anchor_id" to "nope") }

        assertEquals("Unknown anchor_id. Call get_anchors for the valid ids.", error.message)
    }

    @Test
    fun daysOutOfRange_isRejected() = runTest {
        val error = assertFailsWith<ToolArgumentException> {
            anchorEvents("anchor_id" to wakeId, "days" to "0")
        }

        assertEquals("days must be 1 to 60", error.message)
    }
}
