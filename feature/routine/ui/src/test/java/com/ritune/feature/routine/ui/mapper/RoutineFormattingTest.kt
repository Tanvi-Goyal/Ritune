package com.ritune.feature.routine.ui.mapper

import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

@RunWith(AndroidJUnit4::class)
class RoutineFormattingTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private fun format(block: @Composable () -> String): String {
        var result = ""
        composeTestRule.setContent { result = block() }
        return result
    }

    @Test
    fun formatDuration_picksSecondsMinutesOrBoth() {
        val formatted = format {
            "${formatDuration(45.seconds)}|${formatDuration(8.minutes)}|${formatDuration(8.minutes + 30.seconds)}"
        }

        assertEquals("45 sec|8 min|8 min 30 sec", formatted)
    }

    @Test
    fun routineSummary_joinsDurationStepsAndAnchor() {
        assertEquals("8 min · 4 steps · Starts Wake", format { routineSummary(8.minutes, 4, "Starts Wake") })
    }

    @Test
    fun routineSummary_withoutAnchor_skipsIt() {
        assertEquals("45 sec · 1 step", format { routineSummary(45.seconds, 1, null) })
    }
}
