package com.ritune.core.testing.evals

import kotlin.math.roundToInt
import kotlin.time.Duration
import kotlin.time.DurationUnit

data class EvalReport(
    val promptVersion: Int,
    val results: List<ScenarioResult>,
    val planned: Int = results.size,
) {
    val passed get() = results.count { it.passed }
    val passRate get() = if (results.isEmpty()) 0.0 else passed.toDouble() / results.size

    private val turnLatencies get() = results.flatMap { it.turns }.map { it.latency }.sorted()

    fun percentile(p: Int): Duration =
        turnLatencies.getOrNull(((turnLatencies.size - 1) * p / 100.0).roundToInt()) ?: Duration.ZERO

    fun toMarkdown(): String = buildString {
        val usage = results.map { it.usage }
        appendLine("Prompt v$promptVersion · ${results.size} scenarios")
        if (results.size < planned) {
            appendLine("Stopped after ${results.size} of $planned: every scenario failed with an error.")
        }
        appendLine()
        appendLine("Passed ${passed}/${results.size} (${(passRate * 100).roundToInt()}%)")
        appendLine("Turn latency p50 ${percentile(50).seconds()} · p95 ${percentile(95).seconds()}")
        appendLine(
            "Tokens in ${usage.sumOf { it.inputTokens }} · out ${usage.sumOf { it.outputTokens }}" +
                " · thinking ${usage.sumOf { it.thinkingTokens }}",
        )
        appendLine()
        appendLine("| Category | Passed |")
        appendLine("|---|---|")
        results.groupBy { it.scenario.category }.forEach { (category, group) ->
            appendLine("| ${category.name.lowercase()} | ${group.count { it.passed }}/${group.size} |")
        }
        appendLine()
        appendLine("| Scenario | Result | Failed checks | Latency | Tokens in/out | Rounds |")
        appendLine("|---|---|---|---|---|---|")
        results.forEach { result ->
            val failed = result.failures.joinToString("; ") { it.name }.ifEmpty { "–" }
            val rounds = result.turns.joinToString("+") { it.record.rounds.toString() }
            appendLine(
                "| ${result.scenario.id} | ${if (result.passed) "pass" else "FAIL"} | $failed" +
                    " | ${result.latency.seconds()} | ${result.usage.inputTokens}/${result.usage.outputTokens}" +
                    " | $rounds |",
            )
        }
    }

    private fun Duration.seconds() = "%.1f s".format(toDouble(DurationUnit.SECONDS))
}

/** One scenario in full: every question, reply, tool call and failed check. For reading failures. */
fun ScenarioResult.transcript(): String = buildString {
    appendLine("${scenario.id}: ${if (passed) "pass" else "FAIL"} (attempt $attempts)")
    turns.forEach { turn ->
        appendLine("  Q: ${turn.turn.question}")
        turn.record.calls.forEach { appendLine("  tool: ${it.name} ${it.arguments}") }
        turn.record.proposals.forEach { appendLine("  proposal: $it → ${turn.turn.decision}") }
        appendLine("  A: ${turn.record.reply.trim().ifEmpty { "(nothing)" }}")
        turn.checks.filterNot { it.passed }.forEach { appendLine("  ✗ ${it.name} ${it.detail}") }
    }
}
