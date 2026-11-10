package com.ritune.core.domain.usecases.coach.tools

import com.ritune.core.models.RunOutcome
import com.ritune.core.models.coach.ToolCall
import com.ritune.core.testing.data.aRoutine
import com.ritune.core.testing.data.aRunSummary
import com.ritune.core.testing.data.aStep
import com.ritune.core.testing.repository.FakeRunHistoryRepository
import com.ritune.core.testing.time.FakeTimekeeper
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

class RunHistoryToolTest {

    // The fake clock's "now" is Monday 2026-01-05 08:00 UTC.
    private val runs = FakeRunHistoryRepository()
    private val tool = RunHistoryTool(runs, FakeTimekeeper())
    private val morning = aRoutine(name = "Morning", steps = listOf(aStep(id = "a"), aStep(id = "b")))

    private suspend fun runHistory(vararg args: Pair<String, String>): JsonObject {
        val call = ToolCall(id = "1", name = RunHistoryTool.DEFINITION.name, arguments = args.toMap())
        return Json.parseToJsonElement(tool.run(call)).jsonObject
    }

    @Test
    fun describesEachRunInTheWindow_newestFirst() = runTest {
        runs.save(aRunSummary(morning, runId = "old", startedAt = Instant.parse("2025-12-29T07:00:00Z")))
        runs.save(aRunSummary(morning, runId = "first", startedAt = Instant.parse("2025-12-30T07:00:00Z")))
        val start = Instant.parse("2026-01-05T06:30:00Z")
        runs.save(
            aRunSummary(
                morning,
                runId = "today",
                startedAt = start,
                finishedAt = start + 12.minutes + 30.seconds,
                outcome = RunOutcome.STOPPED,
                completedSteps = 1,
                skippedSteps = 0,
            ),
        )

        val result = runHistory()

        assertEquals(2, result["total_runs"]!!.jsonPrimitive.int)
        val newest = result["runs"]!!.jsonArray.first().jsonObject
        assertEquals("Morning", newest["routine"]!!.jsonPrimitive.content)
        assertEquals("2026-01-05", newest["date"]!!.jsonPrimitive.content)
        assertEquals("06:30", newest["start_time"]!!.jsonPrimitive.content)
        assertEquals("12 min 30 s", newest["duration"]!!.jsonPrimitive.content)
        assertEquals(false, newest["finished"]!!.jsonPrimitive.content.toBoolean())
        assertEquals(1, newest["steps_completed"]!!.jsonPrimitive.int)
        assertEquals(2, newest["steps_total"]!!.jsonPrimitive.int)
    }

    @Test
    fun manyRuns_areCappedButCounted() = runTest {
        repeat(RunHistoryTool.MAX_RUNS + 5) { i ->
            val start = Instant.parse("2026-01-05T00:00:00Z") + i.minutes
            runs.save(aRunSummary(morning, runId = "r$i", startedAt = start))
        }

        val result = runHistory("days" to "1")

        assertEquals(RunHistoryTool.MAX_RUNS + 5, result["total_runs"]!!.jsonPrimitive.int)
        assertEquals(RunHistoryTool.MAX_RUNS, result["runs"]!!.jsonArray.size)
    }
}
