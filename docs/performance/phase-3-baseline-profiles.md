[← Performance home](../performance.md)

# Phase 3: Baseline Profiles

> Written up in the series as [Part 2](https://medium.com/@goyaltanvi94/custom-baseline-profiles-made-ritunes-first-screen-load-10-faster-b256866b154d).

**The generated Baseline Profile makes cold startup 10% faster to TTFD, 83 ms on the Redmi, once it is compiled.** Scrolling the routines list and opening a routine barely change.

**What was built.** The `androidx.baselineprofile` plugin on `:app` and a `:baselineprofile` generator module. `BaselineProfileGenerator` drives the release build with no seeding (`SeedActivity` doesn't exist there); logging an anchor on Today creates the data the other screens show.

- `startup()` launches to Today with `includeInStartupProfile = true`, which also produces a Startup Profile. The build uses it to put startup classes first in the primary DEX file.
- `journeys()` logs an anchor, opens Rhythm, opens and scrolls Routines, opens Detail and the Editor, starts and stops a routine, and opens Settings.
- It runs on the `pixel6Api34Aosp` managed device (an `aosp` image, not ATD), not on the phones.
- The result is 24,311 lines, 2,548 of them for Ritune's own code.

**Setup.** Redmi Note 10 Pro, 60 Hz, session 4 (2026-11-03, 06:31–08:33 UTC, auto-rotate off), `CompilationMode.Ignore()`, 15 iterations per run, seed 21 days / 30 routines.

- **A and B are the same code.** B ships the generated profile in `app/src/main/generated/baselineProfiles`. A is a worktree at the same commit without that folder, so it ships only the rules its libraries bring (Compose, AndroidX).
- **Both states are set inside the test, before measuring:**
  - Day one: Gradle installs the APK fresh, so ART only verifies it.
  - Compiled: `compile=bgdexopt` makes `SeededAppRule` send profileinstaller's install broadcast and then run `bg-dexopt-job`, before the app has run at all. So only the bundled rules get compiled.
- **Order:** for each journey and state, runs alternate A, B, A, B. Each side pools 2 runs, 30 iterations.
- **Validity:** every run's `dexopt-before` matches its `dexopt-after`: `verify / install` for day one, `speed-profile / bg-dexopt` for compiled.

| Journey | State | Metric | A median (P90) | B median (P90) | B vs A |
|---|---|---|---|---|---|
| Cold startup | Compiled | TTID | 513.6 ms (555.9) | 484.6 ms (535.2) | **−29 ms (−5.7%)** |
| Cold startup | Compiled | TTFD | 812.6 ms (918.0) | 729.7 ms (844.1) | **−83 ms (−10.2%)** |
| Cold startup | Compiled | `InitializeApp` | 82.0 ms (92.8) | 70.7 ms (83.3) | **−11 ms (−13.8%)** |
| Cold startup | Day one | TTID | 519.4 ms (556.9) | 522.5 ms (577.4) | +0.6%, within noise |
| Cold startup | Day one | TTFD | 912.7 ms (1027.4) | 971.2 ms (1078.0) | +6.4%, within noise |
| Cold startup | Day one | `InitializeApp` | 75.3 ms (91.7) | 83.2 ms (98.2) | +10.4%, within noise |
| Scroll Routines | Compiled | Frame CPU time | 4.8 ms (6.5) | 4.7 ms (7.4) | −0.9% |
| Scroll Routines | Compiled | Frame overrun P99 | 12.7 ms | 7.6 ms | tail only |
| Scroll Routines | Day one | Frame CPU time | 5.7 ms (6.8) | 5.6 ms (6.7) | −2.5% |
| Open Detail (15 steps) | Compiled | `RoutineDetail.open` | 137.1 ms (156.0) | 133.2 ms (150.5) | −4 ms (−2.8%) |
| Open Detail (15 steps) | Day one | `RoutineDetail.open` | 186.3 ms (198.9) | 181.3 ms (202.4) | −5 ms (−2.7%) |

Each run's median, A1 / A2 against B1 / B2:

| Journey, state | Metric | A runs | B runs |
|---|---|---|---|
| Cold, compiled | TTID | 508.8 / 518.5 | 479.0 / 486.3 |
| Cold, compiled | TTFD | 815.8 / 809.4 | 709.9 / 744.3 |
| Cold, day one | TTFD | 950.8 / 898.7 | 965.2 / 985.9 |
| Open Detail, compiled | `RoutineDetail.open` | 149.0 / 135.1 | 131.9 / 134.5 |
| Open Detail, day one | `RoutineDetail.open` | 185.7 / 187.9 | 177.0 / 184.9 |

Results: `perf/results/2026-11-03-redmi-p3-<journey>-<state>-<A|B><run>/`, for example [cold, compiled, B1](../perf/results/2026-11-03-redmi-p3-coldStartup-compiled-B1/).

Findings:

- **Compiled cold startup is the win.** Both B runs beat both A runs on TTID, TTFD and `InitializeApp`, so the gap is larger than the run-to-run spread. Precompiled app code shortens the work the splash waits on (`InitializeApp` −11 ms) and the path from first frame to Today's data (TTFD −83 ms against TTID −29 ms).
- **Day one shows no gain, as expected.** A profile is only a list of methods; nothing compiled it, so B ran the same interpreted and JIT code as A. B's higher day-one numbers sit inside A's own spread (TTFD runs 899–951 ms), so they are not a measured cost.
- **The compiled state is what a Play user gets.** Play compiles the bundled profile when it installs the app, so a user starts in the compiled state on first launch. The day-one state only applies to sideloads, and there profileinstaller saves the profile at first launch and the next background dexopt compiles it.
- **Scroll barely moves**, because A already ships the Compose and library rules that cover the list code. The P99 overrun drops from 12.7 to 7.6 ms in the compiled state, but that is a handful of frames, so it is not claimed as an improvement.
- **Open Detail gains 4–5 ms in both states**, which is at the edge of the noise.
- **Compiling at all matters more than whose rules are compiled.** For A alone, compiling cuts Open Detail from 186 to 137 ms and cold TTFD from 913 to 813 ms. The app's own rules then take startup a further 83 ms.

**Run notes.** It took five attempts to complete all 24 runs, and two problems stopped the earlier ones:

- **MIUI's "Install via USB" prompt.** When nobody accepts it, nothing installs, zero tests run, and Gradle still reports success. The script now stops when a run produces no `benchmarkData.json`.
- **Auto-rotate.** It turned the phone to landscape during an Open Detail run, and the test couldn't find the long routine.

Only the last complete attempt is reported. Earlier attempts' files are kept locally in `perf/results/superseded/`, which is not committed.

---

← Previous: [Phase 2: Macrobenchmark](phase-2-macrobenchmark.md) · Next: [Phase 4: deep analysis and fixes](phase-4-fixes.md) → · [Performance home](../performance.md)
