# Release journeys

Plain-English walkthroughs of Ritune's main flows, in the journey format of the official `android` CLI. Each `<action>` is either a step to perform or a "verify" check of what is on screen. A journey passes only if every action does.

They complement the automated tests in [docs/testing.md](../docs/testing.md). Claude runs them like a manual tester would, on a full emulator, so they also cover the notification shade, edge-to-edge drawing and how the app feels.

| Journey | Covers |
|---|---|
| [log-anchor.xml](log-anchor.xml) | Logging, undo, and state across tabs |
| [run-routine.xml](run-routine.xml) | The player: pause, resume, skip, summary |
| [edit-routine.xml](edit-routine.xml) | Creating, reordering and saving a routine |
| [player-background.xml](player-background.xml) | The foreground notification and its actions |
| [settings-and-reset.xml](settings-and-reset.xml) | Theme and "Delete all data" |

## Running them before a release

1. Install the `android` CLI and its skill: `android init`.
2. Start an emulator **with** System UI (a Google APIs or AOSP image; ATD images have no notification shade) and install the release candidate, the signed `app-release.apk` from the Release workflow:

   ```bash
   adb install app-release.apk
   ```

   Without a release build, `./gradlew :app:installDebug` works for a dry run.

3. In Claude Code, ask: "Run every journey in `journeys/` on the running emulator and write a report for each."
4. Claude follows the `android-cli` skill's journey rules, evaluating each step exactly as written, and writes one markdown report per journey. Any failure blocks the release until it is fixed or the journey is updated on purpose.

## Adding a journey

Add one when a new main user flow ships, and update the existing ones when a flow changes. Write each action as a single step a user would take, and use the exact text shown on screen.
