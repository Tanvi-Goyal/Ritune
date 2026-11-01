package com.ritune.benchmark

import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.ExperimentalMacrobenchmarkApi
import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.StartupTimingMetric
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ritune.core.designsystem.TestTags
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class StartupBenchmark {
    @get:Rule(order = 0)
    val app = SeededAppRule()

    @get:Rule(order = 1)
    val rule = MacrobenchmarkRule()

    @Test
    fun coldStartup() = startup(StartupMode.COLD)

    /** The process stays alive; a new activity is created, so Today reports TTFD again. */
    @Test
    fun warmStartup() = startup(StartupMode.WARM)

    /**
     * The existing activity comes back. Its ReportDrawnWhen already fired on the first launch,
     * so this reports TTID only.
     */
    @Test
    fun hotStartup() = startup(StartupMode.HOT)

    // CompilationMode.Ignore is experimental; needed because MIUI skips dexopt started from the
    // shell ("Skipped dexOpt for start by shell"), so the other modes would be silently ignored.
    @OptIn(ExperimentalMacrobenchmarkApi::class)
    private fun startup(mode: StartupMode) = rule.measureRepeated(
        packageName = PACKAGE,
        metrics = listOf(StartupTimingMetric()),
        compilationMode = CompilationMode.Ignore(),
        startupMode = mode,
        iterations = 15,
        setupBlock = { pressHome() },
    ) {
        startActivityAndWait()
        // Keep tracing until reportFullyDrawn fires, otherwise there is no TTFD.
        device.waitForTag(TestTags.TODAY_CONTENT)
    }
}
