# Performance

This is the single source of truth for how Ritune's performance was measured and improved: what was found, what was changed, what was dropped, and how to repeat any of it. It is written for someone who has never seen the project. Read this page first; every claim here links to the chapter that proves it.

Ritune is measured before it is changed. Every result names its device, build, compilation state and iteration count, so anyone can rerun it.

## The story in five steps

1. **Measure first (Phase 0).** The same release build draws its first frame in 377 ms on a mid-range Redmi Note 10 Pro and 133 ms on a Pixel 9 Pro: 2.8× slower. Compiling the app helps less than expected, so something else is the cost.
2. **Look inside one start (Phase 1).** A single annotated trace shows that 40% of the Redmi's startup passes before any Ritune code runs, and that opening the Room database is most of what the splash screen waits for.
3. **Build a harness and test hypotheses (Phase 2).** Macrobenchmark replaces hand-run `adb` loops. It rules out the first suspected fix (Today's data size), and finds two real kinds of cost: work that grows with every list row, and code running for the first time.
4. **Ship compiled code (Phase 3).** Ritune's own Baseline Profile cuts compiled cold-start TTFD by 83 ms (−10.2%), proved with an alternating A/B on the same commit.
5. **Fix what the traces point at (Phase 4).** Nine experiments: six shipped, two dropped because the gain was lost in the noise, one kept as a cleanup. Together, Today shows its content 110 ms sooner after a cold start (−14%), and every screen that was touched opens 17–28% faster.

## Results at a glance

All numbers are from the Redmi Note 10 Pro (the mid-range phone) unless the row says otherwise. Each row links to the full result.

| Finding | Number | Conditions | Phase |
|---|---|---|---|
| The mid-range phone starts Ritune 2.8× slower than the flagship | TTID median 377 ms vs 133 ms on the Pixel 9 Pro | Release build, day one, `am start -W`, 10 runs | [0](performance/phase-0-foundations.md#phase-0-manual-cold-start) |
| 40% of cold-start TTID passes before any Ritune code runs | `bindApplication` at 144 of 357 ms | One Perfetto trace, release build | [1](performance/phase-1-cold-start-trace.md#phase-1-cold-start-trace) |
| Opening the Room database is most of the startup wait | 27 of `InitializeApp`'s 34 ms | Same trace | [1](performance/phase-1-cold-start-trace.md#phase-1-cold-start-trace) |
| Data size does not explain Today's slow load | 4 seed sizes within ~45 ms of each other on TTFD, against ~40 ms of noise | Macrobenchmark, day one | [2](performance/phase-2-macrobenchmark.md#data-size-is-not-why-today-is-slow) |
| Compiled code is what Today's load waits for | Speed profile cuts TTFD by 118 ms (735 → 617 ms) | Macrobenchmark, session 1 | [2](performance/phase-2-macrobenchmark.md#cold-start-and-compilation-modes) |
| Opening Routines grows with every row | ~135 ms fixed + ~4.6 ms per routine | Macrobenchmark, day one, 7 / 37 / 67 routines | [2](performance/phase-2-macrobenchmark.md#opening-routines) |
| Ritune's own Baseline Profile speeds up compiled cold start | TTFD −83 ms (−10.2%), TTID −29 ms (−5.7%) | Alternating A/B, same commit, 2 × 15 iterations per side | [3](performance/phase-3-baseline-profiles.md#phase-3-baseline-profiles) |
| A lazy Routines list opens 60% faster at 67 routines | 195 → 78 ms (−118 ms), shader compilation subtracted; flat from 7 to 67 routines | Alternating A/B against `main`, compiled, 30 iterations per side | [4](performance/phase-4-fixes.md#fix-1-a-lazy-routines-list) |
| Starting a routine no longer draws an empty player frame | `Player.open` 100 → 88 ms (−12 ms, −12%) | Alternating A/B, compiled, 30 iterations per side | [4](performance/phase-4-fixes.md#fix-2-the-player-draws-its-content-on-the-first-frame) |
| All of Phase 4 together: Today is ready sooner after a cold start, and every screen opens faster | Cold-start TTFD 802 → 691 ms (−110 ms, −14%); Routines −28%, Detail −23%, Player −17% | Before vs after Phase 4, alternating A/B, compiled, 30 per side | [4](performance/phase-4-before-and-after.md#phase-4-before-and-after) |
| Preloading Today during the splash shows its content sooner | TTFD 766 → 691 ms (−75 ms, −10%) | Cold start, alternating A/B, compiled, 30 per side | [4](performance/phase-4-fixes.md#fix-5-today-draws-its-content-on-the-first-frame) |
| Starting `InitializeApp` from `Application.onCreate` drops a frame from startup | Content in the first frame in 27 of 30 starts (was 0); TTFD −43 ms, −71 ms with shader compilation subtracted | Same | [4](performance/phase-4-fixes.md#fix-6-start-initializeapp-from-application) |
| Detail loads one routine, and starts loading before the screen subscribes | `RoutineDetail.open` 113 → 100 ms (−12.7 ms, −11%); 2 fewer empty frames | Alternating A/B, compiled, 30 per side | [4](performance/phase-4-fixes.md#fix-8-detail-and-editor-load-sooner) |
| Leaks, StrictMode, R8 rules and Compose stability add no fix | No leaks, no violation in Ritune's code, ~1% of code kept by rules | Phase 4 analysis | [4](performance/phase-4-fixes.md#phase-4-analysis-findings) |

## How the work is organised

The work moved through five stages, Phase 0 to Phase 4. Each stage ends in a measured result and a pull request, and has its own chapter.

| Phase | Goal | What it found | Chapter | PR | Series |
|---|---|---|---|---|---|
| 0. Foundations | Methodology, devices, a first manual baseline | The Redmi starts 2.8× slower than the Pixel; between-session noise is about 10% | [Phase 0](performance/phase-0-foundations.md) | [#39](https://github.com/Tanvi-Goyal/Ritune/pull/39) | [Part 1](https://medium.com/@goyaltanvi94/ritune-takes-2-8-longer-to-start-on-a-mid-range-phone-here-is-how-i-found-where-the-time-goes-ee1453c9b3e2) |
| 1. Instrumentation | Trace sections, TTFD, one annotated cold-start trace | 40% of startup precedes Ritune's code; Room's open dominates the splash wait | [Phase 1](performance/phase-1-cold-start-trace.md) | [#40](https://github.com/Tanvi-Goyal/Ritune/pull/40) | Part 1 |
| 2. Macrobenchmark | A repeatable harness for startup, scroll and screen-open journeys | Data size is not the cause; Routines grows 4.6 ms per row; the rest is first-run code | [Phase 2](performance/phase-2-macrobenchmark.md) | [#41](https://github.com/Tanvi-Goyal/Ritune/pull/41), [#43](https://github.com/Tanvi-Goyal/Ritune/pull/43) | [Part 2](https://medium.com/@goyaltanvi94/custom-baseline-profiles-made-ritunes-first-screen-load-10-faster-b256866b154d) |
| 3. Baseline and Startup Profiles | Ship compiled first-run code, measured in an A/B | Compiled cold-start TTFD −83 ms (−10.2%) | [Phase 3](performance/phase-3-baseline-profiles.md) | [#44](https://github.com/Tanvi-Goyal/Ritune/pull/44) | Part 2 |
| 4. Deep analysis and fixes | Trace-driven fixes, each with its benchmark delta | 7 fixes merged, 2 dropped; TTFD −110 ms (−14%) | [Fixes](performance/phase-4-fixes.md), [before/after](performance/phase-4-before-and-after.md) | [#53](https://github.com/Tanvi-Goyal/Ritune/pull/53)–[#61](https://github.com/Tanvi-Goyal/Ritune/pull/61) | [Part 3](https://medium.com/@goyaltanvi94/experiments-and-outcomes-improving-ritunes-performance-655b8002fd44) |

## Where to go next

| If you want to… | Read |
|---|---|
| Understand what a number means before trusting it | [Methodology](performance/methodology.md): devices, builds, compilation states, metrics |
| Know what the benchmarks do and where the raw results live | [Benchmark harness](performance/benchmark-harness.md) |
| Follow the investigation in order | Phases [0](performance/phase-0-foundations.md), [1](performance/phase-1-cold-start-trace.md), [2](performance/phase-2-macrobenchmark.md), [3](performance/phase-3-baseline-profiles.md), [4](performance/phase-4-fixes.md) |
| See what one specific fix changed and how it was measured | [Phase 4 fix list](performance/phase-4-fixes.md#fix-candidates-and-build-order) |
| See the whole of Phase 4 measured at once | [Phase 4 before and after](performance/phase-4-before-and-after.md) |
| Take the rules for your own measurements | [Lessons and changes of approach](performance/lessons.md) |
| Reproduce a measurement | [Commands](performance/commands.md) |
| Look at the raw benchmark output | [`perf/results/`](perf/results/) |

## Glossary

| Term | Meaning |
|---|---|
| **TTID** | Time to initial display: from launch until the app draws its first frame. |
| **TTFD** | Time to full display: from launch until Today shows real content. This is what the user waits for. |
| **Cold, warm, hot start** | Cold: the process is created. Warm: the process is kept, a new activity is created. Hot: the existing activity comes back. |
| **Compilation state** | How much of the app is already machine code. *Day one*: none (a fresh install). *Baseline Profile*: what a Play install gets. *Speed profile*: a user days later. Every result names its state; see [Methodology](performance/methodology.md#compilation-state). |
| **Baseline Profile** | A list of classes and methods shipped in the APK, so ART compiles them at install and the first launch runs compiled code. |
| **Trace section** | A named slice the app writes into a system trace, so Perfetto shows Ritune's work next to Android's. |
| **Macrobenchmark** | The Jetpack library that launches or drives the app, repeats a journey N times and reads metrics from the traces. |
| **A/B (alternating)** | Run the old build (A) and the new build (B) in the order A, B, A, B in one session, so phone drift hits both sides equally. |
| **P50, P90, P99** | The median, and the values 90% and 99% of runs fall under. On a noisy phone the P90 matters as much as the median. |
| **Noise** | The spread between repeated runs of the same code. A change only counts if it is larger than this. |
| **Shader compile** | The GPU compiling shaders on the first draws. Macrobenchmark clears the cache before each cold start, so it lands unevenly between runs; where it matters, results are given with it subtracted. |
| **MIUI** | Xiaomi's Android build. It ignores `adb shell cmd package compile` while still printing `Success`, which shapes how Ritune sets compilation states. |

## The five rules that came out of this work

The full list is in [Lessons](performance/lessons.md). These five matter most:

- **Measure the noise before the effect.** A 10% swing between Redmi sessions decides how many runs every later claim needs.
- **Only compare runs from one session, alternating A, B, A, B.** The Redmi shifted about 100 ms on TTFD between sessions with no code change.
- **Name the compilation state with every number**, and check it before and after each run.
- **Change one input before fixing what you suspect.** The query fix I expected to need would have saved almost nothing.
- **Measure the whole phase once at the end.** The startup fixes summed to 142 ms on their own, but gave 110 ms together.

## Where things live

| Path | Holds |
|---|---|
| `docs/performance.md` and `docs/performance/` | This page and its chapters |
| `docs/perf/results/` | One folder per benchmark run, with its [README](perf/results/README.md) listing the files, sessions and known gaps |
| `docs/perf/cold-start.pftxt`, `cold-start.sql` | The Perfetto config and the query that summarise a cold-start trace |
| `docs/perf/phase3-ab.sh`, `phase4-ab.sh` | The scripts that run an alternating A/B and copy each run into `results/` |
| `:benchmark` | The Macrobenchmark journeys |
| `:baselineprofile` | The Baseline Profile generator |
| `core/designsystem/.../TraceSections.kt` | The names of Ritune's trace sections |

## Write-ups and sources

The work is written up as a series on Medium:

- Part 1, Phases 0–1: [Ritune takes 2.8× longer to start on a mid-range phone. Here is how I found where the time goes](https://medium.com/@goyaltanvi94/ritune-takes-2-8-longer-to-start-on-a-mid-range-phone-here-is-how-i-found-where-the-time-goes-ee1453c9b3e2)
- Part 2, Phases 2–3: [Custom Baseline Profiles made Ritune's first screen load 10% faster](https://medium.com/@goyaltanvi94/custom-baseline-profiles-made-ritunes-first-screen-load-10-faster-b256866b154d)
- Part 3, Phase 4: [Experiments and Outcomes: Improving Ritune's Performance](https://medium.com/@goyaltanvi94/experiments-and-outcomes-improving-ritunes-performance-655b8002fd44)

Primary sources:

- [App startup time](https://developer.android.com/topic/performance/vitals/launch-time), Android Developers
- [Custom trace events](https://developer.android.com/topic/performance/tracing/custom-events), Android Developers
- [Macrobenchmark overview](https://developer.android.com/topic/performance/benchmarking/macrobenchmark-overview), Android Developers
- [Baseline Profiles overview](https://developer.android.com/topic/performance/baselineprofiles/overview), Android Developers
- [Create Baseline Profiles](https://developer.android.com/topic/performance/baselineprofiles/create-baselineprofile), Android Developers
- [Startup Profiles and DEX layout](https://developer.android.com/topic/performance/baselineprofiles/dex-layout-optimizations), Android Developers
- [Recording system traces with `record_android_trace`](https://perfetto.dev/docs/quickstart/android-tracing), Perfetto
