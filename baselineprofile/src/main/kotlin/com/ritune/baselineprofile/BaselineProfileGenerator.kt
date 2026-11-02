package com.ritune.baselineprofile

import androidx.benchmark.macro.MacrobenchmarkScope
import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Direction
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import com.ritune.core.designsystem.TestTags
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

private const val PACKAGE = "com.ritune"

/**
 * Records the code each journey runs, so it ships precompiled.
 * Run with `./gradlew :app:generateBaselineProfile`; it writes app/src/main/generated/baselineProfiles.
 *
 * It drives the release build the way a user does, with no seeding: SeedActivity only exists in
 * the benchmark build type. Logging anchors on Today creates the data the other screens show.
 * Button labels are matched by their English text, so the emulator must use an English locale.
 */
@RunWith(AndroidJUnit4::class)
class BaselineProfileGenerator {
    @get:Rule
    val rule = BaselineProfileRule()

    /** Startup only: these rules also decide which classes go first in the primary dex. */
    @Test
    fun startup() = rule.collect(packageName = PACKAGE, includeInStartupProfile = true) {
        pressHome()
        startActivityAndWait()
        device.waitForTag(TestTags.TODAY_CONTENT)
    }

    @Test
    fun journeys() = rule.collect(packageName = PACKAGE) {
        // API 33+ would otherwise cover the player with the permission dialog.
        device.executeShellCommand("pm grant $PACKAGE android.permission.POST_NOTIFICATIONS")
        pressHome()
        startActivityAndWait()
        device.waitForTag(TestTags.TODAY_CONTENT)

        logAnchor()
        openRhythm()
        openRoutinesAndScroll()
        openDetailAndEditor()
        startAndStopRoutine()
        openSettings()
    }

    private fun MacrobenchmarkScope.logAnchor() {
        // Each iteration logs the next anchor; once all are logged there's nothing left to tap.
        device.findObject(By.text("Log"))?.click()
        device.waitForIdle()
    }

    private fun MacrobenchmarkScope.openRhythm() {
        device.findObject(By.res(TestTags.NAV_RHYTHM)).click()
        device.waitForTag(TestTags.RHYTHM_CONTENT)
    }

    private fun MacrobenchmarkScope.openRoutinesAndScroll() {
        device.findObject(By.res(TestTags.NAV_ROUTINES)).click()
        device.waitForTag(TestTags.ROUTINES_LIST)
        val list = device.findObject(By.res(TestTags.ROUTINES_LIST))
        list.setGestureMargin(device.displayWidth / 5)
        list.fling(Direction.DOWN)
        device.waitForIdle()
        list.fling(Direction.UP)
        device.waitForIdle()
    }

    private fun MacrobenchmarkScope.openDetailAndEditor() {
        device.findObject(By.res(TestTags.ROUTINE_ROW)).click()
        device.waitForTag(TestTags.ROUTINE_DETAIL_CONTENT)
        device.findObject(By.res(TestTags.ROUTINE_EDIT)).click()
        device.waitForTag(TestTags.ROUTINE_EDITOR_CONTENT)
        device.pressBack()
        device.waitForTag(TestTags.ROUTINE_DETAIL_CONTENT)
    }

    private fun MacrobenchmarkScope.startAndStopRoutine() {
        device.findObject(By.res(TestTags.ROUTINE_START)).click()
        device.waitForTag(TestTags.PLAYER_CONTENT)
        device.findObject(By.text("Stop")).click()
        check(device.wait(Until.hasObject(By.text("Stop this routine?")), TIMEOUT_MS)) {
            "Stop dialog never appeared"
        }
        // The dialog's confirm button is also "Stop"; its window is on top, so it comes last.
        device.findObjects(By.text("Stop")).last().click()
        // A stopped run opens its summary, which replaces the player; Done goes back to Detail.
        check(device.wait(Until.hasObject(By.text("Done")), TIMEOUT_MS)) { "Summary never appeared" }
        device.findObject(By.text("Done")).click()
        device.waitForTag(TestTags.ROUTINE_DETAIL_CONTENT)
    }

    private fun MacrobenchmarkScope.openSettings() {
        // Detail has no bottom bar, so go back to the routines list first.
        device.pressBack()
        device.waitForTag(TestTags.BOTTOM_BAR)
        device.findObject(By.res(TestTags.NAV_TODAY)).click()
        device.waitForTag(TestTags.TODAY_CONTENT)
        device.findObject(By.text("Settings")).click()
        check(device.wait(Until.hasObject(By.text("Back")), TIMEOUT_MS)) { "Settings never opened" }
        device.pressBack()
        device.waitForTag(TestTags.TODAY_CONTENT)
    }
}

private const val TIMEOUT_MS = 5_000L

private fun UiDevice.waitForTag(tag: String) =
    check(wait(Until.hasObject(By.res(tag)), TIMEOUT_MS)) { "\"$tag\" never appeared" }
