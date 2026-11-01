package com.ritune.benchmark

import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.ExperimentalMacrobenchmarkApi
import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.StartupTimingMetric
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import com.ritune.core.designsystem.TestTags
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

private const val PACKAGE = "com.ritune"

@RunWith(AndroidJUnit4::class)
class StartupBenchmark {
    @get:Rule
    val rule = MacrobenchmarkRule()

    private val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())

    private lateinit var dexoptBefore: String

    // Seed size comes from the run arguments, so data experiments don't need code changes.
    private val args = InstrumentationRegistry.getArguments()
    private val seedDays = args.getString("seedDays")?.toInt() ?: 21
    private val seedRoutines = args.getString("seedRoutines")?.toInt() ?: 30

    @Before
    fun seed() {
        dexoptBefore = dexoptState()
        device.executeShellCommand(
            "am start -W -n $PACKAGE/.SeedActivity --ei days $seedDays --ei routines $seedRoutines"
        )
    }

    // Both files are written here because Macrobenchmark empties the output folder when it starts.
    @After
    fun saveDexoptState() {
        val dir = InstrumentationRegistry.getArguments().getString("additionalTestOutputDir")
            ?: return
        File(dir, "dexopt-before.txt").writeText(dexoptBefore)
        File(dir, "dexopt-after.txt").writeText(dexoptState())
        File(dir, "seed.txt").writeText("days=$seedDays routines=$seedRoutines\n")
    }

    // CompilationMode.Ignore is experimental; needed because MIUI ignores shell compile commands.
    @OptIn(ExperimentalMacrobenchmarkApi::class)
    @Test
    fun coldStartup() = rule.measureRepeated(
        packageName = PACKAGE,
        metrics = listOf(StartupTimingMetric()),
        compilationMode = CompilationMode.Ignore(),
        startupMode = StartupMode.COLD,
        iterations = 15,
        setupBlock = { pressHome() },
    ) {
        startActivityAndWait()
        // Keep tracing until reportFullyDrawn fires, otherwise there is no TTFD.
        val loaded = device.wait(
            Until.hasObject(
                By.res(TestTags.TODAY_CONTENT)
            ),
            5_000
        )
        check(loaded) { "Today screen never finished loading" }
    }

    /** This app's section of `dumpsys package dexopt`. */
    private fun dexoptState(): String =
        device.executeShellCommand("dumpsys package dexopt").lineSequence()
            .dropWhile { it.trim() != "[$PACKAGE]" }
            .takeWhile { it.trim() == "[$PACKAGE]" || !it.trim().startsWith("[") }
            .joinToString("\n")
}
