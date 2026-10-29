package com.ritune.core.designsystem.components

import androidx.activity.ComponentActivity
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ritune.core.designsystem.theme.RituneTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

@RunWith(AndroidJUnit4::class)
class ComponentsBehaviorTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private val calls = mutableListOf<String>()

    private fun anchorRow(logged: Boolean) {
        composeTestRule.setContent {
            RituneTheme {
                AnchorRow(
                    name = "Wake",
                    timeText = "7:02",
                    logged = logged,
                    statusDescription = if (logged) "Logged" else "Not logged",
                    actionLabel = "Log",
                    onAction = { calls += "action" },
                    onClick = { calls += "click" },
                )
            }
        }
    }

    @Test
    fun anchorRow_pending_offersActionAndRowClick() {
        anchorRow(logged = false)

        composeTestRule.onNodeWithText("Log").performClick()
        composeTestRule.onNodeWithText("Wake", substring = true)
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "Not logged"))
            .performClick()

        assertEquals(listOf("action", "click"), calls)
    }

    @Test
    fun anchorRow_logged_hidesAction() {
        anchorRow(logged = true)

        composeTestRule.onNodeWithText("Log").assertDoesNotExist()
    }

    @Test
    fun weekBars_mismatchedSizes_fails() {
        assertFailsWith<IllegalArgumentException> {
            composeTestRule.setContent {
                WeekBars(values = listOf(1f, 0.5f), labels = listOf("M"), descriptions = listOf("", ""))
            }
        }
    }
}
