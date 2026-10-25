package com.ritune.core.screenshottesting

import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.test.DeviceConfigurationOverride
import androidx.compose.ui.test.DarkMode
import androidx.compose.ui.test.FontScale
import androidx.compose.ui.test.junit4.AndroidComposeTestRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.then
import androidx.test.ext.junit.rules.ActivityScenarioRule
import com.github.takahirom.roborazzi.captureRoboImage
import org.robolectric.RuntimeEnvironment

typealias ComponentActivityRule =
    AndroidComposeTestRule<ActivityScenarioRule<ComponentActivity>, ComponentActivity>

private const val SCREENSHOT_DIR = "src/test/screenshots"
private const val DENSITY = "420dpi"

private val widthsDp = listOf(400, 610, 900)
private val heightsDp = listOf(400, 500, 1000)

private data class Variant(val suffix: String, val dark: Boolean, val fontScale: Float)

private val variants = listOf(
    Variant("light", dark = false, fontScale = 1f),
    Variant("dark", dark = true, fontScale = 1f),
    Variant("font1.5", dark = false, fontScale = 1.5f),
)

/** Captures the screen at every compact/medium/expanded width and height combination. */
fun ComponentActivityRule.captureScreenSizes(name: String, content: @Composable () -> Unit) {
    for (width in widthsDp) {
        for (height in heightsDp) {
            capture("${name}_w${width}dp_h${height}dp", width, height, Variant("light", false, 1f), content)
        }
    }
}

/** Captures the screen on a phone-sized window in dark mode and at 1.5x font scale. */
fun ComponentActivityRule.captureScreenVariants(name: String, content: @Composable () -> Unit) {
    variants.drop(1).forEach { capture("${name}_${it.suffix}", 400, 500, it, content) }
}

/** Captures a component, wrapped to its own size, in light, dark and 1.5x font scale. */
fun ComponentActivityRule.captureComponent(name: String, content: @Composable () -> Unit) {
    variants.forEach { capture("${name}_${it.suffix}", 400, 500, it, wrap = true, content = content) }
}

private fun ComponentActivityRule.capture(
    fileName: String,
    widthDp: Int,
    heightDp: Int,
    variant: Variant,
    content: @Composable () -> Unit,
    wrap: Boolean = false,
) {
    RuntimeEnvironment.setQualifiers("w${widthDp}dp-h${heightDp}dp-$DENSITY")
    activity.setContent {
        DeviceConfigurationOverride(
            DeviceConfigurationOverride.DarkMode(variant.dark) then
                DeviceConfigurationOverride.FontScale(variant.fontScale),
        ) {
            if (wrap) Box { content() } else content()
        }
    }
    onRoot().captureRoboImage("$SCREENSHOT_DIR/$fileName.png")
}
