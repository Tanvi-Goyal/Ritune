package com.ritune.core.designsystem

import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.platform.InspectorInfo
import androidx.tracing.Trace

/**
 * Async trace sections that time how long a screen takes to appear: from the tap that opens it
 * to the first frame that draws its content. The Macrobenchmark journeys read them with
 * TraceSectionMetric, so renaming one breaks those benchmarks.
 */
object TraceSections {
    const val ROUTINES_OPEN = "Routines.open"
    const val RHYTHM_OPEN = "Rhythm.open"

    // One screen opens at a time, so a fixed cookie pairs each begin with its end.
    internal const val COOKIE = 0

    fun beginScreenOpen(section: String) = Trace.beginAsyncSection(section, COOKIE)
}

/**
 * Ends [section] the first time this content is drawn. An end without a begin, such as after
 * a deep link, is ignored by the trace.
 */
fun Modifier.endTraceOnFirstDraw(section: String): Modifier = this then EndTraceElement(section)

private data class EndTraceElement(val section: String) : ModifierNodeElement<EndTraceNode>() {
    override fun create() = EndTraceNode(section)
    override fun update(node: EndTraceNode) {
        node.section = section
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "endTraceOnFirstDraw"
        properties["section"] = section
    }
}

private class EndTraceNode(var section: String) : Modifier.Node(), DrawModifierNode {
    private var ended = false

    override fun ContentDrawScope.draw() {
        drawContent()
        if (!ended) {
            ended = true
            Trace.endAsyncSection(section, TraceSections.COOKIE)
        }
    }
}
