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
 */
class SeededAppRule : TestRule {
    private val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
    private val args = InstrumentationRegistry.getArguments()
    private val seedDays = args.getString("seedDays")?.toInt() ?: 21
    private val seedRoutines = args.getString("seedRoutines")?.toInt() ?: 30

    override fun apply(base: Statement, description: Description) = object : Statement() {
        override fun evaluate() {
            val before = dexoptState()
            device.executeShellCommand(
                "am start -W -n $PACKAGE/.SeedActivity --ei days $seedDays --ei routines $seedRoutines"
            )
            try {
                base.evaluate()
            } finally {
                save(description.methodName, before)
            }
        }
    }

    // Written after the test because Macrobenchmark empties the output folder when it starts.
    // Prefixed with the test name so tests in one run don't overwrite each other.
    private fun save(test: String, before: String) {
        val dir = args.getString("additionalTestOutputDir") ?: return
        File(dir, "$test-dexopt-before.txt").writeText(before)
        File(dir, "$test-dexopt-after.txt").writeText(dexoptState())
        File(dir, "$test-seed.txt").writeText("days=$seedDays routines=$seedRoutines\n")
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
