# Tempo

A native Android timetable maker, part of **Axis**. Kotlin, Jetpack Compose and Material 3, with a forest-green identity.

## Build

Open this directory in Android Studio with JDK 17, Android SDK 36 and the included **Gradle 8.13 wrapper**, then run:

```sh
./gradlew :core:schedule:test :core:data:testDebugUnitTest :app:lintDebug :app:assembleDebug
```

CI installs the pinned Gradle version and publishes a debug APK for each successful build. Android 8.0 (API 26) or newer is required. Package: `cc.xdan.tempo`.

## First milestone

- Resumable three-step onboarding and an optional fictional demo.
- Today summary, weekday timeline, editable sessions and overlap warnings.
- Subjects/activities with optional icons, six colours and default locations.
- Independent day hours and editable usual-period shortcuts.
- Real-duration timelines, evenly rounded blocks and subtle dashed frees.
- Forest, Ocean, Amber, light/dark/system and Android 12+ dynamic colour.
- Atomic device-local saving and validated versioned snapshots.

Widgets, recurrence, free tags, portable presets, calendar/PNG exports are tracked next. Outfit is bundled under the SIL Open Font Licence. The rounded timetable icon includes adaptive and monochrome variants.

See [roadmap](docs/ROADMAP.md) and [architecture](docs/ARCHITECTURE.md). Early development; not a release.
