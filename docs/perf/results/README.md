# Benchmark results

One folder per Macrobenchmark run, named `<date>-<phone>-<journey>-<state>`. [`../../performance.md`](../../performance.md) explains what each run shows.

Each folder holds:

| File | What it is |
|---|---|
| `com.ritune.benchmark-benchmarkData.json` | Every metric and iteration, plus the phone's build fingerprint |
| `*dexopt-before.txt`, `*dexopt-after.txt` | The app's compilation state before and after the run. If they differ, the run is invalid. |
| `*seed.txt` | The seed size used (days of anchor events, number of routines, and whether the 15-step routine was added) |
| `additionaltestoutput.benchmark.message_*.txt` | Min, median and max per metric, with the trace file for each |

Perfetto traces are not committed; they are 1–2 MB each. Rebuild them by rerunning the benchmark at the recorded commit.

## Sessions

Only compare folders from the same session. The Redmi's timings shifted between sessions 1 and 2 on 2026-11-01, for old and new code alike (see "Sessions" and "The Redmi drifts between sessions" in `performance.md`). Each later day is a new session.

| Session (UTC, 2026-11-01) | Redmi folders |
|---|---|
| 1: 06:30–11:30 | `redmi-startup-day-one`, `redmi-seed-A/B/C`, `redmi-startup-speed-profile`, `redmi-scroll-day-one`, `redmi-open-routines-day-one`, `redmi-open-routines-R0/R30/R60`, `redmi-open-rhythm-day-one`, `redmi-open-routine-detail-editor`, `redmi-open-player-day-one` |
| 2: 12:36–14:30, after MIUI optimization was switched off and on | `redmi-startup-warm-day-one`, `redmi-startup-hot-day-one`, `redmi-startup-day-one-rerun`, `redmi-startup-ab-old-0afdb46`, `redmi-startup-ab-head` |
| 3: 2026-11-02, 10:38–11:00 | `2026-11-02-redmi-player-skip-steps`, `2026-11-02-redmi-startup-sections` (cold), `2026-11-02-redmi-startup-sections-warm` |
| 4: 2026-11-03, 06:31–08:33 | `2026-11-03-redmi-p3-<journey>-<dayone\|compiled>-<A\|B><1\|2>`: the Phase 3 A/B, 24 runs. A is the same code without the generated Baseline Profile. |
| 5: 2026-11-06, 02:32–03:14 | `2026-11-06-redmi-p4-routines-lazy-list-<journey>[-R0\|-R60]-compiled-<A\|B><1\|2>`: the Phase 4 fix 1 A/B, 12 runs. A is `main` at `3fa8efd`, B the lazy list with its regenerated Baseline Profile. |

`2026-11-02-redmi-p3-smoke-compiled-B` only checked that `compile=bgdexopt` works on MIUI. It is not part of any comparison.

## Gaps

- `2026-11-01-redmi-startup-day-one`, `2026-11-01-pixel-startup-day-one` and `2026-11-01-pixel-startup-day-one-aa` predate `seed.txt`. They used the default seed, 21 days and 30 routines, which their commit hard-coded.
- The first Redmi startup run (medians: TTID 467 ms, TTFD 771 ms), used as the Redmi's A/A partner, has no folder. Gradle cleared its output before it was copied.
- Gradle clears `benchmark/build/outputs/connected_android_test_additional_output/` on every run. Copy each result here straight after its run.
- `2026-11-01-redmi-startup-day-one` kept no traces, so session 1's cold start can't be broken down from its own run.
- `2026-11-01-redmi-startup-ab-old-0afdb46` ran the old commit, whose `seed.txt` has no `longRoutine` field. That commit had no long routine, so it is the same as `longRoutine=false`.
- Session 4 is the fifth attempt at the Phase 3 runs. Earlier attempts stopped on MIUI's "Install via USB" prompt or on auto-rotate (see "Phase 3: Baseline Profiles", run notes, in `performance.md`), and their files live in `superseded/`, which is not committed. Open Detail ran about 40 minutes after the other two journeys. Its compiled runs were redone as a set of four, after an install prompt stopped them.
- Session 3's first cold run was lost: the warm and `skipSteps` runs started before it was copied, and each run clears the output folder. `2026-11-02-redmi-startup-sections` is the rerun, made after `skipSteps`. The first warm run, if it ran, was lost the same way, and `2026-11-02-redmi-startup-sections-warm` is the run after cold.
