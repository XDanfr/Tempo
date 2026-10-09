# Tempo

A native Android timetable maker, part of **Axis**. Kotlin, Jetpack Compose and Material 3, with a forest-green identity.

## Build

Open this directory in Android Studio with JDK 17, Android SDK 36 and the included **Gradle 8.13 wrapper**, then run:

```sh
./gradlew :core:schedule:test :core:data:testDebugUnitTest :app:lintDebug :app:assembleDebug
```

CI installs the pinned Gradle version and publishes a debug APK for each successful build. Android 8.0 (API 26) or newer is required. Package: `cc.xdan.tempo`.

## First milestone

- Resumable four-step onboarding: day hours, usual periods/breaks, subjects and appearance.
- Today summary, weekday timeline, editable sessions and overlap warnings.
- Subjects/activities with a searchable 63-option icon picker, six colours and default locations.
- Independent day hours, editable usual-period shortcuts, and repeating Break/Lunch/Changeover/custom breaks.
- Real-duration timelines, filled or outlined blocks, and tappable dashed frees that prefill the exact time range.
- Forest, Ocean, Amber and Material You theme options, plus light/dark/system modes.
- Atomic device-local saving and validated versioned snapshots, including v1 → v2 migration.
- Stronger Outfit typography using genuine Medium/Semibold/Bold font files.
- Spring day/tab transitions, animated timeline changes and predictive back progress/cancellation.
- Predictive modal editor sheets; Back from a top-level tab returns to Today, then system Back exits the app.

Widgets, recurrence, free tags, portable presets, calendar/PNG exports are tracked next. Outfit is bundled under the SIL Open Font Licence. The rounded timetable icon includes adaptive and monochrome variants.

Normal CI publishes development APKs using the runner's debug signing key and also validates the minified release variant. The manual **Signed release** workflow builds a signed APK and app bundle using a persistent key from repository secrets. See [release setup](docs/RELEASES.md) for the four required secrets and download instructions.

See [device testing](docs/DEVICE_TESTING.md), [roadmap](docs/ROADMAP.md) and [architecture](docs/ARCHITECTURE.md). Early development; not a release.
