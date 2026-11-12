[← Performance home](../performance.md)

# Methodology

> **In short.** Every number in this project names its device, build, compilation state and run count. This chapter defines those terms, the metrics, and the trace sections the app writes, so each result elsewhere can be read, and repeated, without guessing.

## Device

Published numbers come from two physical phones, one mid-range and one flagship, so results show both ends of the range Ritune's users have. Each result names its device. Emulators and the CI managed device (`pixel6Api34`) only check that benchmarks run; their timings are never published.

| Property | Redmi Note 10 Pro | Pixel 9 Pro |
|---|---|---|
| Model / codename | M2101K6P (`sweetin`) | Pixel 9 Pro (`caiman`) |
| Device class | Mid-range (2021) | Flagship (2024) |
| Chip | Qualcomm SM7150 (Snapdragon 732G) | Google Tensor G4 |
| RAM | 6 GB | 16 GB |
| Android version / API level | 12 / 31 | 17 / 37 |
| Refresh rate | 60 or 120 Hz; currently set to 60 Hz | Adaptive, up to 120 Hz |

The frame deadline depends on the refresh rate: 16.7 ms at 60 Hz, 8.3 ms at 120 Hz. Record each phone's refresh rate setting with every frame result, and don't change it between a before and an after run.

The Redmi is the primary device. It is closer to a typical user's phone, and slow code shows up there first.

## Build

| Property | Value |
|---|---|
| Build type | `release`, with R8 optimization on. The `benchmark` build type copies `release` with `initWith`, so benchmarks measure R8-optimized code. |
| Signing | Release keystore, from the `RITUNE_KEYSTORE_*` environment variables |
| Debuggable | No. A debuggable build turns off many ART optimizations, so debug timings are never published. |
| Commit | The commit hash goes next to each result |

Debug builds are used only where speed doesn't matter: recomposition counts, LeakCanary and StrictMode (Phase 4).

## Compilation state

Startup speed depends heavily on how much of the app is compiled to machine code, so every result names its state.

| State | How it is set | Real user it simulates |
|---|---|---|
| None (day one) | `adb shell cmd package compile -m verify -f com.ritune`, or `CompilationMode.None()` | First launch after an install or update, with no profile |
| Baseline Profile | On MIUI: `compile=bgdexopt` (profileinstaller's install broadcast, then `bg-dexopt-job`), before the app runs. Elsewhere: `CompilationMode.Partial(BaselineProfile.Require)` | First launch after a Play install, which compiles the shipped Baseline Profile |
| Speed profile | `adb shell cmd package compile -m speed-profile -f com.ritune` after several launches, or `CompilationMode.Partial(warmupIterations = N)` | A user days later, after background dexopt or cloud profiles |

`CompilationMode.Full()` compiles everything. No real user's phone is in that state, so it is never published as a result.

Always check the state before a run, and record it with the result:

```bash
adb shell dumpsys package dexopt | grep -A2 "\[com.ritune\]" | grep -o "status=[a-z-]*\] \[reason=[a-z-]*"
```

**Xiaomi (MIUI) ignores `cmd package compile` from adb.** It prints `Success`, but logcat shows `Skipped dexOpt for start by shell` and the state doesn't change. On the Redmi, set the state the way a real phone gets it:

| State | How to set it on MIUI | Expected `dumpsys` state |
|---|---|---|
| None (day one) | `adb uninstall com.ritune`, then install the APK again | `status=verify [reason=install]` |
| Speed profile | Use the app for 1–2 minutes, then `adb shell cmd package bg-dexopt-job com.ritune` (the same background job that compiles apps overnight) | `status=speed-profile [reason=bg-dexopt]` |

## Conditions

- Phone on charge and cool to the touch. Rest it between suites to avoid thermal throttling.
- Brightness fixed, auto-rotate off, Wi-Fi on, other apps force-stopped.
- Same app data for every run. Record whether the database was freshly seeded.

## Iterations and reporting

- Startup: at least 10 runs for a manual check, and 20–30 when claiming an improvement.
- Report the median (P50) with the min–max range. Frame metrics also report P90 and P99.
- Only compare runs from the same session. The Redmi's timings can shift between sessions by more than the noise (see [Phase 2](phase-2-macrobenchmark.md#the-redmi-drifts-between-sessions)), so a before and an after run back to back, ideally alternating A, B, A, B, with nothing changed on the phone in between.
- A change counts as an improvement only if it is larger than the run-to-run spread. To isolate one piece of work, measure its trace section directly as well as the end-to-end number.
- Report end-to-end changes as the headline, and section changes as the explanation. Only work on the critical path changes what the user feels.

## Metrics

| Metric | Meaning | Source |
|---|---|---|
| TTID | Time until `MainActivity` draws its first frame. Includes everything the splash waits for (`InitializeApp` → `DatabaseSeeder`, and the theme DataStore read). | `TotalTime` from `am start -W`, or `timeToInitialDisplayMs` |
| TTFD | Time until Today leaves its `Loading` state, reported by `ReportDrawnWhen` in `TodayScreen` | The `reportFullyDrawn()` slice in a trace, or `timeToFullDisplayMs` |
| Screen open | From the tap to the first frame that draws the screen's content | A `<Screen>.open` trace section, read with `TraceSectionMetric` |
| Frame CPU time | Main-thread and RenderThread time for one frame | `FrameTimingMetric` |
| Frame overrun | How far each frame finished past its deadline (negative means early) | `FrameTimingMetric` |

## Trace sections

Ritune writes these sections into system traces. Their names are stable, because queries and benchmarks look them up by name.

| Section | Where | Type | Measures | Added in |
|---|---|---|---|---|
| `InitializeApp` | `MainViewModel` | Async | Startup initialization, which the splash waits on | Phase 1 |
| `ThemeRead` | `AppThemeViewModel` | Async, ends at the first value | The first theme read, which the splash also waits on | Phase 1 |
| `Seed.anchors`, `Seed.routines` | `DatabaseSeeder`, inside the write transaction | Async | Counting, and seeding when empty | Phase 1 |
| `Player.notify` | `PlayerService` | Sync | Each ongoing-notification rebuild during a run | Phase 1 |
| `Routines.open`, `Rhythm.open` | Begins on the tab tap (`RituneApp`), ends on the first draw of the content | Async | Opening a tab | Phase 2 |
| `RoutineDetail.open`, `RoutineEditor.open` | Begins on the tap (`RoutineNavigation`), ends on the first draw of the step column | Async | Opening a routine or its editor | Phase 2 |
| `Player.open` | Begins on Detail's Start button, ends on the first draw of the player's content | Async | Starting a routine | Phase 2 |

The section names live in `core/designsystem/.../TraceSections.kt`.

- Sections around `suspend` calls must be async: a sync section would be closed by whichever thread the coroutine resumes on.
- A screen-open section ends on the first frame that **draws** the content (`Modifier.endTraceOnFirstDraw`), not when the data arrives. A section around only the query would miss navigation, composition, measuring and drawing, which is most of what the user waits for.
- Only one screen opens at a time, so a fixed cookie pairs each begin with its end.

---

Next: [Benchmark harness](benchmark-harness.md) → · [Performance home](../performance.md)
