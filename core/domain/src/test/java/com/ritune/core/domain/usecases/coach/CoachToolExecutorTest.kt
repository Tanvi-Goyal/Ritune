package com.ritune.core.domain.usecases.coach

import com.ritune.core.domain.usecases.rhythm.ConsistencyCalculator
import com.ritune.core.domain.usecases.rhythm.ObserveCurrentDay
import com.ritune.core.domain.usecases.rhythm.ObserveWeekRhythm
import com.ritune.core.models.DefaultCatalog
import com.ritune.core.models.coach.ToolCall
import com.ritune.core.testing.data.anAnchorEvent
import com.ritune.core.testing.repository.FakeAnchorEventRepository
import com.ritune.core.testing.repository.FakeAnchorRepository
import com.ritune.core.testing.time.FakeTimekeeper
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Test
import kotlin.math.roundToInt
import kotlin.test.assertEquals
import kotlin.time.Instant

class CoachToolExecutorTest {

    // The fake clock's "today" is Monday 2026-01-05, so this week is 2025-12-30..2026-01-05.
    private val timekeeper = FakeTimekeeper()
    private val anchors = FakeAnchorRepository()
    private val events = FakeAnchorEventRepository()
    private val executor = CoachToolExecutor(
        ObserveCurrentDay(timekeeper),
        ObserveWeekRhythm(anchors, events, timekeeper),
    )

    // Wake at 07:00 on four days and at 08:00 on one: 4 of 5 logs are steady.
    private val wakeLogs = listOf(
        "2025-12-31T07:00:00Z",
        "2026-01-01T07:00:00Z",
        "2026-01-02T07:00:00Z",
        "2026-01-03T07:00:00Z",
        "2026-01-04T08:00:00Z",
    ).mapIndexed { i, at -> anAnchorEvent(id = "e$i", occurredAt = Instant.parse(at)) }

    private suspend fun weekRhythm(vararg args: Pair<String, String>): JsonObject {
        val call = ToolCall(id = "1", name = CoachTools.weekRhythm.name, arguments = args.toMap())
        return Json.parseToJsonElement(executor.execute(call).json).jsonObject
    }

    @Test
    fun weekRhythm_returnsTheCalculatorsNumbers() = runTest {
        events.events.value = wakeLogs

        val result = weekRhythm()

        val weekEnd = LocalDate(2026, 1, 5)
        val expected = ConsistencyCalculator.compute(anchors.anchors.value, wakeLogs, weekEnd)
        assertEquals("2025-12-30..2026-01-05", result["week"]!!.jsonPrimitive.content)
        assertEquals(80, result["consistency_percent"]!!.jsonPrimitive.int)
        assertEquals((expected.consistency!! * 100).roundToInt(), result["consistency_percent"]!!.jsonPrimitive.int)
        val wake = result["anchors"]!!.jsonArray
            .map { it.jsonObject }
            .single { it["name"]!!.jsonPrimitive.content == DefaultCatalog.wake.name }
        assertEquals("07:00", wake["usual_time"]!!.jsonPrimitive.content)
        assertEquals("07:00", wake["target_time"]!!.jsonPrimitive.content)
        assertEquals(12, wake["drift_minutes"]!!.jsonPrimitive.int)
        assertEquals(5, wake["logs_this_week"]!!.jsonPrimitive.int)
        assertEquals(7, result["days"]!!.jsonArray.size)
    }

    @Test
    fun weekRhythm_withOffset_readsAnEarlierWeek() = runTest {
        val result = weekRhythm("week_offset" to "1")

        assertEquals("2025-12-23..2025-12-29", result["week"]!!.jsonPrimitive.content)
    }

    @Test
    fun weekRhythm_withoutEnoughData_leavesConsistencyOut() = runTest {
        val result = weekRhythm()

        assertEquals(false, result["has_enough_data"]!!.jsonPrimitive.content.toBoolean())
        assertEquals(null, result["consistency_percent"])
    }

    @Test
    fun weekRhythm_withOffsetOutOfRange_returnsAnError() = runTest {
        val result = weekRhythm("week_offset" to "40")

        assertEquals("week_offset must be 0 to 12", result["error"]!!.jsonPrimitive.content)
    }

    @Test
    fun unknownTool_returnsAnErrorTheModelCanRead() = runTest {
        val result = executor.execute(ToolCall(id = "1", name = "delete_everything"))

        assertEquals("""{"error":"Unknown tool: delete_everything"}""", result.json)
    }
}
