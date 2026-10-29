package com.ritune.feature.player.ui.screens

import androidx.activity.ComponentActivity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ritune.core.designsystem.theme.RituneTheme
import com.ritune.core.screenshottesting.captureScreenSizes
import com.ritune.core.screenshottesting.captureScreenVariants
import com.ritune.feature.player.ui.screens.player.PlayerContent
import com.ritune.feature.player.ui.screens.player.PlayerUiState
import com.ritune.feature.player.ui.screens.summary.SummaryContent
import com.ritune.feature.player.ui.screens.summary.SummaryUiState
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.GraphicsMode
import java.util.TimeZone
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class PlayerScreensScreenshotTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private val defaultZone = TimeZone.getDefault()

    // The summary shows its start time in the device zone; pin it so goldens match on every machine.
    @Before
    fun pinTimeZone() = TimeZone.setDefault(TimeZone.getTimeZone("UTC"))

    @After
    fun restoreTimeZone() = TimeZone.setDefault(defaultZone)

    private val running = PlayerUiState.Active(
        routineName = "Evening reset",
        stepIndex = 1,
        stepCount = 4,
        stepTitle = "Write tomorrow's top three",
        instruction = "Keep it short. Three things, no more.",
        nextStepTitle = "Light stretch",
        remaining = 134.seconds,
        isPaused = false,
        confirmingStop = false,
    )

    private val done = SummaryUiState.Content(
        routineName = "Evening reset",
        completed = true,
        completedSteps = 4,
        skippedSteps = 0,
        totalSteps = 4,
        duration = 8.minutes + 2.seconds,
        startedAt = Instant.parse("2026-01-05T20:16:00Z"),
    )

    @Composable
    private fun Screen(content: @Composable () -> Unit) {
        RituneTheme {
            Surface(color = MaterialTheme.colorScheme.background) { content() }
        }
    }

    @Test
    fun player_acrossScreenSizes() {
        composeTestRule.captureScreenSizes("player") { Screen { PlayerContent(running, {}) } }
    }

    @Test
    fun player_darkAndLargeFont() {
        composeTestRule.captureScreenVariants("player") { Screen { PlayerContent(running, {}) } }
    }

    @Test
    fun player_pausedOnLastStep() {
        val paused = running.copy(stepIndex = 3, nextStepTitle = null, instruction = null, isPaused = true)
        composeTestRule.captureScreenVariants("player_paused_last") { Screen { PlayerContent(paused, {}) } }
    }

    @Test
    fun summary_acrossScreenSizes() {
        composeTestRule.captureScreenSizes("summary") { Screen { SummaryContent(done, {}) } }
    }

    @Test
    fun summary_darkAndLargeFont() {
        composeTestRule.captureScreenVariants("summary") { Screen { SummaryContent(done, {}) } }
    }

    @Test
    fun summary_stopped() {
        val stopped = done.copy(completed = false, completedSteps = 2)
        composeTestRule.captureScreenVariants("summary_stopped") { Screen { SummaryContent(stopped, {}) } }
    }

    @Test
    fun summary_notFound() {
        composeTestRule.captureScreenVariants("summary_not_found") {
            Screen { SummaryContent(SummaryUiState.NotFound, {}) }
        }
    }
}
