package com.ritune.feature.today.ui.screens

import androidx.activity.ComponentActivity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ritune.core.designsystem.theme.RituneTheme
import com.ritune.core.screenshottesting.captureScreenSizes
import com.ritune.core.screenshottesting.captureScreenVariants
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class TodayScreenScreenshotTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private val partlyLogged = TodayUiState.Content(
        day = LocalDate(2026, 1, 5),
        anchors = listOf(
            AnchorItem("wake", "Wake", LocalTime(7, 0), LocalTime(6, 52), RoutineRef("morning-start", "Morning start")),
            AnchorItem("daylight", "Morning light", LocalTime(7, 30), LocalTime(7, 41), null),
            AnchorItem("workout", "Move", LocalTime(17, 30), null, null),
            AnchorItem("last-meal", "Last meal", LocalTime(19, 30), null, null),
            AnchorItem("shutdown", "Screens off", LocalTime(21, 30), null, RoutineRef("wind-down", "Wind down")),
            AnchorItem("lights-out", "Lights out", LocalTime(22, 30), null, null),
        ),
        runningRoutineName = "Wind down",
    )

    @Composable
    private fun Today(state: TodayUiState) {
        RituneTheme {
            Surface(color = MaterialTheme.colorScheme.background) {
                TodayContent(state, SnackbarHostState(), onEvent = {}, onOpenSettings = {})
            }
        }
    }

    @Test
    fun today_acrossScreenSizes() {
        composeTestRule.captureScreenSizes("today") { Today(partlyLogged) }
    }

    @Test
    fun today_darkAndLargeFont() {
        composeTestRule.captureScreenVariants("today") { Today(partlyLogged) }
    }

    @Test
    fun today_nothingLogged() {
        val empty = partlyLogged.copy(
            anchors = partlyLogged.anchors.map { it.copy(loggedAt = null) },
            runningRoutineName = null,
        )
        composeTestRule.captureScreenVariants("today_empty") { Today(empty) }
    }
}
