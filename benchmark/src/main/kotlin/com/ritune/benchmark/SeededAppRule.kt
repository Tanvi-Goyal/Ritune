package com.ritune.benchmark

import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import java.io.File
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement

internal const val PACKAGE = "com.ritune"

/**
 * Seeds the app once per test, and saves the dexopt state from before and after it.
 * If the two differ, the phone recompiled the app mid-run and the results are discarded.
 * Seed size comes from the `seedDays` and `seedRoutines` run arguments.
 * `compile=bgdexopt` first installs the app's bundled profile and compiles it the way the phone
 * does overnight; without it the app stays as installed (`verify`).
 * [withLongRoutine] also adds the 15-step routine the open-detail and open-editor journeys use.
 */
class SeededAppRule(private val withLongRoutine: Boolean = false) : TestRule {
    private val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
    private val args = InstrumentationRegistry.getArguments()
    private val seedDays = args.getString("seedDays")?.toInt() ?: 21
    private val seedRoutines = args.getString("seedRoutines")?.toInt() ?: 30
    private val compile = args.getString("compile")

    override fun apply(base: Statement, description: Description) = object : Statement() {
        override fun evaluate() {
            when (compile) {
                null -> Unit
                "bgdexopt" -> compileBundledProfile()
                else -> error("Unknown compile=$compile")
            }
            val before = dexoptState()
            device.executeShellCommand(
                "am start -W -n $PACKAGE/.SeedActivity --ei days $seedDays " +
                    "--ei routines $seedRoutines --ez longRoutine $withLongRoutine"
            )
            try {
                base.evaluate()
            } finally {
                save(description.methodName, before)
            }
        }
    }

    // profileinstaller's receiver writes the bundled profile where ART reads it, as on a first
    // launch outside Play. Then the overnight job compiles it. Not `cmd package compile`: MIUI
    // skips compiles started from the shell, but runs this job.
    private fun compileBundledProfile() {
        val install = device.executeShellCommand(
            "am broadcast -a androidx.profileinstaller.action.INSTALL_PROFILE " +
                "-n $PACKAGE/androidx.profileinstaller.ProfileInstallReceiver"
        )
        check("result=1" in install) { "Profile not installed: $install" }
        device.executeShellCommand("cmd package bg-dexopt-job $PACKAGE")
        val state = dexoptState()
        check("status=speed-profile" in state) { "Not compiled after bg-dexopt-job:\n$state" }
    }

    // Written after the test because Macrobenchmark empties the output folder when it starts.
    // Prefixed with the test name so tests in one run don't overwrite each other.
    private fun save(test: String, before: String) {
        val dir = args.getString("additionalTestOutputDir") ?: return
        File(dir, "$test-dexopt-before.txt").writeText(before)
        File(dir, "$test-dexopt-after.txt").writeText(dexoptState())
        File(dir, "$test-seed.txt")
            .writeText("days=$seedDays routines=$seedRoutines longRoutine=$withLongRoutine\n")
    }

    private fun dexoptState(): String =
        device.executeShellCommand("dumpsys package dexopt").lineSequence()
            .dropWhile { it.trim() != "[$PACKAGE]" }
            .takeWhile { it.trim() == "[$PACKAGE]" || !it.trim().startsWith("[") }
            .joinToString("\n")
}

/** `device.wait` returns false on timeout instead of failing, so check it. */
internal fun UiDevice.waitForTag(tag: String, timeoutMs: Long = 5_000) =
    check(wait(Until.hasObject(By.res(tag)), timeoutMs)) { "\"$tag\" never appeared" }

internal fun UiDevice.waitForText(text: String, timeoutMs: Long = 5_000) =
    check(wait(Until.hasObject(By.text(text)), timeoutMs)) { "\"$text\" never appeared" }
