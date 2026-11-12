[← Performance home](../performance.md)

# Phase 4 before and after

> Written up in the series as [Part 3](https://medium.com/@goyaltanvi94/experiments-and-outcomes-improving-ritunes-performance-655b8002fd44).

Each fix was measured against its own base, in its own session, so their gains can't be added up. This A/B measures the whole phase at once ([#61](https://github.com/Tanvi-Goyal/Ritune/pull/61)).

**Method.** Redmi Note 10 Pro, release key, benchmark build, 2026-11-08, 08:21 to 10:43 UTC.

- **A = `79f03b4`**: `main` before #53, the first Phase 4 fix, with the Baseline Profile it shipped.
- **B = `08d9c59`**: `main` with #60 merged, with its profile regenerated for that code.
- Every journey compiled (`compile=bgdexopt`; `speed-profile` confirmed before and after all 32 runs), run A, B, A, B, 15 iterations a run, 30 per side.
- The benchmark code is the same on both sides, apart from the `TodayPreload` section, which A doesn't have.
- `phase4-ab.sh` gained `A_COMMIT`, so A can be a commit other than the branch's base.
- Ranges are bootstrapped 95% ranges of the median difference, 4,000 resamples.
- Results: `perf/results/2026-11-08-redmi-p4-before-after-*` (32 folders: 8 journeys × A1, B1, A2, B2).

| Journey, median | Before (`79f03b4`) | After (`08d9c59`) | Change | 95% range |
|---|---|---|---|---|
| Cold start, full display (TTFD) | 801.6 ms | 691.3 ms | **−110.3 ms (−14%)** | −136.9 to −69.5 |
| Cold start, first frame (TTID) | 485.7 ms | 668.4 ms | +182.7 ms (+38%), by design | +156.4 to +204.2 |
| Open Routines, 37 routines (the rule's default seed) | 156.6 ms | 112.6 ms | −44.0 ms (−28%) | −61.4 to −11.6 |
| Open Routines, 67 routines | 217.6 ms | 169.8 ms | −47.8 ms (−22%) | −77.6 to −28.3 |
| Open Detail | 132.8 ms | 102.1 ms | −30.7 ms (−23%) | −37.6 to −20.6 |
| Open Editor | 93.5 ms | 87.4 ms | −6.1 ms (−6%) | −10.8 to +0.1 |
| Open Player | 107.8 ms | 89.6 ms | −18.2 ms (−17%) | −26.8 to −5.4 |
| `Player.notify` on the main thread, per run of 5 skips | 30.0 ms | 0 ms | −30.0 ms | all 30 runs at 0 |
| Scroll Routines, late frames per iteration | 2.3% | 3.3% | +1.0 pt | −0.6 to +10.6 |

**What it shows.**

- **Today is ready 110 ms sooner after a cold start, and the first frame comes later by design.** Before, frame 1 drew only the splash, and Today's content followed 316 ms later. Since [fix 5](phase-4-fixes.md#fix-5-today-draws-its-content-on-the-first-frame), frame 1 is Today's content, so the first frame and full display are 23 ms apart. Full display is when Today can be read and used. P90 full display went from 932.9 to 772.7 ms, but its range (−398 to +218 ms) includes zero.
- **Every screen open is faster.** Routines −28% (−22% with 60 more routines), Detail −23%, Player −17%. The Editor's −6% sits at the edge of the noise; it matches fix 8's single-routine query, which measured −8.9 ms on its own.
- **The notification work left the main thread.** Each `Player.notify` call takes longer now (6.0 → 11.0 ms on average), because it runs on the Default dispatcher: 55 ms per run in all, none of it on main.
- **Scrolling shows no change.** Late frames per iteration and P90 frame CPU per iteration (6.1 → 6.5 ms, range −0.4 to +3.3 ms) both include zero. Pooled, 9.0% of frames were late before and 14.3% after, but that comes from a few iterations with 36% to 63% late frames, on both sides.
- **The startup fixes don't add up.** Fixes 4, 5 and 6 measured −24, −75 and −43 ms on TTFD against their own bases, 142 ms in all; together they give −110 ms. Each one changed what the next one was measured against.

---

← Previous: [Phase 4: deep analysis and fixes](phase-4-fixes.md) · Next: [Lessons and changes of approach](lessons.md) → · [Performance home](../performance.md)
