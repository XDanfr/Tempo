<p align="center">
  <img src="docs/tempo-icon.svg" width="96" alt="Tempo app icon">
</p>

<h1 align="center">Tempo</h1>

<p align="center"><em>Your week, in view.</em></p>

<p align="center">A flexible Android timetable for school, college, work and everyday life.</p>

<p align="center"><strong>Made by XDan</strong></p>

## Get Tempo

Download the latest stable APK from [GitHub Releases](https://github.com/XDanfr/Tempo/releases). Tempo works on Android 8.0 (Oreo) and newer.

Once a public stable release is available, you can check for updates in Settings or turn on automatic updates. Android will ask before installing an update.

## Plan your week

- **Make schedules that fit your life.** Keep separate timetables for school, college, work or personal activities. Set your own days, periods, session times, breaks and lunch.
- **See the day at a glance.** Check what’s happening now, follow a proportional timeline and fill an open period when plans change.
- **Compare timetables.** Put schedules side by side or combine several people’s lessons into one agenda to find shared free time.
- **Make it yours.** Give subjects their own colours, locations and optional icons. Choose Forest, Ocean, Amber or a wallpaper-based Material You theme, in light, dark or system mode.
- **Keep your schedule close.** Add a 2×1, 2×2 or 4×2 widget for current and upcoming periods, with subject colours and theme options.
- **Move your timetable when you need to.** Import during setup or export a `.tempo.json` backup with your subjects, times, breaks, notes, icons and appearance.

## See Tempo in action

<p align="center">
  <img src="docs/screenshots/timetable.jpg" width="250" alt="Tempo’s colourful weekly timetable with subjects, breaks and free periods">
  &nbsp;&nbsp;
  <img src="docs/screenshots/library.jpg" width="250" alt="Tempo’s subject library with individual colours, icons and locations">
</p>

<p align="center"><em>Build a week that looks like yours, then keep its subjects and activities organised in the library.</em></p>

<details>
  <summary>Explore the full settings screen</summary>

  <p align="center"><img src="docs/screenshots/settings-overview.jpg" width="226" alt="Full-height settings screen showing timetable management, themes, day lengths, usual periods, breaks, update controls and sponsorship"></p>

  <p align="center"><em>Theme and block styles, day lengths, usual periods, breaks, update options and support links in one place.</em></p>
</details>

## Widgets at a glance

<p align="center"><img src="docs/screenshots/widgets-all-done.jpg" width="560" alt="Tempo’s 4×2, 2×2 and 2×1 widgets showing the themed All done state after the final lesson of the week"></p>

<p align="center"><em>Three launcher sizes, with a theme-matched “All done” state when the week’s lessons are finished.</em></p>

## Updates and privacy

Your timetable stays on your device unless you export it. Tempo has no accounts or analytics. Update checks contact GitHub; they do not send your timetable. Manual import and export use Android’s file picker.

## What’s next

Multiple-week cycles, calendar `.ics` integration, PNG exports, free tags, share links and optional homework or assignment tracking with widget due dates are planned. See the [roadmap](docs/ROADMAP.md) for details.

## Support Tempo

[Report a bug or suggest a feature](https://github.com/XDanfr/Tempo/issues) · [Sponsor XDan](https://github.com/sponsors/XDanfr) · [xdan.cc](https://xdan.cc)

<details>
  <summary>For contributors</summary>

Open the project in Android Studio with JDK 17 and Android SDK 36. The Gradle wrapper is included:

```sh
./gradlew :core:schedule:test :core:data:testDebugUnitTest :app:testDebugUnitTest :app:lintDebug :app:assembleDebug
```

Release builds use R8 minification and resource shrinking. See [architecture](docs/ARCHITECTURE.md) for module boundaries. Outfit is bundled under the SIL Open Font Licence; its licence is included with the font assets.

</details>
