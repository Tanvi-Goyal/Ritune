package com.ritune.benchmark

import android.os.Build
import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.ExperimentalMacrobenchmarkApi
import androidx.benchmark.macro.ExperimentalMetricApi
import androidx.benchmark.macro.MacrobenchmarkScope
import androidx.benchmark.macro.Metric
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
 * Opening the detail, the editor and the player of the seeded 15-step routine, and moving through
 * its steps. Detail and editor build every step up front, so the routine is fixed to keep runs
 * comparable.
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
        setup = { showLongRoutineDetail() },
    ) {
        device.findObject(By.res(TestTags.ROUTINE_EDIT)).click()
        device.waitForTag(TestTags.ROUTINE_EDITOR_CONTENT)
        device.waitForIdle()
    }

    // The player lives in memory, so killing the app in setup ends the previous iteration's run
    // and every tap starts a new one.
    @Test
    fun openPlayer() = measure(
        section = TraceSections.PLAYER_OPEN,
        setup = {
            grantNotifications()
            showLongRoutineDetail()
        },
    ) {
        device.findObject(By.res(TestTags.ROUTINE_START)).click()
        device.waitForTag(TestTags.PLAYER_CONTENT)
        device.waitForIdle()
    }

    // Each Skip changes the player state once, and the service updates its notification for it.
    // Steps last 60 s, so no step ends on its own during the journey: Count should be SKIPS.
    @OptIn(ExperimentalMetricApi::class)
    @Test
    fun skipSteps() = measure(
        metrics = listOf(
            TraceSectionMetric.Mode.Average,
            TraceSectionMetric.Mode.Count,
            TraceSectionMetric.Mode.Max,
        ).map { TraceSectionMetric("Player.notify", it) },
        setup = {
            grantNotifications()
            showLongRoutineDetail()
            device.findObject(By.res(TestTags.ROUTINE_START)).click()
            device.waitForTag(TestTags.PLAYER_CONTENT)
            device.waitForIdle()
        },
    ) {
        for (step in 2..SKIPS + 1) {
            device.findObject(By.res(TestTags.PLAYER_SKIP)).click()
            device.waitForText("Step $step")
        }
        device.waitForIdle()
    }

    @OptIn(ExperimentalMacrobenchmarkApi::class, ExperimentalMetricApi::class)
    private fun measure(
        section: String,
        setup: MacrobenchmarkScope.() -> Unit,
        open: MacrobenchmarkScope.() -> Unit,
    ) = measure(listOf(TraceSectionMetric(section, TraceSectionMetric.Mode.First)), setup, open)

    @OptIn(ExperimentalMacrobenchmarkApi::class)
    private fun measure(
        metrics: List<Metric>,
        setup: MacrobenchmarkScope.() -> Unit,
        open: MacrobenchmarkScope.() -> Unit,
    ) = rule.measureRepeated(
        packageName = PACKAGE,
        metrics = metrics,
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

    // Android 13+ would otherwise cover the player with the permission dialog.
    private fun MacrobenchmarkScope.grantNotifications() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            device.executeShellCommand("pm grant $PACKAGE android.permission.POST_NOTIFICATIONS")
        }
    }

    private fun MacrobenchmarkScope.showLongRoutineDetail() {
        showLongRoutineInList()
        device.findObject(By.text(TestTags.LONG_ROUTINE_NAME)).click()
        device.waitForTag(TestTags.ROUTINE_DETAIL_CONTENT)
    }

    private companion object {
        const val MAX_SWIPES = 20
        const val SKIPS = 5
    }
}
