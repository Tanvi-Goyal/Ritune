package com.ritune.core.designsystem.components

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ritune.core.designsystem.theme.RituneTheme
import com.ritune.core.screenshottesting.captureComponent
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.GraphicsMode
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ComponentsScreenshotTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private fun capture(name: String, content: @Composable ColumnScope.() -> Unit) {
        composeTestRule.captureComponent(name) {
            RituneTheme {
                Surface {
                    Column(
                        Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        content = content,
                    )
                }
            }
        }
    }

    @Test
    fun anchorRow_loggedPendingAndWithoutAction() {
        capture("anchor_row") {
            AnchorRow(name = "Wake", timeText = "7:02", logged = true, statusDescription = "Logged")
            AnchorRow(
                name = "Lunch",
                timeText = "usually 12:30",
                logged = false,
                statusDescription = "Not logged",
                actionLabel = "Log",
                onAction = {},
            )
            AnchorRow(name = "Bed", timeText = "usually 23:00", logged = false, statusDescription = "Not logged")
        }
    }

    @Test
    fun consistencyRing_emptyPartialFullAndNoTotal() {
        capture("consistency_ring") {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ConsistencyRing(done = 0, total = 4, description = "")
                ConsistencyRing(done = 3, total = 4, description = "")
                ConsistencyRing(done = 4, total = 4, description = "")
                ConsistencyRing(done = 0, total = 0, description = "")
            }
        }
    }

    @Test
    fun weekBars_missingZeroPartialFullAndHighlighted() {
        capture("week_bars") {
            WeekBars(
                values = listOf(null, 0f, 0.4f, 0.75f, 1f, 1.5f, 0.6f),
                labels = listOf("M", "T", "W", "T", "F", "S", "S"),
                descriptions = List(7) { "" },
                highlightIndex = 6,
            )
        }
    }

    @Test
    fun stepProgress_doneCurrentAndAhead() {
        capture("step_progress") {
            StepProgress(total = 5, currentIndex = 2, description = "")
            StepProgress(total = 5, currentIndex = 0, description = "")
            StepProgress(total = 5, currentIndex = 4, description = "")
        }
    }

    @Test
    fun snackbar_withAndWithoutAction() {
        capture("snackbar") {
            RituneSnackbar(message = "Wake logged at 7:02", actionLabel = "Undo", onAction = {})
            RituneSnackbar(message = "All data deleted.", actionLabel = null, onAction = {})
        }
    }

    @Test
    fun switchRow_onOffAndDisabled() {
        capture("switch_row") {
            SwitchRow(label = "Haptics", supportingText = "A short buzz at each step", checked = true, onCheckedChange = {})
            SwitchRow(label = "Sounds", checked = false, onCheckedChange = {})
            SwitchRow(label = "Speech", supportingText = "Unavailable", checked = true, onCheckedChange = {}, enabled = false)
        }
    }

    @Test
    fun textField_filledEmptyAndError() {
        capture("text_field") {
            RituneTextField(value = "Morning start", onValueChange = {}, label = "Name")
            RituneTextField(value = "", onValueChange = {}, label = "Instruction")
            RituneTextField(
                value = "",
                onValueChange = {},
                label = "Name",
                isError = true,
                supportingText = "Give the routine a name",
            )
        }
    }

    @Test
    fun timerText_minutesAndHours() {
        capture("timer_text") {
            TimerText(remaining = 2.minutes + 14.seconds)
            TimerText(
                remaining = 1.hours + 5.minutes,
                style = MaterialTheme.typography.displayMedium,
            )
        }
    }
}
