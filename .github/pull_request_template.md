## What and why

<!-- What this changes, and why. Link the issue if there is one. -->

## Tests

<!-- Tick what applies; docs/testing.md ("What a change needs") explains each. -->

- [ ] Logic, use cases or ViewModels: unit tests cover the changed behavior
- [ ] New repository or port: a fake in `:core:testing`
- [ ] Database: DAO tests; schema change has a new version, exported JSON and a migration test
- [ ] UI: behavior tests, and screenshot goldens re-recorded and reviewed
- [ ] Navigation or deep links: a navigation test in `:app`
- [ ] Service, notifications or other platform code: a device test in `:app`
- [ ] Main user flow: end-to-end test and `journeys/` updated
- [ ] Bug fix: a test that failed before the fix
- [ ] No tests needed, because:

## Checked

- [ ] `./gradlew :app:lintDebug testDebugUnitTest verifyRoborazziDebug`
- [ ] `./gradlew pixel6Api34DebugAndroidTest` (when device tests or platform code changed)
