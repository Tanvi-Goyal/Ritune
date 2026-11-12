# Play Console listing and declarations

Copy for Play Console. Character limits are noted next to each field.

## Store listing (Grow users > Store presence > Main store listing)

**App name** (30): `Ritune: Daily Rhythm`

**Short description** (80):

```
Log the anchors of your day and run guided routines that work with screen off.
```

**Full description** (4000):

```
Ritune helps you build a steady daily rhythm. Log the moments that anchor your day, see how consistent you are across the week, and run short guided routines with cues that keep going when your screen is off.

LOG YOUR DAY IN ONE TAP
Six anchors shape your day: wake, morning light, movement, last meal, screens off and lights out. Tap an anchor when it happens. Made a mistake? Undo it, or correct the time later. A ring on the Today screen shows how much of your day you have logged.

SEE YOUR WEEKLY RHYTHM
The Rhythm screen shows how steady each anchor has been this week. A log counts as steady when it lands within 30 minutes of that anchor's usual time. You can see your steadiest anchor, how much your wake time drifts, and your consistency day by day.

GUIDED ROUTINES
Start with seven short routines, or build your own:
• Morning start
• Morning light walk
• 20-minute strength
• Close the kitchen
• Evening reset
• Wind down
• Focus block

Add, edit and reorder steps, set how long each one lasts, and link a routine to an anchor so starting it logs that anchor for you.

CUES THAT KEEP TIME FOR YOU
At each step Ritune can buzz, ring a soft bell and read the step name aloud, with a 3-2-1 countdown before the next step and a chime when you finish. Turn any cue on or off, even mid-routine; changes apply from the next step. Pause, resume, skip or stop from the app or the notification. The routine keeps running with the screen off, so you can put your phone down and follow along.

ASK THE RHYTHM COACH
Optional and off until you turn it on. Ask about your week in plain words and get short answers based on your own logs. The coach can suggest a new target time, a routine or a fix to a log, and nothing changes until you tap Apply. It runs on Google's Gemini, and it isn't medical advice.

PRIVATE BY DESIGN
• No account, no ads, no analytics
• Your logs, routines and history stay on your phone
• Only the coach uses the internet, and only after you turn it on
• Delete all data from Settings at any time

LIGHT AND DARK THEMES
A calm, minimal design that follows your system theme, or pick one in Settings.

Ritune is not a medical device and does not give medical advice. Check with a professional before starting a new exercise or sleep routine.
```

**App category**: Health & Fitness
**Tags** (pick up to 5 in console): Habit tracker, Routine planner, Self-improvement, Daily planner, Wellness
**Contact email**: goyaltanvi94@gmail.com
**Privacy policy**: https://tanvi-goyal.github.io/Ritune/privacy-policy

**Graphics**: `docs/store/out/icon-512.png`, `docs/store/out/feature-graphic.png`, `docs/store/out/screenshots/*.png` (upload in this order: today, rhythm, routines, editor, player, player_dark, rhythm_dark, settings).

## Foreground service declaration (Policy > App content > Foreground service permissions)

**Type**: `FOREGROUND_SERVICE_SPECIAL_USE`, subtype `guided_routine_timer`

**Feature that uses it**:

```
Guided routine player. The user starts a routine (a sequence of timed steps, for example a 20-minute bodyweight workout or a 15-minute wind-down). While it runs, the service keeps the step timer accurate and plays the user-enabled cues at each step boundary: a vibration pattern, a short bell sound, the step name read aloud with the device's text-to-speech engine, a 3-2-1 countdown and a completion chime. An ongoing notification shows the current step and a timer, with pause, resume and skip actions. The service starts only when the user taps Start and stops as soon as the routine finishes or the user stops it.
```

**User impact if the task is deferred or interrupted**:

```
Routines are designed to be followed with the phone put down and the screen off. If the system deferred or stopped the work, step changes would be late or missed, cues would not play, and the user would lose their place mid-workout or mid-routine. The task cannot be postponed because each cue must fire at the exact moment a step ends.
```

**Why no other foreground service type fits**:

```
shortService is limited to about three minutes, but routines run from 5 to over 30 minutes. mediaPlayback does not fit because the app does not play continuous media or expose a media session; it plays brief, intermittent cues and vibrations tied to a timer. health does not fit because the app reads no sensors and requires no body-sensor or activity-recognition permission. dataSync, location, camera, microphone and the other types are unrelated. specialUse with the subtype guided_routine_timer describes the task accurately.
```

**Video** (30-60 s, upload unlisted to YouTube and paste the link). Show:
1. Open Routines and tap Start on a routine.
2. The notification appearing with the timer and controls.
3. Turning the screen off; the next step's cue still plays (sound on).
4. Turning the screen on, pausing and resuming from the notification.
5. The routine finishing and the notification disappearing.

## Data safety (Policy > App content > Data safety)

Since 1.0.2 the optional rhythm coach sends data off the device, so the app collects data under Play's definition. These answers cover the release build: Firebase AI Logic, Firebase Authentication (anonymous) and Firebase App Check with Play Integrity. It has no analytics, crash reporting, ads or Firebase Installations.

### Data collection and security

| Question | Answer |
|---|---|
| Does your app collect or share any of the required user data types? | **Yes** |
| Is all of the user data collected by your app encrypted in transit? | **Yes** (Firebase uses HTTPS) |
| Which account creation methods does your app support? | **My app does not allow users to create an account** (the anonymous Firebase ID is not an account the user can sign in to) |
| Do you provide a way for users to request that their data is deleted? | **Yes**. Turning the coach off or "Delete all data" deletes the anonymous ID. Leave the deletion URL empty unless the form requires one |

### Data types

| Category › type | Collected | Shared | Ephemeral | Required or optional | Purposes |
|---|---|---|---|---|---|
| Health and fitness › Health info | Yes | Yes | No | Optional | App functionality |
| Health and fitness › Fitness info | Yes | Yes | No | Optional | App functionality |
| App activity › Other user-generated content | Yes | Yes | No | Optional | App functionality |
| Device or other IDs | Yes | No | No | Optional | App functionality, Fraud prevention, security, and compliance |

Why each one:
- **Health info**: the coach sends wake and lights-out logs and targets, which describe sleep.
- **Fitness info**: the coach sends routines, run history and movement anchors.
- **Other user-generated content**: the questions typed to the coach.
- **Device or other IDs**: the anonymous Firebase Authentication ID, sent with every coach request and used to limit requests per install. Play Integrity also sends device attestation data. Google Play handles that data, but it is covered here too.
- **Shared**: the Gemini API free tier lets Google use requests to improve its products. That goes beyond a service provider acting only on our instructions, so coach content is marked shared. The ID is used by Firebase only to run the service, so it is collected, not shared.
- **Optional**: nothing is sent unless the user turns the coach on.
- **Not declared**: Firebase Authentication logs IP addresses for abuse prevention. They are not used to infer location, so Approximate location is not declared.

Sources: [Firebase data disclosure](https://firebase.google.com/docs/android/play-data-disclosure), [Play Integrity data safety](https://developer.android.com/google/play/integrity/terms), [Play Console Help](https://support.google.com/googleplay/android-developer/answer/10787469).

Update this form before releasing any version that adds a library or feature that sends data.

## Other App content answers

| Section | Answer |
|---|---|
| Privacy policy | URL above |
| Ads | No, the app does not contain ads |
| App access | All functionality is available without special access |
| Content rating | Questionnaire, category "All other app types": answer No to violence, sexuality, language, controlled substances, gambling, user interaction, sharing location, digital purchases. Expected rating: Everyone / PEGI 3 |
| Target audience | 18 and over |
| News app | No |
| Health apps | Tick only the activity/fitness and sleep-related feature options (exact labels vary in the form); none of the medical, clinical or disease-management ones |
| Government app | No |
| Financial features | None |
| Data deletion | In-app: turning the coach off or "Delete all data" deletes the anonymous ID. No account to delete |
