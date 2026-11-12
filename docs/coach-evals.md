# Coach evals

The evals score the rhythm coach on a fixed set of questions with the real model. Unit tests use `FakeCoachGateway` and prove the code works. The evals measure how well the model behaves with that code: does it call the right tools, quote only real numbers, refuse medical advice and propose only what was asked.

## What is checked

The scenarios live in `CoachEvalScenarios` in `:core:testing`. Each one seeds synthetic data, asks one or more questions and checks every reply:

| Check | Passes when |
|---|---|
| tools | The turn called every expected tool |
| no forbidden tools | A read-only question called no `propose_` tool |
| grounded | Every number in the reply appears in a tool result, the question or the prompt. "10:30 PM" matches "22:30". |
| proposal / no proposal | The expected card was proposed, or none when nothing was asked for |
| valid proposal | No proposal was rejected by the tool's own validation |
| disclaimer | A health question points to a doctor or professional |
| within cap | The turn stayed inside `RunCoachTurn.MAX_TOOL_ROUNDS` |
| replied | The model said something, including after Apply or Decline |

Quota and network failures are retried after a minute, because they say nothing about the coach. If the first three scenarios all fail with an error, the run stops: that is a setup problem, such as an unregistered App Check token.

## Running them

The evals call Gemini, so they cost quota and never run in CI. `CoachEvalTest` skips itself unless an App Check debug token is passed in.

1. In the Firebase console, open App Check, then Apps, then Manage debug tokens, and add a token. Keep its value out of git.
2. Put it in `local.properties` as `coachEvals.appCheckToken=<token>`. That file is ignored by git.
3. Run on a connected device. Installing the debug build replaces a Play install of Ritune. The token goes in through the environment, so it doesn't show in the process list.

```bash
adb logcat -c
env "ORG_GRADLE_PROJECT_android.testInstrumentationRunnerArguments.firebaseAppCheckDebugSecret=$(grep '^coachEvals.appCheckToken=' local.properties | cut -d= -f2)" \
  ./gradlew :app:connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.ritune.evals.CoachEvalTest
adb logcat -d -s CoachEval:I -v raw > coach-evals.txt
```

Add `-Pandroid.testInstrumentationRunnerArguments.evalScenarios=fix-log,undo-then-ask` to rerun only some scenarios.

The log has a transcript for every scenario, then a summary: the pass rate by category, turn latency p50 and p95, token use, and one row per scenario. The summary names `CoachInstructions.VERSION`, so bump it with every prompt change and compare runs by version.
