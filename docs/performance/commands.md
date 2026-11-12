[← Performance home](../performance.md)

# Commands

> **In short.** Copy-paste steps to reproduce each measurement, in the order the phases happened.

## Manual cold start (Phase 0)

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

`force-stop` before each launch makes every run a cold start. On the Redmi, use the MIUI steps in [Compilation state](methodology.md#compilation-state) instead of `cmd package compile`.

## Both phones at once

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

## Cold-start trace (Phase 1)

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

## Macrobenchmark journeys (Phase 2)

With the `RITUNE_KEYSTORE_*` variables set, the phone connected, and auto-rotate off:

```bash
./gradlew :benchmark:connectedBenchmarkAndroidTest
```

Copy each run's output from `benchmark/build/outputs/connected_android_test_additional_output/` to `docs/perf/results/<date>-<phone>-<journey>-<state>/` before the next run: Gradle clears it every time. On MIUI, accept the "Install via USB" prompt whenever it appears.

## Baseline Profile A/B (Phase 3)

[`perf/phase3-ab.sh`](../perf/phase3-ab.sh) runs each journey with and without the generated profile, in both compilation states, alternating A, B, A, B. It copies every run to `perf/results/` before the next run starts.

Set up A once, as a worktree at the same commit without the generated profile:

```bash
git worktree add --detach ../Ritune-prove-A HEAD
rm -r ../Ritune-prove-A/app/src/main/generated
cp local.properties ../Ritune-prove-A/
```

Then, from the repo root, with the `RITUNE_KEYSTORE_*` variables set and auto-rotate off:

```bash
docs/perf/phase3-ab.sh                                    # all three journeys, 24 runs
STATES=compiled docs/perf/phase3-ab.sh OpenRoutineBenchmark#openDetail   # one journey, one state
```

- **Install prompt:** watch the phone. On MIUI, accept the "Install via USB" prompt within about 10 seconds whenever it appears.
- **If a run produces no result**, the script stops and asks you to check the phone.
- **Rerunning a run** moves its old folder to `perf/results/superseded/` rather than merging into it.

Regenerate the profile after changing code it covers:

```bash
./gradlew :app:generateBaselineProfile
```

## Fix A/B (Phase 4)

[`perf/phase4-ab.sh`](../perf/phase4-ab.sh) runs one fix against `main` on the Redmi, alternating A1, B1, A2, B2, and copies every run to `perf/results/` before the next starts. A is the commit the fix branched from on `main`, in a separate worktree, with its own Baseline Profile; B is the fix branch, with a profile regenerated for its code.

```bash
git worktree add --detach ../Ritune-p4-main "$(git merge-base HEAD origin/main)"
cp local.properties ../Ritune-p4-main/
```

Then, from the fix branch, with the `RITUNE_KEYSTORE_*` variables set and auto-rotate off:

```bash
FIX=routines-lazy-list docs/perf/phase4-ab.sh OpenScreenBenchmark#openRoutines
FIX=routines-lazy-list SEED_ROUTINES=60 docs/perf/phase4-ab.sh OpenScreenBenchmark#openRoutines
FIX=routines-lazy-list docs/perf/phase4-ab.sh ScrollBenchmark#scrollRoutines
```

- `STATES="dayone compiled"` adds the day-one state (the default is `compiled`). `REPS=2` resumes at the second A, B pair after an interrupted run.
- The script refuses to start unless `A_DIR` is at the branch's base on `main` (`git merge-base HEAD origin/main`), and stops if a run produced no result (usually MIUI's install prompt). To move A to a new base: `git -C ../Ritune-p4-main checkout --detach <commit>`.
- `A_COMMIT=<commit>` checks A against that commit instead of the branch's base. The before/after run used it with A at `main` before Phase 4: `A_COMMIT=79f03b4 FIX=before-after docs/perf/phase4-ab.sh StartupBenchmark#coldStartup`.
- Cold-start fixes use `StartupBenchmark#coldStartup`; for example `FIX=today-first-frame docs/perf/phase4-ab.sh StartupBenchmark#coldStartup`.

## Compose compiler reports

```bash
./gradlew :app:assembleRelease -Pritune.composeReports=true
```

Each module writes its stability reports and metrics to `build/compose_compiler/`.

---

← Previous: [Lessons and changes of approach](lessons.md) · [Performance home](../performance.md)
