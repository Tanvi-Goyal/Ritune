package com.ritune

import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText

private val inBottomBar = hasAnyAncestor(hasTestTag("bottomBar"))

fun bottomBarItem(label: String): SemanticsMatcher = hasText(label) and inBottomBar

fun screenText(text: String, substring: Boolean = false): SemanticsMatcher =
    hasText(text, substring = substring) and !inBottomBar
