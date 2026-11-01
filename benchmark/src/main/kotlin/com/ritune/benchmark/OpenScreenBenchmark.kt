package com.ritune.benchmark

import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.ExperimentalMacrobenchmarkApi
import androidx.benchmark.macro.ExperimentalMetricApi
import androidx.benchmark.macro.TraceSectionMetric
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.uiautomator.By
import com.ritune.core.designsystem.TestTags
import com.ritune.core.designsystem.TraceSections
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Time from tapping a tab to the first frame that draws its content, read from the
 * app's async trace section. Like ScrollBenchmark, setupBlock kills the app itself, so each
 * open runs in a fresh process.
 */
@RunWith(AndroidJUnit4::class)
class OpenScreenBenchmark {
    @get:Rule(order = 0)
    val app = SeededAppRule()

    @get:Rule(order = 1)
    val rule = MacrobenchmarkRule()

    @Test
    fun openRoutines() = open(
        tab = TestTags.NAV_ROUTINES,
        content = TestTags.ROUTINES_LIST,
        section = TraceSections.ROUTINES_OPEN,
    )

    @OptIn(ExperimentalMacrobenchmarkApi::class, ExperimentalMetricApi::class)
    private fun open(tab: String, content: String, section: String) = rule.measureRepeated(
        packageName = PACKAGE,
        metrics = listOf(
            TraceSectionMetric(
                section,
                mode = TraceSectionMetric.Mode.First
            )
        ),
        compilationMode = CompilationMode.Ignore(),
        startupMode = null,
        iterations = 15,
        setupBlock = {
            killProcess()
            startActivityAndWait()
            device.waitForTag(TestTags.TODAY_CONTENT)
        },
    ) {
        device.findObject(By.res(tab)).click()
        // Keep tracing until the content is drawn, otherwise the section has no end.
        device.waitForTag(content)
        device.waitForIdle()
    }
}
