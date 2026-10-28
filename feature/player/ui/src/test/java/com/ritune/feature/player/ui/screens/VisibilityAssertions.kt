package com.ritune.feature.player.ui.screens

import androidx.compose.ui.test.SemanticsNodeInteraction
import kotlin.test.assertEquals

/** Fails when any part of the node is clipped, for example cut off by a scroll viewport. */
internal fun assertFullyVisible(node: SemanticsNodeInteraction) {
    val semantics = node.fetchSemanticsNode()
    assertEquals(
        semantics.size.height.toFloat(),
        semantics.boundsInRoot.height,
        "visible height of ${semantics.boundsInRoot} differs from full height ${semantics.size.height}",
    )
}
