# AGENTS.md

Guidance for anyone, human or AI agent, changing Ritune. Read the [README](README.md) for what the app does and how it is built.

## Project map

| Module | Holds | Depends on |
|---|---|---|
| `:app` | Activity, NavHost, Hilt root, navigation commands | everything it wires |
| `:feature:<name>:ui` | One feature's screens, ViewModels and graph builder | `:core:domain`, `:core:designsystem` |
| `:core:domain` | Use cases, `RoutinePlayerReducer`, ports, repository interfaces | `:core:models` |
| `:core:models` | Plain Kotlin models (JVM module) | nothing Android |
| `:core:data` | Repository implementations, mappers, seeding | `:core:domain`, `:core:database`, `:core:datastore` |
| `:core:database` / `:core:datastore` | Room and DataStore | `:core:models` |
| `:core:designsystem` | Theme, components, UI strings | nothing project-level |
| `:core:navigation` | Deep-link constants | nothing |
| `:platform:android` | Player engine, foreground service, cues, notifications | `:core:domain`, `:core:navigation` |
| `:core:testing`, `:core:screenshot-testing` | Test doubles and screenshot helpers | test classpaths only |
| `build-logic` | Convention plugins (`ritune.android.feature`, `ritune.hilt`, …) | n/a |

## Architecture rules

- Dependencies point inward. Features never depend on each other, on `:core:data` or on `:platform:android`; `:app` connects them.
- The domain talks to Android only through ports (`PlayerController`, `CuePlayer`, `RunRecorder`, repositories).
- Screens follow state, events and one-off effects. ViewModels expose a `StateFlow` of UI state and a `Flow` of effects.
- Time comes from `Timekeeper`, ids from `IdGenerator` and dispatchers from `@Dispatcher` qualifiers, never directly from the system.
- Navigation follows [docs/navigation.md](docs/navigation.md). Deep links navigate but never act.
- New modules apply the convention plugins and declare only their own extra dependencies. Versions live in `gradle/libs.versions.toml`.

## Code style

- Kotlin official style. Match the surrounding code.
- Keep comments rare. Write one only to explain *why*, never to narrate *what*.
- User-facing strings live in string resources, never inline: `:core:designsystem` for screens, `:platform:android` for notifications.

## Testing

Every change ships with tests. [docs/testing.md](docs/testing.md) explains what goes where and why. Its **What a change needs** table lists the tests each kind of change requires.

- Use the fakes in `:core:testing`. A new repository or port gets a fake there in the same change.
- No `Thread.sleep` or real clocks in local tests; use virtual time.
- A bug fix starts with a test that fails without it.
- A UI change re-records its screenshot goldens, and you review every changed image.

Before opening a pull request:

```bash
./gradlew :app:lintDebug testDebugUnitTest verifyRoborazziDebug :app:assembleDebug
```

```bash
./gradlew pixel6Api34DebugAndroidTest
```

## Git workflow

- Branches are `<type>/<kebab-description>`, for example `feat/anchor-reminders` or `test/feature-rhythm`.
- Commits follow Conventional Commits: `feat:`, `fix:`, `test:`, `build:`, `ci:`, `docs:`, `refactor:`, `chore:`. Keep each commit to one logical change.
- One pull request per change, merged with a merge commit after CI passes.
- When pull requests are stacked, merge from the bottom up, and retarget each one to `main` before merging it. Otherwise it merges into a branch that has already landed.
