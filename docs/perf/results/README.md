# Benchmark results

One folder per Macrobenchmark run, named `<date>-<phone>-<journey>-<state>`. [`../../performance.md`](../../performance.md) explains what each run shows.

Each folder holds:

| File | What it is |
|---|---|
| `com.ritune.benchmark-benchmarkData.json` | Every metric and iteration, plus the phone's build fingerprint |
| `*dexopt-before.txt`, `*dexopt-after.txt` | The app's compilation state before and after the run. If they differ, the run is invalid. |
| `*seed.txt` | The seed size used (days of anchor events, number of routines) |
| `additionaltestoutput.benchmark.message_*.txt` | Min, median and max per metric, with the trace file for each |

Perfetto traces are not committed; they are 1–2 MB each. Rebuild them by rerunning the benchmark at the recorded commit.

## Gaps

- `2026-11-01-redmi-startup-day-one`, `2026-11-01-pixel-startup-day-one` and `2026-11-01-pixel-startup-day-one-aa` predate `seed.txt`. They used the default seed, 21 days and 30 routines, which their commit hard-coded.
- The first Redmi startup run (medians: TTID 467 ms, TTFD 771 ms), used as the Redmi's A/A partner, has no folder. Gradle cleared its output before it was copied.
- Gradle clears `benchmark/build/outputs/connected_android_test_additional_output/` on every run. Copy each result here straight after its run.
