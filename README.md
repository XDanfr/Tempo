# Tempo

A native Android timetable maker, part of **Axis**. Kotlin, Jetpack Compose and Material 3, with a forest-green identity.

## Build

Open this directory in Android Studio with JDK 17, Android SDK 36 and **Gradle 8.13**. Until a generated Gradle wrapper is checked in, install Gradle 8.13 and run:

```sh
gradle :core:schedule:test :core:data:testDebugUnitTest :app:lintDebug :app:assembleDebug
```

CI installs the pinned Gradle version and publishes a debug APK for each successful build. Android 8.0 (API 26) or newer is required. Package: `cc.xdan.tempo`.

See [roadmap](docs/ROADMAP.md) and [architecture](docs/ARCHITECTURE.md). Early development; not a release.
