[← Performance home](../performance.md)

# Benchmark harness

> **In short.** A Macrobenchmark module drives a release-like build through fixed user journeys, records a trace on every run and reads the metrics from the traces. This chapter lists the journeys, how the data is seeded, where results are stored, and the measurement sessions.

Phases 2 and 3 replaced the manual `am start -W` loops with Jetpack Macrobenchmark. A Macrobenchmark test launches or drives the release-like build, repeats each journey, records a system trace on every iteration and reads its metrics from those traces.

| Module | Purpose |
|---|---|
| `:benchmark` | A `com.android.test` module (convention plugin `ritune.android.test`) holding every journey below |
| `:baselineprofile` | The generator, `BaselineProfileGenerator`, which walks the critical journeys to record the profile |
| `app/src/main/generated/baselineProfiles/` | The generated `baseline-prof.txt` (24,311 lines, 2,548 for Ritune's own code) and `startup-prof.txt` |

## Journeys

All journeys use `CompilationMode.Ignore()` and 15 iterations. Macrobenchmark's own compilation modes compile from the shell, which MIUI skips while still reporting success, so the harness sets the state itself and checks it (see [Seeding and state checks](#seeding-and-state-checks)).

| Test | What is measured | Metrics |
|---|---|---|
| `StartupBenchmark.coldStartup` | Cold start (`StartupMode.COLD`) to Today with data | TTID, TTFD, startup sections (`Mode.First`) |
| `StartupBenchmark.warmStartup` | Warm start: process kept, new `MainActivity` | TTID, TTFD, startup sections |
| `StartupBenchmark.hotStartup` | Hot start: the existing activity comes back | TTID |
| `ScrollBenchmark.scrollRoutines` | Fling down and up the Routines list; only the flings are measured | `FrameTimingMetric` |
| `OpenScreenBenchmark.openRoutines`, `openRhythm` | The tab tap, after a launch to Today | `Routines.open`, `Rhythm.open` |
| `OpenRoutineBenchmark.openDetail` | The tap on the 15-step routine | `RoutineDetail.open` |
| `OpenRoutineBenchmark.openEditor` | The tap on Edit, after Detail is open | `RoutineEditor.open` |
| `OpenRoutineBenchmark.openPlayer` | The tap on Start, after Detail is open | `Player.open` |
| `OpenRoutineBenchmark.skipSteps` | 5 taps on Skip in a running 15-step routine | `Player.notify` average, count and max |

Everything before the measured action runs in `setupBlock`, which isn't timed.

## Seeding and state checks

`SeededAppRule` runs before every journey:

- **Seeds the database** through `SeedActivity` in the benchmark build, from the run arguments `seedDays` (default 21 days of anchor events) and `seedRoutines` (default 30 routines). `longRoutine=true` adds one 15-step routine, saved last and without the random generator, so the rest of the data matches earlier runs.
- **Records the compilation state** before and after the run. If the two differ, the phone recompiled the app mid-run and the run is discarded.
- **`compile=bgdexopt`** reaches the Baseline Profile state on MIUI: it sends profileinstaller's install broadcast, runs `bg-dexopt-job`, and checks the state is `speed-profile` before the app has ever run.

## Results folders

Every run is copied straight after it finishes to [`perf/results/<date>-<phone>-<journey>-<state>/`](../perf/results/), because Gradle clears its output folder on the next run. Each folder holds the benchmark JSON, the dexopt state before and after, the seed size and a summary per metric. Perfetto traces are not committed. [`perf/results/README.md`](../perf/results/README.md) lists the files and every known gap.

## Sessions

The Redmi's timings can shift between sessions by more than the noise, so results are grouped by session. **Only compare results from the same session.**

| Session (UTC) | What ran | Notes |
|---|---|---|
| 1: 2026-11-01, 06:30–11:30 | Cold startup baseline and A/A, seed experiment, speed-profile, scroll, open Routines (and R0/R30/R60), open Rhythm, open Detail and Editor, start routine | Consistent with itself: the open-Routines rerun landed within 8 ms of the first run |
| — about 12:20 | MIUI optimization switched off and back on, to test whether `CompilationMode.Full` could work (it can't) | |
| 2: 2026-11-01, 12:36–14:30 | Warm and hot startup, a cold rerun, and old-commit vs current-code A/B | Cold is about 60–80 ms slower on TTID and 120–220 ms on TTFD than in session 1, for old and new code alike |
| 3: 2026-11-02, 10:38–11:00 | Skipping player steps, then cold and warm startup with the Phase 1 trace sections | Cold TTID (523 ms) is still at session 2's level, so the shift has lasted overnight |
| 4: 2026-11-03, 06:31–08:33 | The Phase 3 Baseline Profile A/B, 24 runs | Auto-rotate off |
| 5: 2026-11-06, 02:32–10:54 | Phase 4: fix 1 A/B, 12 runs (02:32–03:14); fix 2, 4 runs (04:18–04:43); fix 3 baseline, 1 run (~05:05); fix 4 in three steps, 12 runs (05:28–06:30); notification A/B (09:24–09:52); fix 5 (09:52–10:03); fix 6 (10:43–10:54); two diagnostic runs | Compiled only. A moved within the session (cold TTFD 799 → 831 ms), so each fix is read against its own A runs |
| 6: 2026-11-08 | Phase 4: fix 7 A/B, 4 runs; Today composition diagnostic, 1 run; fix 8 A/Bs, 12 runs (detail early load 06:26–06:50); the [before/after](phase-4-before-and-after.md#phase-4-before-and-after) A/B, 32 runs (08:21–10:43) | Compiled only. One Editor B run was rerun whole after an install prompt went unaccepted |

The shift between sessions 1 and 2 lines up with the MIUI optimization toggle, but that is not proven. What is proven is that the code did not cause it: the old commit `0afdb46` was just as slow in session 2 ([details](phase-2-macrobenchmark.md#the-redmi-drifts-between-sessions)).

---

← Previous: [Methodology](methodology.md) · Next: [Phase 0: manual cold start](phase-0-foundations.md) → · [Performance home](../performance.md)
