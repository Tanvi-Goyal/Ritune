[← Performance home](../performance.md)

# Lessons and changes of approach

> **In short.** The rules this work taught, and every place the measurements changed the approach. Read this before starting the next measurement.

## Changes of approach

Where the phones disagreed with the first idea, the approach changed. Each change and its reason:

| Phase | Started as | What happened | Why |
|---|---|---|---|
| 0 | Set compilation states with `cmd package compile` | Set them by reinstalling (day one) or with `bg-dexopt-job` (compiled) | MIUI skips shell-started compiles and still prints `Success` |
| 0 | Measure on a physical phone | Kept the Redmi as the primary device, rather than switching to a phone that obeys `cmd package compile` | The mid-range phone is the point of the study |
| 1 | Composition tracing in debug and benchmark builds | Moved to Phase 2's `benchmark` build type | It needs a release-like build: debug timings mislead, and adding it to release ships extra code to users |
| 2 | A `ritune.android.benchmark` convention plugin | The module uses the existing `ritune.android.test` plugin | It already had everything the module needs |
| 2 | Scroll the routines list and Rhythm | Rhythm's scroll test dropped; opening Rhythm is measured instead | Rhythm fits on one screen, so a fling draws no frames |
| 2 | Compare `None`, `Partial` and `Full` | `Full` dropped; all journeys use `Ignore()` and set the state themselves | MIUI won't run shell-started dexopt, with MIUI optimization on or off |
| 2 | Measure the "player tick" | Measured `Player.notify` with a journey that skips steps | It runs once per player state change (start, step change, pause, resume), not per tick |
| 2 | Measure seeding on the cold path | `Seed.*` measured only on an already-seeded database | Every benchmark seeds before it runs, so the first-launch inserts aren't measured |
| 2 | Fix Today's queries | Dropped before writing it | Changing the seed size moved TTFD by less than the noise |
| 3 | Verify the profile with `CompilationMode.None` vs `Partial` | Measured with `Ignore()` and `compile=bgdexopt`, the state a Play install reaches | `Partial(BaselineProfile.Require)` compiles from the shell, which MIUI skips |
| 4 | Pick 2–3 fixes | Every evidence-backed candidate is in scope; one is dropped only with a recorded reason | More candidates came out of Phases 1–3 than expected |
| 4 | The 200 ms poll → notification churn as a fix | Built as main-thread cleanup (#57); the per-second work is a battery question, not yet measured | It isn't a poll: `Player.notify` runs once per state change. Moving it off main freed the main thread but made no frame faster |
| 4 | Fix the startup critical path by creating the view models earlier | Became three fixes: the reads off main (#55), Today preloaded (#56), `InitializeApp` from `Application` (#58) | Starting the reads earlier alone made them finish later; each trace pointed at the next step |
| 4 | An A/B against `main` | A is the branch's base on `main` | `main` moved while fixes were measured |

## Lessons

- **Measure the noise before the effect.** A 10% swing between Redmi sessions decides how many runs every later claim needs.
- **Only compare runs from one session, alternating A, B, A, B.** The Redmi shifted ~100 ms on TTFD between sessions with no code change.
- **Name the compilation state with every number, and check it inside the test before and after.** The same APK starts at different speeds on day one and a week later.
- **Change one input before fixing what you suspect.** The query fix I expected to need would have saved almost nothing.
- **Use the libraries' profile as the baseline, not "no profile".** Compose and AndroidX already ship their own rules.
- **When two waits run in parallel, shortening one only helps until the other becomes the critical path.**
- **Use an async trace section around a coroutine.** A sync section can be closed by whichever thread the coroutine resumes on.
- **End a screen's trace section on its first drawn frame, not when its data arrives.**
- **One trace is one sample.** Run a query over every iteration's trace and report the median.
- **A fix's gain depends on the compilation state.** The player's empty frame cost ~30 ms at day one and 12 ms compiled. Measure the fix in the state users are in, and don't promise the day-one number.
- **Starting work earlier only helps if it can also finish without the main thread.** Fix 4 started the splash's reads 48 ms earlier, and they finished later, because their last step queued behind `onResume`.
- **The thing a screen waits for isn't always the thing that holds it.** Moving the reads off main fixed the reads, but the first frame came at the same time: main's own work was the limit.
- **Moving work into the first frame raises TTID even when the app gets faster.** The first drawn frame pays for the EGL context and shader compiles. Read TTFD, and subtract `shader_compile` when a fix moves it between frames.
- **A cost the trace points at can still be too small to show.** Fix 7 removed ~11 ms from the first text measure, and frame 1 didn't get measurably shorter. Measure before keeping the change.
- **Off the main thread is only faster if the work was inside a frame.** The notification updates ran between frames, so moving them changed nothing on screen.
- **Measure the whole phase once at the end.** Fix-by-fix gains are each read against a different base, and they overlap: the startup fixes summed to 142 ms but gave 110 ms together.
- **Check what else changes between A and B.** Each run starts from a fresh install, so shader compilation landed in different iterations on each side and made B look 21 ms faster on its own. Subtract it, or warm the cache, before reading the difference.
- **The phone can lie.** MIUI reports success for skipped compiles, and Gradle reports success when the install prompt blocks every test. Check outputs, not exit codes.

---

← Previous: [Phase 4 before and after](phase-4-before-and-after.md) · Next: [Commands](commands.md) → · [Performance home](../performance.md)
