# Testing

Ritune has no QA team, so the tests are the QA. Every change has to pass three gates:

1. **Local tests** (JVM and Robolectric) on every pull request: logic, ViewModels, storage, Compose behavior and screenshots.
2. **Device tests** on every pull request: Room, navigation, the foreground service and end-to-end journeys on a Gradle Managed Device.
3. **Release journeys** before every release: plain-English walkthroughs that Claude runs on an emulator through the official `android` CLI (see [Release journeys](#release-journeys)).

## The pyramid

Most tests are small and fast; only a few drive the whole app.

| Layer | Source set and runner | What it covers | Reference test |
|---|---|---|---|
| Pure logic | `test`, JUnit | Reducers, calculators, validators | `RoutinePlayerReducerTest` |
| Coroutines and time | `test`, `runTest` + `VirtualTimekeeper` | Anything with `delay`, timers or a clock | `PlayerEngineTest` |
| Use cases | `test`, real use case + fakes | Business rules across repositories | `LogAnchorTest` |
| ViewModels | `test`, `MainDispatcherRule` + Turbine | State, events, one-off effects | `TodayViewModelTest` |
| DataStore | `test`, real DataStore in a temp folder | Preference mapping and atomic updates | `SettingsRepositoryImplTest` |
| Compose behavior | `test`, Robolectric + `createAndroidComposeRule` | Rendering per state, clicks, state restoration | `TodayContentTest` |
| Screenshots | `test`, Robolectric + Roborazzi | Layout at 9 window sizes, dark mode, 1.5x font | `TodayScreenScreenshotTest`, `ButtonsScreenshotTest` |
| Room | `androidTest`, in-memory database | DAOs, transactions, the exported schema | `RoutineDaoTest`, `RituneDatabaseSchemaTest` |
| Navigation | `androidTest` in `:app`, Hilt | Tabs, back, deep links | `RituneNavigationTest`, `RituneDeepLinkTest` |
| Platform | `androidTest` in `:app`, Hilt | The foreground service and its notification | `PlayerServiceTest` |
| End to end | `androidTest` in `:app`, Hilt | A full user journey on the real app (about 5% of tests) | `LogAnchorJourneyTest` |

Copy the reference test for the layer you are testing.

## Where things live

| Path | Contents |
|---|---|
| `<module>/src/test` | Local tests. Compose modules run them on Robolectric. |
| `<module>/src/androidTest` | Device tests. A module gets the managed device only once this folder exists. |
| `<module>/src/test/screenshots` | Roborazzi goldens, committed. |
| `:core:testing` | Shared test doubles for every module: `Fake*Repository`, port fakes, `FakeTimekeeper`, `VirtualTimekeeper`, `SequentialIdGenerator`, `MainDispatcherRule`, `HiltTestRunner`, data builders (`aRoutine()`, `aStep()`, …) and `robolectric.properties`. |
| `:core:screenshot-testing` | `captureScreenSizes`, `captureScreenVariants` and `captureComponent`. Compose modules only. |
| `app/src/androidTest/.../di` | `@TestInstallIn` modules: an in-memory Room database and fresh DataStore files for every Hilt test. |
| `journeys/` | Release journeys for the `android` CLI. |

The convention plugins in `build-logic` add all test dependencies. A module never declares them itself.

## Commands

All local tests, including screenshot verification:

```bash
./gradlew testDebugUnitTest verifyRoborazziDebug
```

One module, or one test class:

```bash
./gradlew :feature:today:ui:testDebugUnitTest --tests '*TodayViewModelTest'
```

All device tests on the managed emulator (the first run downloads the image):

```bash
./gradlew pixel6Api34DebugAndroidTest
```

Coverage report, written to `build/reports/kover/htmlCoverage/index.html`:

```bash
./gradlew :koverHtmlReportCoverage
```

Re-record screenshot goldens after an intended UI change:

```bash
./gradlew recordRoborazziDebug
```

What CI runs on every pull request:

```bash
./gradlew :app:lintDebug testDebugUnitTest verifyRoborazziDebug :koverXmlReportCoverage :app:assembleDebug :app:assembleRelease
```

## Rules

1. **Fakes over mocks.** Use the fakes in `:core:testing`. A new repository or port interface gets a fake there in the same change, and the fake must behave like the real implementation (same ordering, same ranges). Reach for a mocking library only when no fake is possible.
2. **Wire real code to fakes.** ViewModel tests build the real use cases on top of fakes, so they test behavior, not mock setup.
3. **Virtual time only.** Local tests never call `Thread.sleep` or read the system clock. Inject `Timekeeper` and use `FakeTimekeeper` for a fixed clock, or `VirtualTimekeeper` when coroutine delays and "now" must move together. `advanceTimeBy` runs tasks scheduled *before* the target time; follow it with `runCurrent()` for work due exactly then.
4. **Name tests `action_condition_expectedResult`.** For example `invoke_withinDedupeWindow_returnsExistingEvent`. One behavior per test, with arrange, act and assert separated by blank lines.
5. **Find UI the way a user does.** Match on visible text from string resources and on semantics such as click labels. Add a `testTag` only when an element needs more than three matchers.
6. **Every screen test checks state restoration** with `StateRestorationTester` when the screen keeps any saveable state.
7. **Screenshot tests wrap screens in a themed `Surface`,** the way the app's root `Scaffold` does, so dark mode renders on the right background.
8. **Device tests never share state.** Hilt tests get a fresh in-memory database and new DataStore files from the test modules; don't bypass them.
9. **A bug fix starts with a failing test** that reproduces the bug, committed together with the fix.
10. **Check that a new test can fail.** Break the code under test once and watch the test go red before you trust it.

## What a change needs

Use this table when you add a feature or change one. The PR template repeats it as a checklist.

| When you change… | Add or update… |
|---|---|
| A reducer, calculator, validator or other pure logic | Unit tests for every transition or branch you touched |
| A use case | A use case test with `:core:testing` fakes |
| A repository or port interface | Its fake in `:core:testing`, kept behaving like the real implementation |
| A repository implementation | A DataStore test (local) or a Room-backed test (device) |
| A DAO, entity or relation | A DAO test on an in-memory database |
| The database schema | Bump the version, commit the new schema JSON, write a migration and a `MigrationTestHelper` test for it |
| A ViewModel | A ViewModel test for its state, events and effects |
| A screen's UI | A behavior test and screenshot tests, then re-record the goldens |
| A design-system component | A `captureComponent` screenshot test |
| A route, deep link or navigation command | A navigation or deep-link test in `:app` |
| The player service, notifications or other platform code | A device test in `:app` |
| A main user flow | Its end-to-end test and its release journey in `journeys/` |
| A bug | A test that failed before the fix |

## Screenshots

Goldens live next to each module's tests in `src/test/screenshots` and are committed.

- `verifyRoborazziDebug` fails when a screen changes. When CI fails, download the `screenshot-diffs` artifact to see the before and after.
- If the change is intended, run `./gradlew recordRoborazziDebug`, look at every changed image, and commit them with the UI change.
- Robolectric renders with its own fonts, so goldens recorded on macOS match Linux CI exactly. To record on CI instead, run the **Record screenshots** workflow, or add the `record-screenshots` label to a pull request. Then commit the artifact:

```bash
gh run download <run-id> --name screenshots
```

## Coverage

Kover merges every module's `coverage` variant into one report. It excludes generated code: Hilt, Dagger factories, `@Module`s, Room `_Impl` classes, `ComposableSingletons` and `@Preview` functions. CI writes the line coverage to the job summary. There is no minimum yet; once every feature has tests, add Kover verify rules, starting at 80% for `:core:domain`.

## Environment decisions

These look odd without context. Revisit each one when the tool it works around is upgraded.

| Decision | Where | Why |
|---|---|---|
| Robolectric runs API 36 | `:core:testing/src/main/resources/robolectric.properties` | Robolectric's API 37 image removed `InputManager.getInstance()`, which Compose input injection needs. |
| `--add-exports=java.base/jdk.internal.access=ALL-UNNAMED` | `AndroidComposeConventionPlugin` | Robolectric reads raw `FileDescriptor` fields through that JDK package. |
| `failOnNoDiscoveredTests = false` in Compose modules | `AndroidComposeConventionPlugin` | Robolectric needs Android resources, which add a generated test `R` class that Gradle counts as test sources. |
| No Hilt in local tests | `HiltConventionPlugin` | Local tests pass fakes through constructors. Hilt testing is for device tests. |
| The service test reads `NotificationManager`, not the shade | `PlayerServiceTest` | The managed device runs an ATD image, which has no System UI. |
| The managed device exists only where `src/androidTest` does, one emulator at a time in CI | `AndroidConfig.kt`, `ci.yml` | Each module boots its own emulator, and several at once exhaust a GitHub runner. |

## Release journeys

`journeys/` holds plain-English test scripts in the journey format of the official `android` CLI. Before a release, Claude runs each one on a full emulator (one with System UI, unlike the ATD test device) and writes a pass or fail report. They catch what the automated tests can't see, such as the notification shade or a confusing screen. See [`journeys/README.md`](../journeys/README.md).

## Why each tool

- **JUnit 4 and `kotlin.test`.** The Android test runners are built on JUnit 4, and `kotlin.test` gives readable assertions like `assertIs` without another library.
- **`kotlinx-coroutines-test`.** `runTest` runs coroutines on a virtual clock, so a 10-minute routine runs in milliseconds.
- **Turbine.** Collects a `Flow` inside a test and lets you assert each emission in order.
- **Robolectric.** Runs Android and Compose code on the JVM. No emulator, so UI tests take seconds.
- **Roborazzi.** Captures Compose screenshots under Robolectric and compares them with committed goldens. It catches visual regressions that behavior tests can't.
- **Gradle Managed Devices.** Gradle creates, boots and tears down the emulator itself, so device tests run the same way on a laptop and in CI.
- **Hilt testing.** `@TestInstallIn` swaps production modules for test ones across a whole device test, such as an in-memory database.
- **UI Automator.** Drives the system outside the app, such as a real back press.
- **Kover.** Merges coverage from JVM and Android modules into one report.
