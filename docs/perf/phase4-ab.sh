#!/usr/bin/env bash
# Phase 4 Prove: one fix against main, on one phone, in one session.
#
#   A = the fix branch's base on main, checked out in A_DIR, with its own Baseline Profile
#   B = this checkout: the fix, with a Baseline Profile regenerated for its code
#
# For each journey and state the runs alternate A, B, A, B. "compiled" passes compile=bgdexopt:
# SeededAppRule installs the bundled profile and runs bg-dexopt-job before measuring, the state a
# Play install starts in. Each run's output is copied to docs/perf/results/ before the next run
# starts, because Gradle clears the output folder at the start of every run.
#
# Usage: FIX=routines-lazy-list docs/perf/phase4-ab.sh OpenScreenBenchmark#openRoutines
#   SEED_ROUTINES=60  seeds 60 routines on top of the 7 built-in ones (default: the rule's 30)
#   STATES="dayone compiled"  (default: compiled)
#   A_COMMIT=79f03b4  checks A_DIR against this commit instead of the merge-base
#   REPS=2  resumes at the second A, B pair after an interrupted run (default: "1 2")
# Needs the RITUNE_KEYSTORE_* variables, like any benchmark build.
set -euo pipefail

B_DIR=$(cd "$(dirname "$0")/../.." && pwd)
A_DIR=${A_DIR:-$B_DIR/../Ritune-p4-main}
FIX=${FIX:?set FIX to the fix name, for example routines-lazy-list}
export ANDROID_SERIAL=${ANDROID_SERIAL:-de0a757d}
OUT_SUB=benchmark/build/outputs/connected_android_test_additional_output/benchmark/connected
DATE=$(date -u +%F)
TESTS=("$@")
[[ ${#TESTS[@]} -gt 0 ]] || { echo "name at least one test, for example OpenScreenBenchmark#openRoutines"; exit 1; }

# Main can move while a fix is measured. A must be the commit the fix branched from, unless A_COMMIT
# names another one, for example main before Phase 4 for the before/after run.
A_COMMIT=$(git -C "$B_DIR" rev-parse "${A_COMMIT:-$(git -C "$B_DIR" merge-base HEAD origin/main)}")
[[ $(git -C "$A_DIR" rev-parse HEAD) == "$A_COMMIT" ]] ||
  { echo "A_DIR isn't at $A_COMMIT"; exit 1; }

run() { # side test state rep
  local side=$1 test=$2 state=$3 rep=$4 dir=$B_DIR
  [[ $side == A ]] && dir=$A_DIR
  local args=(-Pandroid.testInstrumentationRunnerArguments.class=com.ritune.benchmark.$test)
  [[ $state == compiled ]] && args+=(-Pandroid.testInstrumentationRunnerArguments.compile=bgdexopt)
  local seed=""
  if [[ -n ${SEED_ROUTINES:-} ]]; then
    args+=(-Pandroid.testInstrumentationRunnerArguments.seedRoutines=$SEED_ROUTINES)
    seed=-R$SEED_ROUTINES
  fi
  local name=$DATE-redmi-p4-$FIX-${test##*#}$seed-$state-$side$rep
  echo "== $name, started $(date -u +%T) UTC"
  (cd "$dir" && ./gradlew -q :benchmark:connectedBenchmarkAndroidTest "${args[@]}")
  # If MIUI's "Install via USB" prompt isn't accepted, nothing installs, zero tests run and Gradle
  # still succeeds. Stop here instead of saving an empty result.
  compgen -G "$dir/$OUT_SUB/*/*benchmarkData.json" > /dev/null ||
    { echo "!! $name ran no test (check the phone for an install prompt)"; exit 1; }
  local dest=$B_DIR/docs/perf/results/$name
  # A rerun replaces the run with the same name; the old one is kept aside, never merged into it.
  if [[ -e $dest ]]; then
    mkdir -p "$B_DIR/docs/perf/results/superseded"
    mv "$dest" "$B_DIR/docs/perf/results/superseded/$name-$(date -u +%H%M%S)"
  fi
  mkdir -p "$dest"
  cp -R "$dir/$OUT_SUB"/*/. "$dest/"
  rm -rf "${dir:?}/$OUT_SUB"
}

read -ra STATE_LIST <<< "${STATES:-compiled}"
for test in "${TESTS[@]}"; do
  for state in "${STATE_LIST[@]}"; do
    for rep in ${REPS:-1 2}; do
      run A "$test" "$state" "$rep"
      run B "$test" "$state" "$rep"
    done
  done
done
echo "== all done, $(date -u +%T) UTC"
