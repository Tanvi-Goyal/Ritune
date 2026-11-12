[← Performance home](../performance.md)

# Phase 0: manual cold start

> Written up in the series as [Part 1](https://medium.com/@goyaltanvi94/ritune-takes-2-8-longer-to-start-on-a-mid-range-phone-here-is-how-i-found-where-the-time-goes-ee1453c9b3e2).

**The Redmi takes 2.8× longer than the Pixel to draw its first frame on day one, and compiling helps less than expected.** This is the first number, before any instrumentation existed. TTID only; TTFD comes in Phase 1.

Method: release APK, `adb shell am start -W` in a loop, 10 cold starts per phone and state, with a `force-stop` and a 3-second pause between runs. `TotalTime` is TTID.

| Date | Commit | Device | Compilation state | Runs | TTID median | TTID range |
|---|---|---|---|---|---|---|
| 2026-10-30 | `21a33c7` | Redmi Note 10 Pro | None (day one) | 10 | 377 ms | 349–607 ms |
| 2026-10-30 | `21a33c7` | Redmi Note 10 Pro | Speed profile (`bg-dexopt`) | 10 | 321 ms | 306–497 ms |
| 2026-10-29 | `4c50885` | Pixel 9 Pro | None (day one) | 10 | 133 ms | 125–151 ms |
| 2026-10-29 | `4c50885` | Pixel 9 Pro | Speed profile | 10 | 115.5 ms | 106–130 ms |

Findings:

- **The Redmi is slower and less consistent.** 3 of its 10 day-one runs went over 500 ms. On this class of phone the P90 matters as much as the median.
- **Compiling helps less than expected.** Speed profile cut the Pixel's cold start by 13% and the Redmi's by 6 to 15%. So most of the Redmi's startup time is something other than uncompiled code.
- **The noise between sessions is large.** Two day-one sessions on the same Redmi and APK gave medians of 377 and 342.5 ms, about 10% apart. The Pixel moved from 133 to 146.5 ms. Any smaller change needs 20 to 30 runs, or a trace section that measures it directly.
- **Libraries already ship profile rules.** The release APK contained a `baselineProfiles` folder before Ritune had a profile of its own: Compose and AndroidX bring their own rules. That sets the baseline for Phase 3.

---

← Previous: [Benchmark harness](benchmark-harness.md) · Next: [Phase 1: cold-start trace](phase-1-cold-start-trace.md) → · [Performance home](../performance.md)
