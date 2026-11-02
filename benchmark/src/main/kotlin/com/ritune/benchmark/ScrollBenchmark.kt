package com.ritune.benchmark

import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.ExperimentalMacrobenchmarkApi
import androidx.benchmark.macro.FrameTimingMetric
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Direction
import com.ritune.core.designsystem.TestTags
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * First scroll after opening the app: a fresh process each iteration, with shaders cached.
 *
 * Not StartupMode.COLD: it runs setupBlock *before* killing the app, so the measure block
 * would find nothing on screen. COLD also clears the shader cache, and the frames would
 * mostly measure shader compiling. Instead, setupBlock kills the app itself.
 */
@RunWith(AndroidJUnit4::class)
class ScrollBenchmark {
    @get:Rule(order = 0)
    val app = SeededAppRule()

    @get:Rule(order = 1)
    val rule = MacrobenchmarkRule()

    // Rhythm isn't here: it shows one week and fits on screen, so a fling draws no frames.
    @Test
    fun scrollRoutines() = scroll(tab = TestTags.NAV_ROUTINES, list = TestTags.ROUTINES_LIST)

    @OptIn(ExperimentalMacrobenchmarkApi::class)
    private fun scroll(tab: String, list: String) = rule.measureRepeated(
        packageName = PACKAGE,
        metrics = listOf(FrameTimingMetric()),
        compilationMode = CompilationMode.Ignore(),
        startupMode = null,
        iterations = 15,
        setupBlock = {
            killProcess()
            startActivityAndWait()
            device.waitForTag(TestTags.TODAY_CONTENT)
            device.findObject(By.res(tab)).click()
            device.waitForTag(list)
        },
    ) {
        val content = device.findObject(By.res(list))
        // Keeps the swipe away from the screen edges, where gesture navigation would take it.
        content.setGestureMargin(device.displayWidth / 5)
        content.fling(Direction.DOWN)
        device.waitForIdle()
        content.fling(Direction.UP)
        device.waitForIdle()
    }
}
