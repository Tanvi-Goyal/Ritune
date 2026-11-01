package com.ritune.benchmark

import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.ExperimentalMacrobenchmarkApi
import androidx.benchmark.macro.ExperimentalMetricApi
import androidx.benchmark.macro.MacrobenchmarkScope
import androidx.benchmark.macro.TraceSectionMetric
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Direction
import com.ritune.core.designsystem.TestTags
import com.ritune.core.designsystem.TraceSections
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Opening the detail and the editor of the seeded 15-step routine. Both screens build every
 * step up front, so the routine is fixed to keep runs comparable.
 */
@RunWith(AndroidJUnit4::class)
class OpenRoutineBenchmark {
    @get:Rule(order = 0)
    val app = SeededAppRule(withLongRoutine = true)

    @get:Rule(order = 1)
    val rule = MacrobenchmarkRule()

    @Test
    fun openDetail() = measure(
        section = TraceSections.ROUTINE_DETAIL_OPEN,
        setup = { showLongRoutineInList() },
    ) {
        device.findObject(By.text(TestTags.LONG_ROUTINE_NAME)).click()
        device.waitForTag(TestTags.ROUTINE_DETAIL_CONTENT)
        device.waitForIdle()
    }

    @Test
    fun openEditor() = measure(
        section = TraceSections.ROUTINE_EDITOR_OPEN,
        setup = {
            showLongRoutineInList()
            device.findObject(By.text(TestTags.LONG_ROUTINE_NAME)).click()
            device.waitForTag(TestTags.ROUTINE_DETAIL_CONTENT)
        },
    ) {
        device.findObject(By.res(TestTags.ROUTINE_EDIT)).click()
        device.waitForTag(TestTags.ROUTINE_EDITOR_CONTENT)
        device.waitForIdle()
    }

    @OptIn(ExperimentalMacrobenchmarkApi::class, ExperimentalMetricApi::class)
    private fun measure(
        section: String,
        setup: MacrobenchmarkScope.() -> Unit,
        open: MacrobenchmarkScope.() -> Unit,
    ) = rule.measureRepeated(
        packageName = PACKAGE,
        metrics = listOf(TraceSectionMetric(section, mode = TraceSectionMetric.Mode.First)),
        compilationMode = CompilationMode.Ignore(),
        startupMode = null,
        iterations = 15,
        setupBlock = setup,
        measureBlock = open,
    )

    // Fresh process, then scroll until the long routine is on screen. It's seeded last, so it
    // sits below the fold; scrolling also works once off-screen rows aren't composed at all.
    // Not `scrollUntil`: it treats a swipe without an accessibility scroll event as the end of
    // the list, and the Compose list sends none, so it stopped after two swipes.
    private fun MacrobenchmarkScope.showLongRoutineInList() {
        killProcess()
        startActivityAndWait()
        device.waitForTag(TestTags.TODAY_CONTENT)
        device.findObject(By.res(TestTags.NAV_ROUTINES)).click()
        device.waitForTag(TestTags.ROUTINES_LIST)
        val list = device.findObject(By.res(TestTags.ROUTINES_LIST))
        list.setGestureMargin(device.displayWidth / 5)
        val routine = By.text(TestTags.LONG_ROUTINE_NAME)
        var swipes = 0
        while (!device.hasObject(routine)) {
            check(swipes++ < MAX_SWIPES) {
                "\"${TestTags.LONG_ROUTINE_NAME}\" not found after $MAX_SWIPES swipes"
            }
            list.scroll(Direction.DOWN, 0.8f)
        }
        device.waitForIdle()
    }

    private companion object {
        const val MAX_SWIPES = 20
    }
}
