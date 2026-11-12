[← Performance home](../performance.md)

# Phase 2: Macrobenchmark

> Written up in the series as [Part 2](https://medium.com/@goyaltanvi94/custom-baseline-profiles-made-ritunes-first-screen-load-10-faster-b256866b154d).

**Phase 2 found two kinds of cost: work that grows with every row, where a lazy list is the fix, and code running for the first time, which a Baseline Profile targets.** It also ruled out a planned fix: Today is not slow because of its data.

All Phase 2 numbers come from Macrobenchmark on the phones, with the JSON for every run in [`perf/results/`](../perf/results/). Each journey was paired with a hypothesis before it ran:

| Hypothesis | Check | Outcome |
|---|---|---|
| Today is slow because of its queries: it reads every routine with every step, but shows only names | Cold startup with the seed split into history and routines, one variable at a time | **Wrong.** Data size moved TTFD by less than the noise. The cost is code running for the first time. |
| Opening Routines costs more as the list grows, because `Column + verticalScroll` composes every row | Open Routines with 7, 37 and 67 routines | **Confirmed.** About 4.6 ms per routine, in a straight line. |

## Summary

Redmi Note 10 Pro, 60 Hz, None (day one, `verify`), `CompilationMode.Ignore()`, 15 iterations, seed 21 days / 30 routines. Sessions are defined in [Sessions](benchmark-harness.md#sessions).

| Journey | Metric | Session | Median | P90 | Results |
|---|---|---|---|---|---|
| Cold startup | TTID / TTFD | 1 | 453 / 735 ms | 494 / 764 ms | [results](../perf/results/2026-11-01-redmi-startup-day-one/) |
| Cold startup | TTID / TTFD | 2 | 529 / 952 ms | 569 / 1036 ms | [results](../perf/results/2026-11-01-redmi-startup-day-one-rerun/) |
| Warm startup | TTID / TTFD | 2 | 241 / 371 ms | 275 / 421 ms | [results](../perf/results/2026-11-01-redmi-startup-warm-day-one/) |
| Hot startup | TTID (no TTFD, by design) | 2 | 127 ms | 142 ms | [results](../perf/results/2026-11-01-redmi-startup-hot-day-one/) |
| Scroll Routines | Frame CPU time / overrun | 1 | 7.5 / −2.8 ms | 9.6 / +1.8 ms (P99 14.0 / +5.0) | [results](../perf/results/2026-11-01-redmi-scroll-day-one/) |
| Open Routines | `Routines.open` | 1 | 316 ms | 342 ms | [results](../perf/results/2026-11-01-redmi-open-routines-day-one/) |
| Open Rhythm | `Rhythm.open` | 1 | 140 ms | 162 ms | [results](../perf/results/2026-11-01-redmi-open-rhythm-day-one/) |
| Open Detail (15-step routine) | `RoutineDetail.open` | 1 | 171 ms | 188 ms | [results](../perf/results/2026-11-01-redmi-open-routine-detail-editor/) |
| Open Editor (15-step routine) | `RoutineEditor.open` | 1 | 172 ms | 189 ms | same folder |
| Start routine | `Player.open` | 1 | 151 ms | 168 ms | [results](../perf/results/2026-11-01-redmi-open-player-day-one/) |
| Cold startup, with trace sections | TTID / TTFD | 3 | 523 / 921 ms | 601 / 1130 ms | [results](../perf/results/2026-11-02-redmi-startup-sections/) |
| Warm startup, with trace sections | TTID / TTFD | 3 | 252 / 387 ms | 284 / 451 ms | [results](../perf/results/2026-11-02-redmi-startup-sections-warm/) |
| Skip 5 player steps (15-step routine) | `Player.notify` average / max per call | 3 | 6.0 / 7.5 ms | 6.8 / 11.2 ms | [results](../perf/results/2026-11-02-redmi-player-skip-steps/) |

The Pixel 9 Pro has one Phase 2 result, cold startup at None: TTID 162 ms (P90 174), TTFD 245 ms (P90 271).

## Cold start and compilation modes

`StartupBenchmark.coldStartup`: `StartupMode.COLD`, `CompilationMode.Ignore()`, 15 iterations. The database is seeded once before the run with 21 days of anchor events and 30 routines. TTFD is when Today first shows loaded data.

These numbers are not comparable with Phase 0: the method is different, and the database holds far more data.

| Date | Commit | Device | Compilation state | TTID median (P90) | TTFD median (P90) | Results |
|---|---|---|---|---|---|---|
| 2026-11-01 | `277e6a2` | Redmi Note 10 Pro, 60 Hz | None (day one) | 453 ms (494) | 735 ms (764) | [results](../perf/results/2026-11-01-redmi-startup-day-one/) |
| 2026-11-01 | `277e6a2` | Pixel 9 Pro, adaptive | None (day one) | 162 ms (174) | 245 ms (271) | [results](../perf/results/2026-11-01-pixel-startup-day-one/) |
| 2026-11-01 | `0afdb46` | Redmi Note 10 Pro, 60 Hz | Speed profile (`bg-dexopt`) | 431 ms (460) | 617 ms (652) | [results](../perf/results/2026-11-01-redmi-startup-speed-profile/) |

Compilation modes, Redmi, cold startup, session 1:

| Mode | How it was set | TTID median (P90) | TTFD median (P90) |
|---|---|---|---|
| None (`verify`) | Fresh install | 453 ms (494) | 735 ms (764) |
| Speed profile | 1–2 minutes of use, then `bg-dexopt-job` | 431 ms (460) | 617 ms (652) |
| Partial (Baseline Profile) | Measured in Phase 3, session 4, against a fresh None row: see [Phase 3](phase-3-baseline-profiles.md#phase-3-baseline-profiles) | — | — |
| Full (`speed`) | Not reachable on MIUI: shell-started dexopt is skipped (`Skipped dexOpt for start by shell`), with MIUI optimization on or off | — | — |

**Compilation state matters more than data.** On the Redmi, speed-profile cuts the TTFD median by 118 ms (735 → 617), three times the noise. TTID drops by only 22 ms. Most of the gain is in loading Today, which runs code for the first time in the process. A Baseline Profile would give day-one installs this compiled state.

**Macrobenchmark clears the shader cache before each cold start** (`MacrobenchmarkScope.dropShaderCache`, through `profileinstaller`). Every iteration therefore compiles its shaders again. In the Redmi speed-profile median trace, the first frame after TTID waits 67 ms on RenderThread `shader_compile`. Real users pay this only after installing or updating the app; later launches reuse the cache.

## Noise

Each phone ran the same build twice at the same state (an A/A test):

| Medians, run 1 → run 2 | TTID | TTFD | Change must exceed |
|---|---|---|---|
| Redmi | 467 → 453 ms | 771 → 735 ms | ~15 ms TTID, ~40 ms TTFD |
| Pixel ([second run](../perf/results/2026-11-01-pixel-startup-day-one-aa/)) | 162.5 → 162.4 ms | 245 → 240 ms | ~5 ms TTID, ~10 ms TTFD |

The Redmi is far noisier than the Pixel. To prove smaller changes, alternate builds A, B, A, B with 30+ iterations per run.

## The Redmi drifts between sessions

In session 2, a cold rerun of the current code gave TTID 529 ms and TTFD 952 ms, far above session 1's 453 / 735 ms. To find out whether the code or the phone had changed, the old commit `0afdb46` and the current code ran back to back:

| Session 2, cold, day one, seed 21/30 | TTID median (P90) | TTFD median (P90) | Results |
|---|---|---|---|
| Old commit `0afdb46` | 532 ms (599) | 896 ms (1021) | [results](../perf/results/2026-11-01-redmi-startup-ab-old-0afdb46/) |
| Current code (`77d3d11`) | 511 ms (556) | 854 ms (940) | [results](../perf/results/2026-11-01-redmi-startup-ab-head/) |
| Earlier rerun, current code | 529 ms (569) | 952 ms (1036) | [results](../perf/results/2026-11-01-redmi-startup-day-one-rerun/) |

The old commit is as slow as the current code, so **the phone changed, not the code**. The phone wasn't hot (thermal status 0, 32 °C), was charging, and had power saver off. The trace puts the extra time in `InitializeApp` and `ThemeRead` (about 86–89 ms each, against about 34 ms in Phase 1), which both read storage. The cause on the phone is not known.

Two consequences:

- Session 1 and session 2 numbers can't be compared. Every comparison, including Phase 3's before and after, must run back to back in one session, ideally alternating A, B, A, B.
- Two runs of the same code in session 2 differed by about 100 ms on TTFD (952 vs 854 ms), more than the ~40 ms A/A spread in session 1.

## Warm and hot startup

Session 2. `StartupBenchmark.warmStartup` and `hotStartup` use the same setup as cold (`pressHome()`, 15 iterations, seed 21/30, `Ignore()`). Only the `StartupMode` changes.

- **Warm** keeps the process. The launch clears the task, so a new `MainActivity` is created.
- **Hot** brings back the existing activity.

| Date | Device | Compilation state | Start | TTID median (P90) | TTFD median (P90) | Results |
|---|---|---|---|---|---|---|
| 2026-11-01 | Redmi Note 10 Pro, 60 Hz | None (day one) | Cold (session 2) | 529 ms (569) | 952 ms (1036) | [results](../perf/results/2026-11-01-redmi-startup-day-one-rerun/) |
| 2026-11-01 | Redmi Note 10 Pro, 60 Hz | None (day one) | Warm | 241 ms (275) | 371 ms (421) | [results](../perf/results/2026-11-01-redmi-startup-warm-day-one/) |
| 2026-11-01 | Redmi Note 10 Pro, 60 Hz | None (day one) | Hot | 127 ms (142) | none, by design | [results](../perf/results/2026-11-01-redmi-startup-hot-day-one/) |

**Hot has no TTFD, and that is correct.** `ReportDrawnWhen` reports through the activity's `FullyDrawnReporter`, which fires once per activity. A hot start reuses the activity whose reporter already fired, so nothing reports for that launch. Today's data is already on screen anyway.

What the median traces show:

- **Warm (241 ms)** skips process creation and `Application.onCreate`, but still pays for:
  - creating the activity (`activityStart` 39 ms, of which `performCreate` 23 ms) and `activityResume` (16 ms)
  - the splash frame (35 ms, including a 10 ms `relayoutWindow`)
  - `InitializeApp` and `ThemeRead` (about 24 ms each, in parallel)
  - Today's first composition in the TTID frame (72 ms: recompose 17, measure 26, `relayoutWindow` 10)

  TTFD then follows two frames later.
- **Hot (127 ms)** does no Compose work at all. Its one big frame (46 ms) is `relayoutWindow` (15 ms) plus re-recording the whole UI's drawing (`Record View#draw()`, 16 ms), because the window's surface and display lists are released while the app is in the background.

## Startup trace sections

The trace sections from Phase 1, read with `TraceSectionMetric` in session 3 (2026-11-02, Redmi Note 10 Pro, None (day one), `Ignore()`, 15 iterations, commit `8eecb1c`):

- `StartupBenchmark.coldStartup` and `warmStartup` read `InitializeApp`, `ThemeRead`, `Seed.anchors` and `Seed.routines` with `Mode.First`, since each runs once per launch. `hotStartup` reads none of them: it reuses the activity and its ViewModels, so the sections don't run.

| Section, median (P90) | Cold | Warm |
|---|---|---|
| TTID | 523 ms (601) | 252 ms (284) |
| TTFD | 921 ms (1130) | 387 ms (451) |
| `InitializeApp` | 81.0 ms (95.3) | 28.9 ms (31.4) |
| `ThemeRead` | 81.5 ms (94.0) | 28.7 ms (32.3) |
| `Seed.anchors` | 2.4 ms (10.0) | 0.9 ms (1.7) |
| `Seed.routines` | 1.7 ms (7.7) | 0.5 ms (2.5) |

[Cold results](../perf/results/2026-11-02-redmi-startup-sections/), [warm results](../perf/results/2026-11-02-redmi-startup-sections-warm/).

**The splash waits for both sections.** `MainActivity` keeps the splash up while `startDestination` or `themeMode` is null. So `InitializeApp` and `ThemeRead` sit on the path to TTID, and the first frame with Today starts within a few milliseconds of `InitializeApp` ending.

**Seeding is a small part of `InitializeApp`.** In a cold trace (iteration 14, TTID 507 ms), the work runs like this:

| Time from `bindApplication` | What happens |
|---|---|
| 144–208 ms | The first frame on the main thread, under the splash |
| 169 / 172 ms | `ThemeRead` and `InitializeApp` start |
| 210–220 ms | `Seed.anchors` and `Seed.routines` run, about 10 ms in all |
| 215–249 ms | The main thread sleeps for 34 ms |
| 251 / 253 ms | `ThemeRead` and `InitializeApp` end |
| 256–353 ms | The first frame with Today (97 ms); TTID ends at 359 ms |

About 38 ms passes before the seeding starts (opening the Room database, as Phase 1 found) and about 33 ms after it (committing the transaction and resuming the coroutine). Neither has its own section. Between 170 and 250 ms the background threads ran for about 22 ms, waited about 20 ms for a free CPU while the first frames were drawn, and were blocked on disk for about 11 ms.

**Warm start waits on the main thread instead.** In a warm trace (iteration 9), the database is already open and seeding takes 1.5 ms, all done within 7 ms. Both sections start during the first frame (38 ms) and can only finish on the main thread, so they end when that frame does, about 30 ms in. Warm saves about 52 ms per section, roughly the cost of opening the database and reading the settings file for the first time in a new process.

In both modes `ThemeRead` and `InitializeApp` end within 2 ms of each other. In warm start that is because both resume on the main thread after the same frame. In cold start it suggests a shared wait on disk or the background thread pool, but the trace doesn't prove it.

These sections run at about 81 ms in session 3 against about 34 ms in Phase 1. That matches the drift described above (86–89 ms in session 2), not a code change.

## Data size is not why Today is slow

The time from first frame to fully drawn is 282 ms on the Redmi and 83 ms on the Pixel; the Phase 1 trace showed 83 ms on the Redmi. Changing only the seed size, at commit `0afdb46`, moved TTFD by less than the noise:

| Seed (days / routines) | TTID median | TTFD median | TTFD − TTID | Results |
|---|---|---|---|---|
| 0 / 0 | 463 ms | 751 ms | 288 ms | [A](../perf/results/2026-11-01-redmi-seed-A/) |
| 21 / 0 | 470 ms | 765 ms | 295 ms | [B](../perf/results/2026-11-01-redmi-seed-B/) |
| 0 / 30 | 482 ms | 781 ms | 298 ms | [C](../perf/results/2026-11-01-redmi-seed-C/) |
| 21 / 30 (baseline) | 453 ms | 735 ms | 282 ms | [baseline](../perf/results/2026-11-01-redmi-startup-day-one/) |

All four are within about 45 ms of each other, close to the Redmi's 40 ms noise. Even with an empty database, Today takes about 290 ms to load. The query fix I expected to need would have saved almost nothing, so it was dropped.

## Scrolling the routines list

`ScrollBenchmark.scrollRoutines`: `FrameTimingMetric`, 15 iterations, seeded with 21 days and 30 routines. Each iteration kills the app, launches it, opens Routines, then flings down and up. Only the flings are measured. The shader cache is kept, so this is a user's first scroll after opening the app.

It doesn't use `StartupMode.COLD`. COLD runs `setupBlock` *before* killing the app, so the list would be gone by the time the measure block runs, and it also clears the shader cache.

| Date | Device | Compilation state | Frames | CPU P50 / P90 / P99 | Overrun P50 / P90 / P99 | Results |
|---|---|---|---|---|---|---|
| 2026-11-01 | Redmi Note 10 Pro, 60 Hz | None (day one) | 1973 | 7.5 / 9.6 / 14.0 ms | −2.8 / +1.8 / +5.0 ms | [results](../perf/results/2026-11-01-redmi-scroll-day-one/) |

**The scroll is smooth on the Redmi even on day one.** Every frame's CPU time fits the 16.7 ms deadline at P99. About 20% of frames have a positive overrun, but FrameTimeline labels them all *Buffer Stuffing*: the app produced frames faster than the screen showed them, so they queued. That adds a little latency, but no frame is dropped. No frame is labelled *App Deadline Missed*. The scroll is smooth because the opening frame already paid for every row.

**Rhythm has no scroll test.** It shows one week, fits on one screen, and a fling draws no frames, so Macrobenchmark fails with "Observed no renderthread slices". Opening Rhythm is the journey to measure there.

## Opening Routines

`OpenScreenBenchmark.openRoutines`: the `Routines.open` async trace section, from the tab tap (`RituneApp`) to the first draw of the routines list (`endTraceOnFirstDraw`), read with `TraceSectionMetric(Mode.First)`. 15 iterations, seeded with 21 days and 30 routines. Each iteration kills the app, launches it and waits for Today; only the tap is measured.

| Date | Device | Compilation state | Median | P90 | Range | Results |
|---|---|---|---|---|---|---|
| 2026-11-01 | Redmi Note 10 Pro, 60 Hz | None (day one) | 316 ms | 342 ms | 281–343 ms | [results](../perf/results/2026-11-01-redmi-open-routines-day-one/) |

**One frame is most of it.** In the median trace (343 ms section), the frame that first shows the routines takes 217 ms on the main thread: recomposition 77 ms, measuring 100 ms, layout 15 ms, drawing 24 ms. That is about 13 missed frames at 60 Hz. The list is a `Column` with `verticalScroll`, so all 30 routines are composed and measured in that frame, including the ones off screen. The main thread runs for 300 of the 343 ms. The JIT runs for 160 ms alongside, because the app is at `verify`.

**The time grows in a straight line with the number of routines.** Same build, phone and state; only `seedRoutines` changes. The 7 built-in routines are always present.

| Routines in the list | Median | P90 | Results |
|---|---|---|---|
| 7 | 167 ms | 176 ms | [R0](../perf/results/2026-11-01-redmi-open-routines-R0/) |
| 37 | 308 ms | 334 ms | [R30](../perf/results/2026-11-01-redmi-open-routines-R30/) |
| 67 | 441 ms | 491 ms | [R60](../perf/results/2026-11-01-redmi-open-routines-R60/) |

Each extra 30 routines adds about 140 ms (+142, then +133): roughly **4.6 ms per routine** on top of about 135 ms of fixed cost. That straight line is what composing and measuring every row up front predicts. A `LazyColumn` composes only the visible rows, so its time should stay roughly flat. The R30 rerun (308 ms) is within 8 ms of the first run (316 ms). Phase 4 [fix 1](phase-4-fixes.md#fix-1-a-lazy-routines-list) made the list lazy and confirmed it.

## Opening Rhythm

`OpenScreenBenchmark.openRhythm`: the same method as opening Routines, with the `Rhythm.open` section ending on the first draw of the Rhythm content.

| Date | Device | Compilation state | Median | P90 | Range | Results |
|---|---|---|---|---|---|---|
| 2026-11-01 | Redmi Note 10 Pro, 60 Hz | None (day one) | 140 ms | 162 ms | 118–205 ms | [results](../perf/results/2026-11-01-redmi-open-rhythm-day-one/) |

**Rhythm's content frame is a quarter of Routines'.** In the median trace (162 ms section), the frame that first shows Rhythm takes 54 ms on the main thread: recomposition 17 ms, measuring 25 ms, layout 5 ms, drawing 7 ms. The rest is the same navigation frames Routines has (about 20, 16 and 15 ms). Loading the data takes only 3 ms on `DefaultDispatch`, while the JIT runs for 107 ms.

So the cost is the fixed price of running Compose and navigation code at `verify`, not data. Varying `seedDays` can change at most those 3 ms, below the noise, so that experiment was skipped. A Baseline Profile is the lever here.

## Opening a routine and its editor

`OpenRoutineBenchmark`: the `RoutineDetail.open` and `RoutineEditor.open` async sections, from the tap (`RoutineNavigation`) to the first draw of the step column. Seeded with 21 days, 30 routines and one extra 15-step routine (`longRoutine=true`), the worst case, since both screens build every step up front.

- `openDetail`: setup kills the app, launches it, opens Routines and scrolls to the 15-step routine. Only the tap on the routine is measured.
- `openEditor`: the same setup, then it also opens the Detail screen. Only the tap on Edit is measured.

| Date | Device | Compilation state | Journey | Median | P90 | Range | Results |
|---|---|---|---|---|---|---|---|
| 2026-11-01 | Redmi Note 10 Pro, 60 Hz | None (day one) | Open Detail | 171 ms | 188 ms | 147–195 ms | [results](../perf/results/2026-11-01-redmi-open-routine-detail-editor/) |
| 2026-11-01 | Redmi Note 10 Pro, 60 Hz | None (day one) | Open Editor | 172 ms | 189 ms | 152–196 ms | same folder |

**The totals match, but the costs differ.** The pattern held in all 15 iterations of each test:

| Median across iterations | Detail | Editor |
|---|---|---|
| Wait before the content frame | ~115 ms | ~67 ms |
| Content frame (main thread) | ~60 ms | ~107 ms |
| … recomposition / measuring | ~11 / ~29 ms | ~32 / ~43 ms |
| RenderThread `shader_compile` | ~29 ms, every iteration | 0 |
| JIT-compiled methods | ~72 | ~53 |

- **Editor's content frame is about 47 ms heavier.** Each Editor step row has a clickable column and two `TextButton`s; a Detail row has three `Text`s. For 15 steps that is 45 interactive nodes against none. (Phase 4 [fix 3](phase-4-fixes.md#fix-3-editor-rows-dropped-for-now) found that the rows themselves have no text fields; only the routine name and the step sheet do.)
- **Detail pays first-navigation costs that Editor doesn't.** Opening Detail is the first push navigation in the process, so it compiles shaders and JITs code for the first time. Editor's setup has already opened Detail, so that work is done before its section starts. In the user's journey that is true too: you always pass through Detail to reach the Editor.
- The frame cost hardly changes between iterations. The run-to-run spread comes from the wait before it.

So the two journeys point at different fixes. Editor's cost is per row, so building fewer rows up front (a lazy list) is the lever. Detail's is mostly first-use work, which a Baseline Profile targets; shader compilation is outside a Baseline Profile's reach.

## Starting a routine

`OpenRoutineBenchmark.openPlayer`: the `Player.open` async section, from the tap on Detail's Start button to the first draw of the player's content. Same seed and setup as opening the Editor; only the Start tap is measured. The section begins in the button, not in navigation, because loading the routine and starting the service happen before the app navigates. On Android 13+ the test grants `POST_NOTIFICATIONS` first, so the permission dialog doesn't cover the player.

| Date | Device | Compilation state | Median | P90 | Range | Results |
|---|---|---|---|---|---|---|
| 2026-11-01 | Redmi Note 10 Pro, 60 Hz | None (day one) | 151 ms | 168 ms | 115–179 ms | [results](../perf/results/2026-11-01-redmi-open-player-day-one/) |

**Starting a routine is faster than opening Detail (171 ms), even though it also starts a service.** Medians across the 15 iterations:

| | Start → player | Open Detail |
|---|---|---|
| Content frame | ~30 ms | ~60 ms |
| RenderThread `shader_compile` | 0 | ~29 ms |
| JIT-compiled methods | ~15 | ~72 |
| Main thread starting the service (`serviceCreate` + `serviceStart`) | ~19 ms, of which `Player.notify` ~4 ms | n/a |
| Main-thread binder calls (`startForegroundService` and others) | ~9 ms | n/a |

- **The service costs about 28 ms of main-thread time.** That is real, but smaller than what the player saves elsewhere.
- **The player screen is light and starts warm.** It has no list; setup has already opened Detail, so the shaders and shared code are ready.
- **The player draws an empty frame first.** `PlayerViewModel` starts its `stateIn` at `Loading`, and the first composition draws an empty `Box`. The content comes one frame later, about 30 ms after. That first frame also pays for `keepScreenOn` (`relayoutWindow`, ~8 ms). The engine's state is already a `StateFlow`, so the ViewModel could start from its current value and skip the empty frame.
- Cue sounds are decoded at the same time on `SoundDecoder` and `MediaCodec` threads, off the main thread.

## Player notification updates

`OpenRoutineBenchmark.skipSteps` reads `Player.notify` with `Average`, `Count` and `Max`. Setup starts the 15-step routine. The measured part taps Skip 5 times and waits each time for the next step's title. Steps last 60 s, so no step ends on its own. Session 3 ([results](../perf/results/2026-11-02-redmi-player-skip-steps/)):

| `Player.notify` | Median | P90 | Range |
|---|---|---|---|
| Average per call | 6.0 ms | 6.8 ms | 5.3–7.5 ms |
| Calls per iteration | 5 | 5 | 5 in every iteration |
| Slowest call | 7.5 ms | 11.2 ms | 5.9–12.6 ms |

- The count is 5 in every iteration, one per Skip, so the averages compare the same work.
- Each update takes about 6 ms on the main thread, and the slowest reach 12.6 ms. That is a large share of a 16.7 ms frame, and enough to drop a frame if one lands during an animation.

---

← Previous: [Phase 1: cold-start trace](phase-1-cold-start-trace.md) · Next: [Phase 3: Baseline Profiles](phase-3-baseline-profiles.md) → · [Performance home](../performance.md)
