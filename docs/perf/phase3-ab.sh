#!/usr/bin/env bash
# Phase 3 Prove: the app with and without its Baseline Profile, on one phone, in one session.
#
#   A = this code without app/src/main/generated (library rules only), checked out in A_DIR
#   B = this code, with the generated baseline and startup profiles
#
# For each journey and state the runs alternate A, B, A, B. Gradle installs the app fresh for
# every run, so "dayone" measures it as installed (verify). "compiled" passes compile=bgdexopt:
# SeededAppRule installs the bundled profile and runs bg-dexopt-job before measuring.
# Each run's output is copied to docs/perf/results/ before the next run starts, because Gradle
# clears the output folder at the start of every run.
#
# Usage: docs/perf/phase3-ab.sh [test ...]   e.g. docs/perf/phase3-ab.sh StartupBenchmark#coldStartup
# Needs the RITUNE_KEYSTORE_* variables, like any benchmark build.
set -euo pipefail

B_DIR=$(cd "$(dirname "$0")/../.." && pwd)
A_DIR=${A_DIR:-$B_DIR/../Ritune-prove-A}
export ANDROID_SERIAL=${ANDROID_SERIAL:-de0a757d}
OUT_SUB=benchmark/build/outputs/connected_android_test_additional_output/benchmark/connected
DATE=$(date -u +%F)
TESTS=("$@")
[[ ${#TESTS[@]} -gt 0 ]] || TESTS=(
  StartupBenchmark#coldStartup
  ScrollBenchmark#scrollRoutines
  OpenRoutineBenchmark#openDetail
)

[[ ! -d $A_DIR/app/src/main/generated ]] || { echo "A_DIR still has the generated profile"; exit 1; }
[[ -d $B_DIR/app/src/main/generated ]] || { echo "B_DIR has no generated profile"; exit 1; }

run() { # side test state rep
  local side=$1 test=$2 state=$3 rep=$4 dir=$B_DIR
  [[ $side == A ]] && dir=$A_DIR
  local args=(-Pandroid.testInstrumentationRunnerArguments.class=com.ritune.benchmark.$test)
  [[ $state == compiled ]] && args+=(-Pandroid.testInstrumentationRunnerArguments.compile=bgdexopt)
  local name=$DATE-redmi-p3-${test##*#}-$state-$side$rep
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
}

# STATES="compiled" reruns one state only, keeping its A, B, A, B order.
read -ra STATE_LIST <<< "${STATES:-dayone compiled}"
for test in "${TESTS[@]}"; do
  for state in "${STATE_LIST[@]}"; do
    for rep in 1 2; do
      run A "$test" "$state" "$rep"
      run B "$test" "$state" "$rep"
    done
  done
done
echo "== all done, $(date -u +%T) UTC"
