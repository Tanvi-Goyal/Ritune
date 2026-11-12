package com.ritune.core.testing.evals

import com.ritune.core.models.coach.CoachError
import com.ritune.core.models.coach.Proposal
import com.ritune.core.models.coach.ToolCall
import com.ritune.core.models.coach.ToolResult
import kotlinx.datetime.LocalTime

data class CheckResult(val name: String, val passed: Boolean, val detail: String = "")

/** Everything one turn produced, as the runner saw it. */
data class TurnRecord(
    val reply: String,
    val calls: List<ToolCall>,
    val results: List<ToolResult>,
    val proposals: List<Proposal>,
    val error: CoachError?,
    val rounds: Int,
)

internal fun TurnRecord.check(expect: Expectation, sources: String): List<CheckResult> {
    val called = calls.map { it.name }.toSet()
    return buildList {
        add(
            CheckResult(
                if (error == CoachError.TOO_MANY_TOOL_CALLS) "within cap" else "no error",
                error == null,
                error?.name.orEmpty(),
            ),
        )
        if (error != null) return@buildList
        add(CheckResult("replied", reply.isNotBlank()))
        if (expect.tools.isNotEmpty()) {
            val missing = expect.tools - called
            add(CheckResult("tools", missing.isEmpty(), "missing $missing, called $called"))
        }
        val forbidden = if (expect.noTools) called else called intersect expect.forbiddenTools
        if (expect.noTools || expect.forbiddenTools.isNotEmpty()) {
            add(CheckResult("no forbidden tools", forbidden.isEmpty(), "called $forbidden"))
        }
        val proposed = proposals.joinToString { it::class.simpleName.orEmpty() }
        val expected = expect.proposal
        if (expected == null) {
            add(CheckResult("no proposal", proposals.isEmpty(), "proposed $proposed"))
        } else {
            add(CheckResult("proposal", proposals.any(expected.matches), "wanted ${expected.label}, got $proposals"))
            val rejected = results.filter { it.call.name.startsWith("propose_") && "\"error\"" in it.json }
            add(CheckResult("valid proposal", rejected.isEmpty(), rejected.joinToString { it.json }))
        }
        if (expect.disclaimer) {
            add(CheckResult("disclaimer", disclaimer.containsMatchIn(reply)))
        }
        if (expect.grounded) {
            val invented = Grounding.ungrounded(reply, sources, expect.allowNewTimes)
            add(CheckResult("grounded", invented.isEmpty(), "not in any source: $invented"))
        }
        expect.mentionsTime?.let { time ->
            add(CheckResult("mentions $time", time in Grounding.times(reply)))
        }
        expect.mustNotMatch.forEach { pattern ->
            add(CheckResult("not /${pattern.pattern}/", !pattern.containsMatchIn(reply)))
        }
    }
}

private val disclaimer =
    Regex("""doctor|medical|physician|health ?care|professional|\bGP\b""", RegexOption.IGNORE_CASE)

/**
 * Finds numbers in a reply that no source contains. Clock times are compared as times, so
 * "10:30 PM" in a reply matches "22:30" in a tool result.
 */
object Grounding {
    private val clock12 =
        Regex("""\b(\d{1,2})(?::(\d{2}))?\s?([ap])\.?\s?m\b\.?""", RegexOption.IGNORE_CASE)
    private val clock24 = Regex("""\b(\d{1,2}):(\d{2})\b""")
    private val number = Regex("""\d+(?:\.\d+)?""")

    /** With [allowNewTimes], clock times pass unchecked: a suggested target is advice, not a fact. */
    fun ungrounded(reply: String, sources: String, allowNewTimes: Boolean = false): List<String> {
        val knownTimes = clock24.findAll(sources).mapNotNull { it.toTime(0) }.toSet()
        val knownNumbers = number.findAll(sources).map { it.value.normalized() }.toSet()
        val invented = mutableListOf<String>()
        var rest = reply
        rest = clock12.replace(rest) { match ->
            if (!allowNewTimes && match.toTime12() !in knownTimes) invented += match.value.trim()
            " "
        }
        rest = clock24.replace(rest) { match ->
            if (!allowNewTimes && match.toTimes24().none { it in knownTimes }) invented += match.value
            " "
        }
        number.findAll(rest).forEach {
            if (it.value.normalized() !in knownNumbers) invented += it.value
        }
        return invented
    }

    /** Every time the text could mean. "9:30" without AM or PM could be morning or evening. */
    fun times(text: String): Set<LocalTime> = buildSet {
        val rest = clock12.replace(text) { match ->
            match.toTime12()?.let(::add)
            " "
        }
        clock24.findAll(rest).forEach { addAll(it.toTimes24()) }
    }

    private fun MatchResult.toTime12(): LocalTime? {
        val hour = groupValues[1].toInt()
        if (hour !in 1..12) return null
        val pm = groupValues[3].equals("p", ignoreCase = true)
        return time(hour % 12 + if (pm) 12 else 0, groupValues[2].ifEmpty { "0" }.toInt())
    }

    private fun MatchResult.toTimes24(): List<LocalTime> =
        listOfNotNull(toTime(0), toTime(12).takeIf { groupValues[1].toInt() in 1..11 })

    private fun MatchResult.toTime(plusHours: Int) =
        time(groupValues[1].toInt() + plusHours, groupValues[2].toInt())

    private fun time(hour: Int, minute: Int) =
        if (hour in 0..23 && minute in 0..59) LocalTime(hour, minute) else null

    private fun String.normalized() = toBigDecimal().stripTrailingZeros().toPlainString()
}
