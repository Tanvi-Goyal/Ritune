package com.ritune.feature.rhythm.ui.screens

import androidx.activity.ComponentActivity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ritune.core.designsystem.theme.RituneTheme
import com.ritune.core.screenshottesting.captureScreenSizes
import com.ritune.core.screenshottesting.captureScreenVariants
import kotlinx.datetime.LocalDate
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class RhythmScreenScreenshotTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private val weekEnd = LocalDate(2026, 1, 11)
    private val fractions = listOf(0.7f, 1f, null, 0.5f, 0.9f, 0f, 0.6f)

    private val scored = RhythmUiState.Content(
        weekEnd = weekEnd,
        days = fractions.mapIndexed { i, fraction -> DayBar(LocalDate(2026, 1, 5 + i), fraction) },
        summary = RhythmSummary.Scored(consistencyPercent = 82, wakeDriftMinutes = 18, steadiestName = "Lights out"),
    )

    @Composable
    private fun Rhythm(state: RhythmUiState) {
        RituneTheme {
            Surface(color = MaterialTheme.colorScheme.background) {
                RhythmContent(state, onOpenSettings = {})
            }
        }
    }

    @Test
    fun rhythm_acrossScreenSizes() {
        composeTestRule.captureScreenSizes("rhythm") { Rhythm(scored) }
    }

    @Test
    fun rhythm_darkAndLargeFont() {
        composeTestRule.captureScreenVariants("rhythm") { Rhythm(scored) }
    }

    @Test
    fun rhythm_early() {
        composeTestRule.captureScreenVariants("rhythm_early") {
            Rhythm(scored.copy(summary = RhythmSummary.Early(loggedDays = 1, requiredDays = 3)))
        }
    }

    @Test
    fun rhythm_building() {
        composeTestRule.captureScreenVariants("rhythm_building") {
            Rhythm(scored.copy(summary = RhythmSummary.Building))
        }
    }
}
