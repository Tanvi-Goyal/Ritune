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
        device.waitForTag(TestTags.TODAY_CONTENT)
    }
}
