package com.ritune.benchmark

import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.ExperimentalMacrobenchmarkApi
import androidx.benchmark.macro.ExperimentalMetricApi
import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.StartupTimingMetric
import androidx.benchmark.macro.TraceSectionMetric
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
    fun coldStartup() = startup(StartupMode.COLD, withSections = true)

    /**
     * The process stays alive; a new activity is created, so Today reports TTFD again and its
     * activity-scoped ViewModels run the startup sections again.
     */
    @Test
    fun warmStartup() = startup(StartupMode.WARM, withSections = true)

    /**
     * The existing activity comes back. Its ReportDrawnWhen already fired on the first launch,
     * so this reports TTID only, and no startup section runs.
     */
    @Test
    fun hotStartup() = startup(StartupMode.HOT, withSections = false)

    // CompilationMode.Ignore is experimental; needed because MIUI skips dexopt started from the
    // shell ("Skipped dexOpt for start by shell"), so the other modes would be silently ignored.
    @OptIn(ExperimentalMacrobenchmarkApi::class, ExperimentalMetricApi::class)
    private fun startup(mode: StartupMode, withSections: Boolean) = rule.measureRepeated(
        packageName = PACKAGE,
        metrics = listOf(StartupTimingMetric()) + if (withSections) {
            STARTUP_SECTIONS.map { TraceSectionMetric(it, mode = TraceSectionMetric.Mode.First) }
        } else {
            emptyList()
        },
        compilationMode = CompilationMode.Ignore(),
        startupMode = mode,
        iterations = 15,
        setupBlock = { pressHome() },
    ) {
        startActivityAndWait()
        // Keep tracing until reportFullyDrawn fires, otherwise there is no TTFD.
        device.waitForTag(TestTags.TODAY_CONTENT)
    }

    private companion object {
        // The Phase 1 sections, each run once per launch. Seeding finds the rule's data already
        // there, so Seed.* times the row counts, not the first-launch inserts.
        val STARTUP_SECTIONS = listOf("InitializeApp", "TodayPreload", "ThemeRead", "Seed.anchors", "Seed.routines")
    }
}
