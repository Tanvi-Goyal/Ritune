# Performance

Ritune measures performance before it changes anything. Every result in this file says what device it ran on, which build, which compilation state and how many iterations, so anyone can reproduce it.

This file has three parts:

1. **[Methodology](#methodology)**: how measurements are taken.
2. **[Results](#results)**: numbers, newest first, one section per phase.
3. **[Commands](#commands)**: copy-paste steps to reproduce each measurement.

## Methodology

### Device

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

### Build

| Property | Value |
|---|---|
| Build type | `release`, with R8 optimization on |
| Signing | Release keystore, from the `RITUNE_KEYSTORE_*` environment variables |
| Debuggable | No. A debuggable build turns off many ART optimizations, so debug timings are never published. |
| Commit | The commit hash goes next to each result |

### Compilation state

Startup speed depends heavily on how much of the app is compiled to machine code, so every result names its state.

| State | How it is set | Real user it simulates |
|---|---|---|
| None (day one) | `adb shell cmd package compile -m verify -f com.ritune`, or `CompilationMode.None()` | First launch after an install or update, with no profile |
| Baseline Profile | `CompilationMode.Partial(BaselineProfile.Require)` (from Phase 3) | First launch, with a shipped Baseline Profile |
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

### Conditions

- Phone on charge and cool to the touch. Rest it between suites to avoid thermal throttling.
- Brightness fixed, auto-rotate off, Wi-Fi on, other apps force-stopped.
- Same app data for every run. Record whether the database was freshly seeded.

### Iterations and reporting

- Startup: at least 10 runs for a manual check, and 20–30 when claiming an improvement.
- Report the median (P50) with the min–max range. Frame metrics also report P90 and P99.
- A change counts as an improvement only if it is larger than the run-to-run spread. To isolate one piece of work, measure its trace section directly as well as the end-to-end number.
- Report end-to-end changes as the headline, and section changes as the explanation. Only work on the critical path changes what the user feels.

### Metrics

| Metric | Meaning | Source |
|---|---|---|
| TTID | Time until `MainActivity` draws its first frame. Includes everything the splash waits for (`InitializeApp` → `DatabaseSeeder`, and the theme DataStore read). | `TotalTime` from `am start -W`, or `timeToInitialDisplayMs` |
| TTFD | Time until Today leaves its `Loading` state, reported by `ReportDrawnWhen` in `TodayScreen` | The `reportFullyDrawn()` slice in a trace, or `timeToFullDisplayMs` (Phase 2) |
| Frame overrun | How far each frame finished past its deadline (negative means early) | `FrameTimingMetric` (Phase 2) |

### Trace sections

Ritune writes these sections into system traces. Their names are stable, because queries and benchmarks look them up by name.

| Section | Where | Type | Measures |
|---|---|---|---|
| `InitializeApp` | `MainViewModel` | Async | Startup initialization, which the splash waits on |
| `ThemeRead` | `AppThemeViewModel` | Async, ends at the first value | The first theme read, which the splash also waits on |
| `Seed.anchors`, `Seed.routines` | `DatabaseSeeder`, inside the write transaction | Async | Counting, and seeding when empty |
| `Player.notify` | `PlayerService` | Sync | Each ongoing-notification rebuild during a run |

Sections around `suspend` calls must be async: a sync section would be closed by whichever thread the coroutine resumes on.

## Results

### Phase 1: cold-start trace

One cold start per phone, recorded with [`perf/cold-start.pftxt`](perf/cold-start.pftxt) and summarised with [`perf/cold-start.sql`](perf/cold-start.sql). Release build, 2026-10-30. Compilation state: Redmi `speed-profile` (MIUI compiled it after the install), Pixel `verify`, so compare structure across phones, not absolute times.

| Milestone (ms from launch) | Redmi Note 10 Pro | Pixel 9 Pro |
|---|---|---|
| `bindApplication` starts | 144.4 | 72.0 |
| `activityStart` starts | 189.8 | 85.7 |
| `activityResume` ends | 237.9 | 107.4 |
| `ThemeRead` | 250.5 → 284.2 (33.7) | 115.6 → 124.0 (8.4) |
| `InitializeApp` | 252.5 → 286.4 (33.9) | 116.5 → 127.9 (11.4) |
| `Seed.anchors` starts | 279.5 | 127.1 |
| TTID | 356.7 | 151.8 |
| TTFD | 440.0 | 198.9 |
| TTID frame | 64.2 ms (3 missed at 60 Hz) | 20.3 ms (2 missed at 120 Hz) |

![Redmi cold start: startup, main thread and Ritune's sections](perf/phase1-redmi-startup.png)

![Redmi InitializeApp: Room opening before the seeding sections](perf/phase1-redmi-initializeapp.png)

Findings:

- **Opening the Room database is most of `InitializeApp`:** 27 of 34 ms on the Redmi, 10.6 of 11.4 ms on the Pixel. Seeding an already-seeded database takes under 1 ms.
- **The startup work begins late.** Both ViewModels are created during the first `Choreographer#doFrame`, 61 ms (Redmi) and 30 ms (Pixel) after `activityStart` begins, because `by viewModels()` is lazy.
- **`ThemeRead` and `InitializeApp` take about the same time**, so shortening only one saves at most the 2–4 ms between them.
- **Today adds 83 ms (Redmi) and 47 ms (Pixel) between TTID and TTFD.** Its queries only start once Today is composed, after the splash ends.
- **The TTID frame is the most expensive frame:** 64 ms on the Redmi, half of it in `measure`, for the first composition of the app shell and Today.
- **40% of the Redmi's TTID passes before `bindApplication`**, while the system creates the process.

### Phase 0: manual cold start

The first number before any instrumentation exists. TTID only; TTFD comes in Phase 1.

| Date | Commit | Device | Compilation state | Runs | TTID median | TTID range |
|---|---|---|---|---|---|---|
| 2026-10-30 | `21a33c7` | Redmi Note 10 Pro | None (day one) | 10 | 377 ms | 349–607 ms |
| 2026-10-30 | `21a33c7` | Redmi Note 10 Pro | Speed profile (`bg-dexopt`) | 10 | 321 ms | 306–497 ms |
| 2026-10-29 | `4c50885` | Pixel 9 Pro | None (day one) | 10 | 133 ms | 125–151 ms |
| 2026-10-29 | `4c50885` | Pixel 9 Pro | Speed profile | 10 | 115.5 ms | 106–130 ms |

### Phase 2: Macrobenchmark cold start

`StartupBenchmark.coldStartup`: `StartupMode.COLD`, `CompilationMode.Ignore()`, 15 iterations. The database is seeded once before the run with 21 days of anchor events and 30 routines. TTFD is when Today first shows loaded data.

These numbers are not comparable with Phase 0: the method is different, and the database holds far more data.

| Date | Commit | Device | Compilation state | TTID median (P90) | TTFD median (P90) | Results |
|---|---|---|---|---|---|---|
| 2026-11-01 | `277e6a2` | Redmi Note 10 Pro, 60 Hz | None (day one) | 453 ms (494) | 735 ms (764) | [results](perf/results/2026-11-01-redmi-startup-day-one/) |
| 2026-11-01 | `277e6a2` | Pixel 9 Pro, adaptive | None (day one) | 162 ms (174) | 245 ms (271) | [results](perf/results/2026-11-01-pixel-startup-day-one/) |
| 2026-11-01 | `0afdb46` | Redmi Note 10 Pro, 60 Hz | Speed profile (`bg-dexopt`) | 431 ms (460) | 617 ms (652) | [results](perf/results/2026-11-01-redmi-startup-speed-profile/) |

**Compilation state matters more than data.** On the Redmi, speed-profile cuts the TTFD median by 118 ms (735 → 617), three times the noise. TTID drops by only 22 ms. Most of the gain is in loading Today, which runs code for the first time in the process. A Baseline Profile would give day-one installs this compiled state.

**Macrobenchmark clears the shader cache before each cold start** (`MacrobenchmarkScope.dropShaderCache`, through `profileinstaller`). Every iteration therefore compiles its shaders again. In the Redmi speed-profile median trace, the first frame after TTID waits 67 ms on RenderThread `shader_compile`. Real users pay this only after installing or updating the app; later launches reuse the cache.

**Data size is not why Today is slow on the Redmi.** The time from first frame to fully drawn is 282 ms on the Redmi and 83 ms on the Pixel; the Phase 1 trace showed 83 ms on the Redmi. Changing only the seed size, at commit `0afdb46`, moved TTFD by less than the noise:

| Seed (days / routines) | TTID median | TTFD median | TTFD − TTID | Results |
|---|---|---|---|---|
| 0 / 0 | 463 ms | 751 ms | 288 ms | [A](perf/results/2026-11-01-redmi-seed-A/) |
| 21 / 0 | 470 ms | 765 ms | 295 ms | [B](perf/results/2026-11-01-redmi-seed-B/) |
| 0 / 30 | 482 ms | 781 ms | 298 ms | [C](perf/results/2026-11-01-redmi-seed-C/) |
| 21 / 30 (baseline) | 453 ms | 735 ms | 282 ms | [baseline](perf/results/2026-11-01-redmi-startup-day-one/) |

All four are within about 45 ms of each other, close to the Redmi's 40 ms noise. Even with an empty database, Today takes about 290 ms to load.

**Noise.** Each phone ran the same build twice at the same state (an A/A test):

| Medians, run 1 → run 2 | TTID | TTFD | Change must exceed |
|---|---|---|---|
| Redmi | 467 → 453 ms | 771 → 735 ms | ~15 ms TTID, ~40 ms TTFD |
| Pixel ([second run](perf/results/2026-11-01-pixel-startup-day-one-aa/)) | 162.5 → 162.4 ms | 245 → 240 ms | ~5 ms TTID, ~10 ms TTFD |

The Redmi is far noisier than the Pixel. To prove smaller changes, alternate builds A, B, A, B with 30+ iterations per run.

### Phase 2: scrolling the routines list

`ScrollBenchmark.scrollRoutines`: `FrameTimingMetric`, 15 iterations, seeded with 21 days and 30 routines. Each iteration kills the app, launches it, opens Routines, then flings down and up. Only the flings are measured. The shader cache is kept, so this is a user's first scroll after opening the app.

It doesn't use `StartupMode.COLD`. COLD runs `setupBlock` *before* killing the app, so the list would be gone by the time the measure block runs, and it also clears the shader cache.

| Date | Device | Compilation state | Frames | CPU P50 / P90 / P99 | Overrun P50 / P90 / P99 | Results |
|---|---|---|---|---|---|---|
| 2026-11-01 | Redmi Note 10 Pro, 60 Hz | None (day one) | 1973 | 7.5 / 9.6 / 14.0 ms | −2.8 / +1.8 / +5.0 ms | [results](perf/results/2026-11-01-redmi-scroll-day-one/) |

**The scroll is smooth on the Redmi even on day one.** Every frame's CPU time fits the 16.7 ms deadline at P99. About 20% of frames have a positive overrun, but FrameTimeline labels them all *Buffer Stuffing*: the app produced frames faster than the screen showed them, so they queued. That adds a little latency, but no frame is dropped. No frame is labelled *App Deadline Missed*.

**Rhythm has no scroll test.** It shows one week, fits on one screen, and a fling draws no frames, so Macrobenchmark fails with "Observed no renderthread slices". Opening Rhythm is the journey to measure there.

### Phase 2: opening Routines

`OpenScreenBenchmark.openRoutines`: the `Routines.open` async trace section, from the tab tap (`RituneApp`) to the first draw of the routines list (`endTraceOnFirstDraw`), read with `TraceSectionMetric(Mode.First)`. 15 iterations, seeded with 21 days and 30 routines. Each iteration kills the app, launches it and waits for Today; only the tap is measured.

| Date | Device | Compilation state | Median | P90 | Range | Results |
|---|---|---|---|---|---|---|
| 2026-11-01 | Redmi Note 10 Pro, 60 Hz | None (day one) | 316 ms | 342 ms | 281–343 ms | [results](perf/results/2026-11-01-redmi-open-routines-day-one/) |

**One frame is most of it.** In the median trace (343 ms section), the frame that first shows the routines takes 217 ms on the main thread: recomposition 77 ms, measuring 100 ms, layout 15 ms, drawing 24 ms. That is about 13 missed frames at 60 Hz. The list is a `Column` with `verticalScroll`, so all 30 routines are composed and measured in that frame, including the ones off screen. The main thread runs for 300 of the 343 ms. The JIT runs for 160 ms alongside, because the app is at `verify`.

**The time grows in a straight line with the number of routines.** Same build, phone and state; only `seedRoutines` changes. The 7 built-in routines are always present.

| Routines in the list | Median | P90 | Results |
|---|---|---|---|
| 7 | 167 ms | 176 ms | [R0](perf/results/2026-11-01-redmi-open-routines-R0/) |
| 37 | 308 ms | 334 ms | [R30](perf/results/2026-11-01-redmi-open-routines-R30/) |
| 67 | 441 ms | 491 ms | [R60](perf/results/2026-11-01-redmi-open-routines-R60/) |

Each extra 30 routines adds about 140 ms (+142, then +133): roughly **4.6 ms per routine** on top of about 135 ms of fixed cost. That straight line is what composing and measuring every row up front predicts. A `LazyColumn` composes only the visible rows, so its time should stay roughly flat. The R30 rerun (308 ms) is within 8 ms of the first run (316 ms).

### Phase 2: opening Rhythm

`OpenScreenBenchmark.openRhythm`: the same method as opening Routines, with the `Rhythm.open` section ending on the first draw of the Rhythm content.

| Date | Device | Compilation state | Median | P90 | Range | Results |
|---|---|---|---|---|---|---|
| 2026-11-01 | Redmi Note 10 Pro, 60 Hz | None (day one) | 140 ms | 162 ms | 118–205 ms | [results](perf/results/2026-11-01-redmi-open-rhythm-day-one/) |

**Rhythm's content frame is a quarter of Routines'.** In the median trace (162 ms section), the frame that first shows Rhythm takes 54 ms on the main thread: recomposition 17 ms, measuring 25 ms, layout 5 ms, drawing 7 ms. The rest is the same navigation frames Routines has (about 20, 16 and 15 ms). Loading the data takes only 3 ms on `DefaultDispatch`, while the JIT runs for 107 ms.

So the cost is the fixed price of running Compose and navigation code at `verify`, not data. Varying `seedDays` can change at most those 3 ms, below the noise, so that experiment was skipped. A Baseline Profile is the lever here.

## Commands

### Manual cold start (Phase 0)

Measure the same artifact users get: a release APK signed with the release key. Build it one of two ways.

**From the command line** (reproducible, and matches `release.yml`). Gradle reads the signing details from `RITUNE_KEYSTORE_PATH`, `RITUNE_KEYSTORE_PASSWORD`, `RITUNE_KEY_ALIAS` and `RITUNE_KEY_PASSWORD`. Without them it builds `app-release-unsigned.apk`, which won't install.

```bash
./gradlew :app:assembleRelease
APK=app/build/outputs/apk/release/app-release.apk
```

**From Android Studio** (Build > Generate Signed App Bundle or APK > APK > release). The wizard writes to `app/release/`:

```bash
APK=app/release/app-release.apk
```

Before measuring, confirm the APK is release-signed and not debuggable:

```bash
BT=~/Library/Android/sdk/build-tools/36.0.0
$BT/apksigner verify --print-certs $APK | grep "certificate DN"   # expect CN=Tanvi Goyal, OU=Ritune
$BT/aapt dump badging $APK | grep debuggable                      # expect no output
adb install -r $APK
```

If the install fails with `INSTALL_FAILED_UPDATE_INCOMPATIBLE`, a debug build is on the phone. Run `adb uninstall com.ritune` first.

Day one (no compiled code):

```bash
adb shell cmd package compile -m verify -f com.ritune

for i in $(seq 1 10); do
  adb shell am force-stop com.ritune; sleep 3
  adb shell am start -W -n com.ritune/.MainActivity | grep TotalTime
done
```

After several launches, compile from the recorded profile and repeat the loop:

```bash
adb shell cmd package compile -m speed-profile -f com.ritune
```

`force-stop` before each launch makes every run a cold start.

### Both phones at once

With more than one phone connected, adb needs a serial (`-s`) on every command. A phone on wireless debugging shows `ip:port`, and the port changes each time you reconnect, so look phones up by codename:

```bash
REDMI=$(adb devices -l | awk '/sweetin/{print $1}')
PIXEL=$(adb devices -l | awk '/caiman/{print $1}')

for s in $REDMI $PIXEL; do
  adb -s $s install -r $APK
done

coldstart() {  # usage: coldstart <serial> <label>
  adb -s "$1" shell cmd package compile -m verify -f com.ritune
  for i in $(seq 1 10); do
    adb -s "$1" shell am force-stop com.ritune; sleep 3
    adb -s "$1" shell am start -W -n com.ritune/.MainActivity | grep TotalTime | sed "s/^/$2 /"
  done
}

coldstart $REDMI redmi & coldstart $PIXEL pixel & wait
```

Running both phones in parallel is safe: `TotalTime` is measured on each phone, so the shared Mac and USB connection don't affect it. `TotalTime` is TTID in milliseconds.

### Cold-start trace (Phase 1)

Records one cold start into a Perfetto trace, on one phone or both at once. It uses Perfetto's official [`record_android_trace`](https://perfetto.dev/docs/quickstart/android-tracing) script; download it once:

```bash
mkdir -p ~/.local/share/perfetto-tools
curl -o ~/.local/share/perfetto-tools/record_android_trace \
  https://raw.githubusercontent.com/google/perfetto/main/tools/record_android_trace
chmod +x ~/.local/share/perfetto-tools/record_android_trace
```

Record, after installing the release APK (see above):

```bash
REC=~/.local/share/perfetto-tools/record_android_trace
CFG=docs/perf/cold-start.pftxt
OUT=~/perfetto-traces/ritune; mkdir -p $OUT

coldtrace() {   # usage: coldtrace <serial> <name>
  adb -s $1 shell dumpsys package dexopt | grep -A2 "\[com.ritune\]" | grep -o "status=[a-z-]*" | sed "s/^/$2 /"
  adb -s $1 shell am force-stop com.ritune
  $REC -c $CFG --serial $1 -n -o $OUT/$2-cold-start.perfetto-trace &
  sleep 3   # give tracing time to start
  adb -s $1 shell am start -W -n com.ritune/.MainActivity | grep TotalTime | sed "s/^/$2 /"
  wait
}

coldtrace $REDMI redmi & coldtrace $PIXEL pixel & wait
```

Open a trace in the [Perfetto UI](https://ui.perfetto.dev) (Open trace file), or summarise it with `trace_processor`:

```bash
trace_processor query -f docs/perf/cold-start.sql ~/perfetto-traces/ritune/redmi-cold-start.perfetto-trace
```

The summary lists the lifecycle, Ritune's sections, TTID and TTFD in ms from launch, then every main-thread frame up to TTFD with its missed 60 Hz and 120 Hz deadlines.
