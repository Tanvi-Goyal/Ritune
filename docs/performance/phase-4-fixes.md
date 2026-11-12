[← Performance home](../performance.md)

# Phase 4: deep analysis and fixes

> Written up in the series as [Part 3](https://medium.com/@goyaltanvi94/experiments-and-outcomes-improving-ritunes-performance-655b8002fd44).

**Status: done.** Six analysis topics were worked through first. They added no new fix candidates, so the candidates were the six found in Phases 1 to 3. Every one has been built and measured, or dropped with a reason, along with the ones the work uncovered on the way. Seven fixes are merged (#53 to #58 and #60). Two are dropped because their saving was below the noise: fix 3, the Editor rows, and fix 7, warming the date formats (its runs are kept, in #59). The [fix list](#fix-candidates-and-build-order) has the outcome of each one. A final A/B of `main` before and after Phase 4 ([#61](https://github.com/Tanvi-Goyal/Ritune/pull/61)) measured the result as a whole: cold-start TTFD −110 ms (−14%), and every screen open faster except the Editor, which is at the edge of the noise ([details](phase-4-before-and-after.md#phase-4-before-and-after)).

## Phase 4 analysis findings

| Topic | Result | Evidence | Fix candidate? |
|---|---|---|---|
| 4.1 PerfettoSQL across the Phase 2–3 traces | In Detail's open, the main thread works 74% of the time. The content frame measures 51 `Text`s in every iteration (median 24.3 ms, range 20.1–29.4 ms): 3 per step for 15 steps, plus 6 in the header. The spread between iterations comes from the frames before the content frame, not from it. | Phase 3 Open Detail, compiled, B1; `descendant_slice` on the content frame and `thread_state` over the section, all 15 traces | No new one. A lazy Detail saves only 4–7 ms. |
| 4.2 Compose stability and recomposition counts | Every row type (`RoutineItem`, `StepItem`) is stable. Only screen-level states holding a `List` are not, and under strong skipping the rows inside still skip. On the player, each second only the path from the state down to the countdown recomposes; the title and step progress skip. | Compose compiler reports (`ui-classes.txt`, release, 2026-11-05); Layout Inspector on the Redmi, debug build | No. A deferred `() -> Duration` read is noted, with no measured cost. |
| 4.3 `LazyColumn` vs `Column + verticalScroll` | A `Column` pays for every row when the screen opens; a `LazyColumn` pays only for visible rows and moves the rest into scrolling. That explains both Phase 2 results for Routines. | Phase 2 Routines open and scroll | Confirms candidate 1 (Routines) and keeps candidate 2 (Editor, with a text-field caveat). Detail and Today stay as they are. |
| 4.4 Heap dumps and LeakCanary | No leaks found across the journeys | LeakCanary 2.14, debug build, Redmi, 2026-11-05 | No |
| 4.5 StrictMode | 2 `DiskReadViolation`s during cold start, both in MIUI's theme-font check (`miui.util.font.FontSettings`), reached through `super.onCreate`. None from Ritune: no Room or DataStore access on the main thread. In release traces, all main-thread disk waits inside `activityStart` add up to 2.7 ms at the median. | Debug build with thread and VM policies, Redmi, 2026-11-05; Phase 3 cold, compiled, B1 traces | No: MIUI's code, and at most 2.7 ms |
| 4.6 R8 output (`r8-analyzer`) | Ritune has no keep rules of its own. Library and AGP rules keep about 1% of the code (optimization 98.72%, shrinking 99.10%, obfuscation 99.14%). The largest rule keeps every enum's `values()` and `valueOf()` (202 methods). | `:app:analyzeReleaseR8Config`, AGP 9.4.1, release, 2026-11-05 | No: a rule change would save less than the noise |

## Fix candidates and build order

Rows 1 to 4 are the candidates found in Phases 1 to 3, in build order. The rest came out of the work: the notification row (first listed as a "200 ms poll") and fix 5 (row 4's Today queries, split out) were added once the first fixes were measured, and fixes 6 to 8 came from traces taken along the way.

| Order | Candidate | Evidence | Lever | Branch | Measured with | Status |
|---|---|---|---|---|---|---|
| 1 | Routines list: `Column + verticalScroll` → `LazyColumn` with keys | `Routines.open` 316 ms; its content frame 217 ms; ~4.6 ms per routine | Compose and measure only the visible rows | `perf/routines-lazy-list` | Routines open at 7 and 67 routines; Routines scroll frame CPU P99 must stay under 16.7 ms | [Done](#fix-1-a-lazy-routines-list): −118 ms at 67 routines, [#53](https://github.com/Tanvi-Goyal/Ritune/pull/53) |
| 2 | Player draws an empty `Loading` frame first | ~30 ms empty frame before the content | Start `stateIn` from the engine's current `StateFlow` value | `perf/player-first-frame` | Start routine journey: the empty frame and `Player.open` | [Done](#fix-2-the-player-draws-its-content-on-the-first-frame): −12 ms, [#54](https://github.com/Tanvi-Goyal/Ritune/pull/54) |
| 3 | Editor step rows: lazy list or lighter rows | Editor content frame ~107 ms against Detail's ~60 ms; 45 interactive nodes for 15 steps | Per-row cost | `perf/editor-rows` | Editor open; re-check the evidence first, because of the text-field caveat | [Dropped for now](#fix-3-editor-rows-dropped-for-now): ~0–10 ms possible, below the noise |
| 4 | Startup critical path: ViewModels created lazily in the first `doFrame`, Today's queries starting only after the splash, and the Room open | Startup work begins 61 ms after `activityStart`; 83 ms between TTID and TTFD; Room open 27 of 34 ms, in parallel with `ThemeRead` | Create ViewModels in `onCreate`, start Today's queries during the splash, move the open off the critical path together with the theme read | `perf/startup-critical-path` | Cold startup TTID and TTFD | [Done](#fix-4-the-splashs-two-reads-off-the-main-thread): reads off main, −24 ms, [#55](https://github.com/Tanvi-Goyal/Ritune/pull/55). Today's queries became fix 5 |
| — | `PlayerService` notification updates on the main thread | ~6 ms per update on main, slowest 12.6 ms (Phase 2) | Collect the player state off main | `perf/player-notify-off-main` | Skip-steps journey: `Player.notify` on main, frame times | [Kept as cleanup](#notification-updates-off-the-main-thread): main freed, no frame faster, [#57](https://github.com/Tanvi-Goyal/Ritune/pull/57) |
| 5 | Today draws an empty `Loading` frame after the splash | ~130 ms between TTID and TTFD | Preload Today's first state during the splash | `perf/today-first-frame` | Cold startup TTFD | [Done](#fix-5-today-draws-its-content-on-the-first-frame): −75 ms TTFD, [#56](https://github.com/Tanvi-Goyal/Ritune/pull/56) |
| 6 | `InitializeApp` starts only when `MainViewModel` exists | Today's data ready at ~394 ms, after frame 1 composes at ~355 ms | Start it from `Application.onCreate` | `perf/startup-first-composition` | Cold startup TTFD; content in frame 1 | [Done](#fix-6-start-initializeapp-from-application): −43 ms TTFD, one frame fewer, [#58](https://github.com/Tanvi-Goyal/Ritune/pull/58) |
| 7 | Today's first composition loads the locale's date data on main | First `rememberTimeFormatter` 24–28 ms, ~17 ms of it on disk | Warm the date patterns off main during startup | `perf/today-date-formats` | Cold startup TTID and TTFD | [Dropped](#fix-7-warm-todays-date-formats-dropped): within noise; results only, [#59](https://github.com/Tanvi-Goyal/Ritune/pull/59) |
| 8 | Detail and Editor wait for their data | Detail: 3–9 frames with no content; every routine read to show one | Read one routine; start Detail's load in the view model | `perf/detail-editor-first-frame` | Detail and Editor open | [Done](#fix-8-detail-and-editor-load-sooner): Detail −12.7 ms, Editor −8.9 ms, [#60](https://github.com/Tanvi-Goyal/Ritune/pull/60) |

The Room open alone saves about 2 ms, because `ThemeRead` then becomes the critical path. It goes in only together with the theme read, or is dropped with that reason.

**The "200 ms poll" isn't a poll.** "The 200 ms poll → notification update churn in `PlayerService`" was an early Phase 4 candidate. Phase 2 showed `Player.notify` runs once per player state change, not per tick; the 200 ms loop in `ObserveRunProgress` only feeds the player screen's countdown. The main-thread part was still built, as [cleanup](#notification-updates-off-the-main-thread). The per-second work while a routine runs is a battery question that has not been measured yet.

**Method for every fix.** Its own branch from `origin/main`, tests as `AGENTS.md` requires, and an alternating A/B on the Redmi at the compiled state, the same method as Phase 3: A = the commit the branch started from on `main`, B = the branch, A1, B1, A2, B2 with 15 iterations per run (30 per side), results copied to `perf/results/` by [`perf/phase4-ab.sh`](../perf/phase4-ab.sh). If a fix changes code the Baseline Profile covers, the profile is regenerated on the branch, so B isn't measured with a profile written for A's code.

- **A is the branch's base, not the latest `main`.** `main` moved while fixes were measured (#54 merged during fix 4's runs), so the script checks A against `git merge-base HEAD origin/main`.
- **Each fix is read only against the A run beside it.** A itself moved between sessions: cold TTFD on A was 799 ms in one run and 831 ms in the next, the same day.
- **From the notification change on, each change has a 95% range**: a bootstrapped range of the difference between the two medians. A range that crosses zero is not a result.
- **Shader compilation is subtracted where it lands unevenly.** Macrobenchmark clears the shader cache before every cold start, but a real user keeps it, except on the first launch after an install or update. When a fix moves the shader work from one frame to another, the numbers are given with `shader_compile` subtracted, read from each iteration's trace, next to the raw ones.

## Fix results

Each fix adds a subsection here when it is measured: the hypothesis, the trace evidence, the change, and the before/after table with device, build, compilation state and results folder, then its PR.

### Fix 1: a lazy Routines list

**Hypothesis.** Opening Routines costs about 4.6 ms per routine because the list is a `Column` with `verticalScroll`: the first frame composes and measures every row, including the ones off screen ([Phase 2](phase-2-macrobenchmark.md#opening-routines)). A `LazyColumn` composes and measures only the rows on screen, so the open time should stay roughly flat as the list grows. With 7 routines all rows fit on screen, so no gain is expected there.

**Change** ([#53](https://github.com/Tanvi-Goyal/Ritune/pull/53), branch `perf/routines-lazy-list`):

- `ListBody` in `RoutinesListScreen` is a `LazyColumn` with keys (the routine id) and content types. The test tag and the `Routines.open` trace section stay on the list, so the benchmarks and the profile generator didn't change.
- A lazy list can't wrap its items in one card, because each item is laid out on its own. So each row draws its own slice of the card: `RituneCardSegment` and `CardSegment.of(index, count)` in `:core:designsystem`. Each slice draws the whole card's background and border, stretched past the edges it shares with its neighbours, then clips it to itself. The list still looks like one card.
- Tests: `RoutinesListContentTest` checks that 50 routines compose fewer than 50 rows and that the last one is still reachable by scrolling; `CardSegmentTest` covers the slices. The 11 Routines screenshot goldens were re-recorded: about 350 pixels per image differ by at most 7/255, on anti-aliased edges.
- The Baseline Profile was regenerated on the branch, so B wasn't measured with a profile written for A's code.

**Method.** Redmi Note 10 Pro, release key, benchmark build, compiled state (`compile=bgdexopt`; `speed-profile` checked before and after every run). A = `main` at `3fa8efd`, B = the branch. Alternating A1, B1, A2, B2, 15 iterations per run, 30 per side, in one session on 2026-11-06 (02:32 to 03:14 UTC). Opening uses `OpenScreenBenchmark.openRoutines` at 7 routines (`seedRoutines=0`) and 67 (`seedRoutines=60`); scrolling uses `ScrollBenchmark.scrollRoutines` at 30.

**Shader compilation is noise in these runs.** Every run starts from a fresh install, so the shader cache starts empty and fills at a different iteration in each run. `shader_compile` on the RenderThread adds 0 to 85 ms to an iteration, and the main thread waits for it. In the 7-routine runs it was 34 ms at the median for A and 1.9 ms for B, which alone made B look 21 ms faster. So the open time is compared with `shader_compile` subtracted, read from each iteration's trace, next to the raw medians.

| Open Routines, compiled, 30 iterations per side | A: `main` | B: lazy list | Change |
|---|---|---|---|
| 7 routines, raw median | 95.2 ms | 74.0 ms | shader noise, see above |
| 7 routines, minus `shader_compile` | 63.0 ms | 68.3 ms | +5 ms, within noise |
| 7 routines, content frame | 29.5 ms | 39.0 ms | +9.5 ms |
| 67 routines, raw median | 222.5 ms | 112.9 ms | −110 ms (−49%) |
| 67 routines, minus `shader_compile` | 195.3 ms | 77.5 ms | **−118 ms (−60%)** |
| 67 routines, content frame | 150.9 ms | 39.7 ms | −111 ms |
| Text measures in the content frame, 7 / 67 routines | 24 / 204 | 24 / 26 | |

| Scroll Routines (30 routines), compiled | A: `main` | B: lazy list |
|---|---|---|
| Frame CPU P50 / P90 / P99 | 5.1 / 8.8 / 12.8 ms | 5.1 / 8.4 / 12.8 ms |
| Frame overrun P99 | 5.7 ms | 5.7 ms |

Results: `perf/results/2026-11-06-redmi-p4-routines-lazy-list-*` (12 folders: 3 journeys × A1, B1, A2, B2). The raw medians above pool the two runs on each side.

**What it shows.**

- **The prediction held.** The lazy list's content frame stays at about 39 ms whether there are 7 or 67 routines, because it measures only the rows on screen: 26 text measures instead of 204.
- **At 67 routines, opening is 118 ms faster** without shader noise, and 110 ms faster raw.
- **At 7 routines there is no gain.** The content frame is about 10 ms slower, the cost of the lazy list's subcomposition when every row already fits. The whole open differs by 5 ms, inside the noise.
- **Scrolling didn't get worse.** Rows are now composed as they scroll on screen, and frame CPU stays well under the 16.7 ms deadline at P99.

Also in #53: `docs/perf/phase4-ab.sh`, an opt-in flag for Compose compiler reports (`-Pritune.composeReports=true`, used in analysis 4.2), and the StrictMode VM policy in debug builds (used in analysis 4.5).

### Fix 2: the player draws its content on the first frame

**Cause.** `PlayerViewModel` started its `stateIn` at `PlayerUiState.Loading`. The screen subscribes only after its first composition, so the first frame always drew an empty `Box`, even though the run had already started before navigation (`StartResult.Started` or `AlreadyRunning`, then `OpenPlayer`). Phase 2 saw the content arrive one frame later, about 30 ms after, at day one ([Starting a routine](phase-2-macrobenchmark.md#starting-a-routine)).

**Change** ([#54](https://github.com/Tanvi-Goyal/Ritune/pull/54), branch `perf/player-first-frame`):

- `ObserveRunProgress.current()` returns the progress right now from `player.state.value`, with the same rounding as the flow: both use one `progressOf()`.
- `PlayerViewModel` starts `stateIn` from `current()`. With nothing running that is still `Loading`, so closing the player behaves as before.
- Tests: `PlayerViewModelTest.running_isActiveBeforeTheScreenCollects` reads the state before anything collects it, which is the value the first frame draws. It fails without the fix. `ObserveRunProgressTest` adds `current_running_matchesTheFirstEmission` and `current_paused_reportsRemaining`.
- The Baseline Profile was regenerated on top of `main` after #53 merged, so the shipped profile covers both fixes.

**Method.** As fix 1: Redmi Note 10 Pro, release key, benchmark build, compiled (`speed-profile` checked before and after every run). `OpenRoutineBenchmark.openPlayer` measures the `Player.open` section, from the Start tap to the player's first content draw. A1, B1, A2, B2, 15 iterations per run, 30 per side, 2026-11-06, 04:18 to 04:43 UTC. A = `79f03b4`, the branch's base, which is `main` before #53; the Routines list isn't on this journey's measured path. `shader_compile` was 0 ms in every iteration, so no correction was needed.

| Open the player, compiled, 30 iterations per side | A: `79f03b4` | B: fix |
|---|---|---|
| Run 1 / run 2 median | 109.2 / 95.2 ms | 90.1 / 86.0 ms |
| Pooled median | 100.0 ms | **88.0 ms (−12 ms, −12%)** |
| Pooled P90 | 122.7 ms | 113.2 ms |
| Frames in the section | 5 in 16 of 30, 4 in 8, 6 in 5, 3 in 1 | 4 in 17 of 30, 3 in 11, 5 in 2 |
| Content frame median | 16.0 ms | 25.3 ms |

Results: `perf/results/2026-11-06-redmi-p4-player-first-frame-*` (4 folders).

**What it shows.**

- **The empty frame is gone.** The section usually has one frame fewer, and both B runs beat both A runs.
- **The gain is 12 ms, not the ~30 ms Phase 2 saw.** Phase 2 was day one, where code runs interpreted and every frame costs more.
- **The content frame is about 9 ms heavier**, because it now does the player's first composition, which used to be spread over the empty frame and the next one. The total still drops, because a whole frame's overhead (input, animation, traversal and the RenderThread sync) is gone.

### Fix 3: Editor rows (dropped for now)

**The evidence was re-checked first.** The Phase 2 caveat was wrong: the step rows have no text fields. Each row is a title, a duration and two `TextButton`s (↑ ↓). The only text fields are the routine name at the top and the step editor's bottom sheet. A routine has at most 20 steps (`RoutineValidator.MAX_STEPS`).

**Baseline on `main` (`d5b2d51`), compiled.** `OpenRoutineBenchmark.openEditor`, Redmi, release key, `speed-profile` before and after, the 15-step routine, 15 iterations, 2026-11-06.

| Open Editor, compiled, 15 steps | Value |
|---|---|
| `RoutineEditor.open` median (range) | 91.5 ms (78.0–102.0 ms) |
| Content frame median | 54.0 ms, no shader compilation |
| … recomposition / measuring / layout and draw (median iteration) | 16.4 / 27.8 / 7.4 ms |
| Text measures in the content frame | 69 (21.5 ms, ~0.31 ms each); 60 of them are in the 15 rows |
| Cost per row | ~2.2 ms (~30–35 ms for 15 rows) |

Results: [`perf/results/2026-11-06-redmi-p4-editor-rows-openEditor-compiled-baseline`](../perf/results/2026-11-06-redmi-p4-editor-rows-openEditor-compiled-baseline/).

**Why it was dropped.**

- **A lazy list saves about nothing at 15 steps.** About 10 of 15 rows fit on the Redmi's screen, so it would skip about 5 rows (~11 ms), and fix 1 measured about 10 ms of lazy-list overhead in the first frame. The net is about 0 at 15 steps and about 10 ms at the 20-step maximum.
- **Lighter move buttons might save about 10 ms**, using vector icons instead of the ↑ ↓ text glyphs (30 of the 69 text measures), but they change the editor's look.
- Both savings are below this run's spread (78 to 102 ms).
- Phase 2's 107 ms content frame was at day one. Compiling with the Baseline Profile halved it, to 54 ms.

It can come back if the Editor grows heavier rows or longer routines.

### Fix 4: the splash's two reads off the main thread

**Evidence, from the compiled traces first** (Phase 3's B runs, 30 cold starts, medians from launch). `activityStart` is at 265.6 ms and `onResume` ends at 328.0 ms. The splash's two reads, `InitializeApp` and `ThemeRead`, start at about 342 ms, 76 ms after `activityStart`. Both end at about 414 ms, about 72 ms each, in parallel. TTID is 492.2 ms and TTFD 688.4 ms. The reads start late because `by viewModels()` is lazy, and the splash condition first reads the view models at the first pre-draw. The Room open (the plan's "Room open" item) is part of `InitializeApp`.

It was built and measured in three steps, each against `d5b2d51`, the branch's base. Each step is read against the A run beside it, because A moved between runs.

| Step | What changed | TTID, A → B | TTFD, A → B | Result |
|---|---|---|---|---|
| 4a | `MainActivity` reads both view models right after `super.onCreate`, so they are created and start reading during `onCreate` | 501.4 → 505.1 ms | 817.0 → 827.7 ms | No gain |
| 4b | + `MainViewModel` runs `InitializeApp` and sets the start destination on `@Dispatcher(Default)` | 499.5 → 493.3 ms (traces) | 630.1 → 612.8 ms, shader compile subtracted | Reads fixed, startup barely moves |
| 4c | + `AppThemeViewModel` collects the settings on `viewModelScope + @Dispatcher(Default)` | 534.7 → 510.9 ms, shader compile subtracted | 664.4 → 640.0 ms, shader compile subtracted | **−24 ms (−4%) on both** |

Redmi Note 10 Pro, release key, compiled, `StartupBenchmark.coldStartup`, 30 cold starts per side, 2026-11-06 (05:28 to 06:30 UTC). Results: `perf/results/2026-11-06-redmi-p4-startup-critical-path-*` (4a), `...-startup-bg-init-*` (4b) and `...-startup-bg-reads-*` (4c).

- **4a: starting earlier made `InitializeApp` finish later.** The reads started 48 ms earlier, but `InitializeApp` ended 11 ms later (432.5 → 443.4 ms) and took 135 ms instead of 79 ms. Its coroutine ran on `viewModelScope`, which is `Dispatchers.Main.immediate`, so its last step (setting the start destination) queued behind `onResume` and the first frame. Starting the reads in `onCreate` also slowed the Activity's own startup: `onResume` ended 16 ms later, from CPU contention and a 10 ms lock contention on resources.
- **4b: the reads were fixed, but the splash wasn't what held the first frame.** Off main, `InitializeApp` took half as long (75.3 → 39.6 ms) and ended 82 ms earlier, at 342 ms. TTID moved 6 ms and TTFD 11 ms the other way, both within noise. The main thread's own work (`onResume`, then the first composition) ended at the same time either way.
- **4c: both reads off main saves about 24 ms.** `ThemeRead` dropped from 101.8 to 4.2 ms, and both reads ended at 344.5 ms, before `onResume` ended (349 ms). So the very first frame composes and draws the whole app.
- **Why raw TTID got 85 ms worse in 4c (525.8 → 611.1 ms).** That first frame now also pays the GPU's one-time costs: creating the EGL context and compiling shaders (101 ms of `shader_compile` before TTID). In A the same shaders compiled after TTID and before TTFD; the set is the same on both sides, about 134 ms, only moved earlier. With shader compile subtracted, TTID and TTFD both drop 24 ms, and raw TTFD agrees (830.8 → 807.2 ms).

Merged as #55. The PR has the 4c change, with tests in `MainViewModelTest` and `AppThemeViewModelTest`, and the `phase4-ab.sh` merge-base check.

### Notification updates off the main thread

**Evidence.** Phase 2 measured `Player.notify` at about 6 ms per update on the main thread, slowest 12.6 ms: enough to drop a frame if one lands during an animation.

**Change** (#57, `perf/player-notify-off-main`). `PlayerService` collects the player state on `@Dispatcher(Default)`. One collector keeps the updates in order, so a late update can't post the notification again after Finished or Idle removed it. The wake lock is taken and released under a lock, and not taken once `onDestroy` has cancelled the scope, so a late update can't hold it until its 4-hour timeout. `startForeground` stays on main, in `onStartCommand`. New instrumented test: skip 3 times, stop at once, and check the notification stays gone.

`OpenRoutineBenchmark.skipSteps`, read from the traces. A = `e49cc9d` (`main` with #55), 30 per side, 2026-11-06, 09:24 to 09:52 UTC. Results: `perf/results/2026-11-06-redmi-p4-player-notify-off-main-*`.

| Skip 5 steps, compiled | A | B | Change (95% range) |
|---|---|---|---|
| `Player.notify` on main | 5 calls, 31.6 ms | 0 | |
| `Player.notify` off main | 0 | 5 calls, 54.2 ms | |
| Longest main-thread frame | 19.4 ms | 18.4 ms | −2.4 to +1.6 ms |
| Janky frames per run | 83 | 79 | −37 to +22 |
| Longest frame | 32.2 ms | 33.0 ms | |

**The main thread is freed, but no frame got faster.** In this journey the updates ran between frames, not inside one. Off main they take longer, because they share cores with the rest of the app. The measured recommendation was to drop it, since it adds threading to the service for no measured gain. Tanvi kept it as main-thread cleanup.

### Fix 5: Today draws its content on the first frame

**Cause.** `TodayViewModel` started its `stateIn` at `TodayUiState.Loading`, and Today draws nothing while loading. Today is the start destination, so every cold start went splash, an empty Today frame, then the content: about 130 ms between TTID and TTFD after #55. Fix 4's look at the Phase 3 traces had already split that gap: the Loading frame (37 ms), the main thread blocked 66 ms on the RenderThread's first draw, and Today's content frame (59 ms). The queries themselves used only about 22 ms of background CPU, so starting them earlier alone would save little; the empty frame had to go.

**Change** (#56, `perf/today-first-frame`):

- A new `@Singleton TodayStateSource` in `:feature:today:ui` holds Today's flow, moved out of `TodayViewModel` unchanged.
- `MainViewModel` calls `preload()` after `InitializeApp`, off main, in a new `TodayPreload` trace section. The splash now also waits for Today's first state.
- `TodayViewModel` starts `stateIn` from `takePreloaded()`. It is handed out once, so a later Today screen starts from `Loading` as before and never shows an old copy.
- `StartupBenchmark` records the `TodayPreload` section.
- Tests: the preloaded state is the value the first frame reads (fails without the fix), it is handed out once, and the start destination comes with Today preloaded.

`StartupBenchmark.coldStartup`, A = `e49cc9d`, 30 cold starts per side, 2026-11-06, 09:52 to 10:03 UTC. Results: `perf/results/2026-11-06-redmi-p4-today-first-frame-*`.

| Cold start, compiled | A | B | Change (95% range) |
|---|---|---|---|
| TTFD median / P90 | 766.2 / 798.9 ms | 691.0 / 731.8 ms | **−75.2 ms** (−112 to −42) |
| TTFD minus `shader_compile` | 605.7 ms | 541.6 ms | −64.1 ms (−95 to −35) |
| TTID median / P90 | 565.3 / 599.8 ms | 576.7 / 698.1 ms | +11.4 ms (−20 to +90) |
| TTID minus `shader_compile` | 477.0 ms | 515.8 ms | +38.7 ms (−14 to +75) |
| `TodayPreload` | | 37.9 ms | |

**Content shows 64 to 75 ms sooner, about 10%: the largest startup gain in Phase 4.** TTID and TTFD now nearly meet, because the first frame is the content. TTID moves later, as expected: the splash waits for the preload (38 ms) and the first frame is heavier. That is within noise at the median and about 100 ms at P90.

### Fix 6: start `InitializeApp` from `Application`

**Evidence, from a diagnostic run first.** Trace sections were added to `MainActivity.onCreate` for this run only (15 cold starts, on top of #56; `perf/results/2026-11-06-redmi-p4-first-composition-coldStartup-compiled-diag`). `onCreate` itself is small: `installSplashScreen` 25 ms (the window decor, which `setContent` would pay anyway), `super.onCreate` with Hilt 4 ms, the view models 4.5 ms, `setContent` 2 ms. The time was elsewhere. Medians from launch: `Application.onCreate` at 252 ms; `InitializeApp` from 324 to 358 ms, because it starts only when `MainViewModel` exists; `TodayPreload` ends at 394 ms. Frame 1 composes at about 355 ms, before the data, so it draws empty, and Today's content waits for frame 2.

**Change** (#58, `perf/startup-first-composition`):

- A new `@Singleton AppStartup` in `:app` runs `InitializeApp` once per process on `@Dispatcher(Default)`. `RituneApplication.onCreate` calls `start()`.
- `MainViewModel` awaits it, then runs `TodayPreload` as before. It is a lazy `Deferred`, so an await without `start()` (tests, other processes) still starts it.
- Today's preload stays in the activity. Started from `Application`, a background process start could leave a stale copy for hours.
- Tests: `AppStartupTest` (initializes once, runs before anyone waits, and an await without `start()` initializes and waits) and the `MainViewModelTest` update.

`StartupBenchmark.coldStartup`, A = `7cabb24` (`main` with #56), 30 cold starts per side, 2026-11-06, 10:43 to 10:54 UTC. Results: `perf/results/2026-11-06-redmi-p4-startup-app-init-*`.

| Cold start, compiled | A | B | Change (95% range) |
|---|---|---|---|
| `InitializeApp` starts | 298.5 ms | 246.4 ms | |
| Today's data ready (`TodayPreload` ends) | 377.2 ms | 332.9 ms | |
| Content drawn in frame 1 | 0 of 30 | 27 of 30 | |
| TTFD median / P90 | 717.7 / 789.3 ms | 674.3 / 716.4 ms | −43.3 ms (−70 to +3) |
| TTFD from traces, minus `shader_compile` | 551.3 ms | 479.8 ms | **−71.4 ms** (−90 to −47) |
| TTID median / P90 | 665.1 / 756.1 ms | 665.2 / 702.7 ms | +0.1 ms (−27 to +41) |

**One frame drops out of startup.** With `InitializeApp` started 52 ms earlier, Today's data is ready before frame 1 composes in 27 of 30 starts, so the content draws in the first frame. The benchmark's TTFD range just touches zero; the trace measure of when the content composes is clear at −71 ms, and P90 TTFD drops 73 ms.

### Fix 7: warm Today's date formats (dropped)

**Evidence, from composition tracing.** A diagnostic build with `runtime-tracing` in the app and `androidx.benchmark.fullTracing.enable=true` (15 cold starts on top of #58; `perf/results/2026-11-06-redmi-p4-today-first-composition-coldStartup-compiled-diag`). Composition tracing adds its own overhead, so these are proportions, not startup times.

| Composable | Median | Range |
|---|---|---|
| `RituneNavHost` | 58.4 ms | 39–77 ms |
| `TodayScreen` | 48.8 ms | 31–61 ms |
| `TodayContent` | 27.7 ms | 16–36 ms |
| `rememberTimeFormatter` (2 calls) | 22.5 ms | 10–29 ms |
| `SummaryCard` | 14.3 ms | 6–20 ms |
| `AnchorItemRow` (6 rows) | 5.9 ms | 5–11 ms |

The first `rememberTimeFormatter` call takes 24 to 28 ms, and the second 0.6 ms. In one iteration the first call spends 16.9 ms waiting on disk and 7.4 ms running: `DateFormat.getBestDateTimePattern` loading the locale's ICU data for the first time, on the main thread, inside Today's first composition.

**Change tried.** `AppStartup.start()` also ran `getBestDateTimePattern` and `DateTimeFormatter.ofPattern` for Today's three skeletons (`hm`, `Hm`, `EEEEdMMMM`) on `@Dispatcher(Default)`, in a `WarmDateFormats` section. The splash didn't wait for it. Predicted: about 20 ms off the content frame.

`StartupBenchmark.coldStartup`, A = `19f6e19` (`main` with #57 and #58), 30 cold starts per side, 2026-11-08. Results: `perf/results/2026-11-08-redmi-p4-today-date-formats-*`.

| Cold start, compiled | A | B | Change (95% range) |
|---|---|---|---|
| TTID median / P90 | 605.6 / 659.8 ms | 618.5 / 641.9 ms | +12.9 ms (−10.5 to +48.9) |
| TTFD median / P90 | 616.2 / 689.3 ms | 618.5 / 641.9 ms | +2.3 ms (−36.7 to +15.0) |
| `reportFullyDrawn` in the trace | 465.2 ms | 450.2 ms | −15.0 ms (−42.6 to +1.2) |
| Frame 1 on the main thread | 272.9 ms | 278.5 ms | +5.6 ms (−11.1 to +33.1) |
| Frame 1 waiting on disk | 20.0 ms | 16.0 ms | −4.1 ms (−15.6 to +4.8) |

**No measurable gain, so the code was dropped.** The warm-up ran as built: 26.7 ms off the main thread, finished before frame 1 in 30 of 30 starts. It did reach the main thread: the first text measure fell from 22.9 to 11.9 ms, and its disk wait from 14.6 to 4.6 ms. But in the same session creating the EGL context took 42.1 ms on B against 34.5 ms on A, and frame 1 waits for it, so the saving didn't show through frame 1's variation. Every range crosses zero. #59 commits the runs and the diagnostic, with no code.

**Startup is bimodal now.** When frame 1 draws only the splash, TTID is about 440 to 460 ms and TTFD about 660 to 690 ms. When frame 1 draws Today, TTID equals TTFD at about 550 to 775 ms, because that frame carries Today's composition (about 35 to 40 ms), the first text and font loads, the EGL context (about 35 to 42 ms) and the shader compiles Macrobenchmark forces on every cold start.

**Today's first composition is closed.** A second composition-tracing run on `main` (`19f6e19`, 15 cold starts; `perf/results/2026-11-08-redmi-p4-today-composition-main-coldStartup-compiled-diag`, committed in #60) put `TodayScreen`'s first composition at 23.4 ms (21.6–24.2), with no disk wait, against 48.8 ms in the first diagnostic. Inside it: `rememberTimeFormatter` 7.9 ms (CPU only now), the first `pluralStringResource` 3.4 ms, the first `stringResource` 3.1 ms, `TodayUi` 2.8 ms, and everything else under 1 ms each. Nothing there is worth a fix, so Today's startup work ends here.

### Fix 8: Detail and Editor load sooner

**Cause.** Like the player before fix 2, Detail and Editor draw `Loading` first. In the Phase 3 Detail traces the wait is longer than one frame: 3 to 9 frames with no content, and the content frame starts 70 to 110 ms after the tap. Two causes:

- `ObserveRoutine` read every routine with all its steps and cues (`observeAll()`), then kept the one it needed. The benchmark seeds about 38 routines. That took about 10 ms at the median on a background thread, up to 36 ms.
- Detail's `stateIn` used `WhileSubscribed`, so its query started only when the screen's `collectAsStateWithLifecycle` subscribed, after the screen's second frame, about 52 ms after the tap. The view model exists by about 10 ms. Editor already loads from its `init`.

**Change** (#60, `perf/detail-editor-first-frame`):

1. **One routine per query.** `RoutineDao.observe(id)` and `RoutineRepository.observe(id)` read one routine with its steps, and `ObserveRoutine` uses them, so both screens benefit. `FakeRoutineRepository` gains `observe`. Tests: `RoutineDaoTest` checks that `observe` returns only that routine with its steps, and null for an unknown id.
2. **Detail loads from its view model.** `RoutineDetailViewModel` uses a `SharingStarted` that starts when the view model is created, then behaves like `WhileSubscribed(5_000)`, so it still stops 5 s after the screen stops collecting. `RoutineDetailViewModelTest.state_loadsBeforeTheScreenCollects` reads `state.value` with no collector; it fails without the change.

A = `19f6e19`, 30 opens per side, 2026-11-08. Change 1 alone was measured on both screens (`perf/results/2026-11-08-redmi-p4-routine-single-query-*`), then both changes on Detail (`...-detail-early-load-*`, 06:26 to 06:50 UTC).

| Open, compiled | A | B | Change (95% range) |
|---|---|---|---|
| Detail, change 1 only, median / P90 | 113.1 / 129.3 ms | 106.4 / 120.9 ms | −6.7 ms (−14.6 to −0.9) |
| Detail, both changes, median / P90 | 113.0 / 130.6 ms | 100.3 / 115.9 ms | **−12.7 ms, −11%** (−21.6 to −4.0) |
| Detail, both, minus `shader_compile` | 84.2 ms | 71.8 ms | −12.4 ms (−22.1 to −4.2) |
| Detail frames before content, both | 6 | 4 | −2 |
| Detail query starts, both | 57.4 ms | 34.6 ms | −22.8 ms (−27.9 to −17.2) |
| Editor, change 1, median / P90 | 95.9 / 106.7 ms | 87.0 / 107.2 ms | −8.9 ms (−13.8 to −2.6) |
| Background work for the query, Detail / Editor, change 1 | 9.6 / 7.7 ms | 5.5 / 2.7 ms | |

**Detail shows content two frames earlier, and the Editor about 9 ms sooner.** Detail's query still starts at about 35 ms, not when the view model is created (about 10 ms), probably because the collection runs on the main thread, which is busy with the tap's first frames. That wasn't checked in the trace. P90 drops 15 ms.

---

← Previous: [Phase 3: Baseline Profiles](phase-3-baseline-profiles.md) · Next: [Phase 4 before and after](phase-4-before-and-after.md) → · [Performance home](../performance.md)
