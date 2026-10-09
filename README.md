# Tempo

Your week, in view. A flexible Android timetable maker for college, work and everything around it — part of **Axis**.

I’m building Tempo to make recurring schedules easy to create, read and share, with a native Material 3 Expressive interface. **Made by XDan.**

## Get Tempo

Download the signed APK from [GitHub Releases](https://github.com/XDanfr/Tempo/releases) and install it on Android 8.0 or newer. Tempo is in early development; export a `.tempo.json` backup before trying development builds.

A signed release can update an earlier release in place when both use the same signing key. Development APKs can use a different key, so Android may reject an update over them.

## Make it yours

- Create separate timetables for college, work or personal activities, then switch between them.
- Set day hours, usual periods and custom session times. Add breaks, lunch and changeover time.
- Give subjects names, colours, locations and optional icons from a searchable picker.
- See Today and proportional day timelines, with dashed frees you can tap to fill.
- Open comparison from Your timetables: view coloured schedules side by side, or combine people’s lessons into one agenda and find shared frees.
- Choose Forest, Ocean, Amber or wallpaper-based Material You, with light, dark and system modes. Style subjects and breaks independently as filled blocks or outlines.
- Import a timetable during setup, or export a complete `.tempo.json` file including subjects, times, breaks, notes, icons and appearance.

## On your home screen

Add Tempo from your launcher’s widget picker. Choose the **2×1 pill**, **2×2 rounded square** or **4×2 rounded rectangle**. Each has a fixed footprint to keep its proportions; launcher grid sizes can vary.

Each widget can follow your current timetable or stay on a fixed one. It shows the current session or break and time left, or the next activity and time until it starts. Countdowns also cover later days within this week. After the final lesson of the week, it shows All done until the new week begins. The 4×2 pairs a centred current/next period with up to two large, readable upcoming-period cards, times, rooms and your finish time. Lesson panels use the same subject colours as the app. If more periods remain than fit, it shows how many more there are.

Touch and hold a widget and open your launcher’s widget settings to choose its own theme or follow the timetable, choose icon visibility, hide locations and customise its empty message. Subject colours and icons carry through to the widget. The pill and square use a vibrant theme background by default, with a lesson-colour option. All sizes can switch crisp 2dp outlines on or off using visual options. Dark-mode borders use the bright theme colour shown in the preview. The wide widget outlines its frame and lesson panels, while completion content stays unoutlined; its next-lesson panel can retain a border. The wide widget can also use darker theme shades instead of subject colours for its lesson panels. All three sizes share the same bright theme palette in light and dark mode. After today’s last lesson, the square centres Done for today above a subject-coloured next-lesson card with its icon and countdown. The wide widget places completion on the left and the next lesson on the right; the square can instead show just the completion message. The pill can optionally replace the next lesson with Done for today after today’s lessons. All done for the week always takes priority, with a large centred title and a completion badge coloured from the widget theme, including on the pill. Android’s battery-saving modes may delay activity changes; the countdown runs through the launcher without Tempo waking every second.

## Updates and privacy

In Settings, check for stable GitHub releases or enable automatic updates. Automatic updates check daily and download on an unmetered connection; Android asks before installation. Update files are checked against release metadata and the installed app’s signing certificate.

Timetables stay on your device unless you export them. Tempo has no accounts or analytics. Update checks contact GitHub; timetable contents are not sent. Manual import/export uses Android’s file picker. Automatic updates need a public stable release with the supplied update metadata.

## What’s next

Multiple-week cycles, calendar `.ics` integration, PNG exports, free tags and share links are planned. See the [roadmap](docs/ROADMAP.md). They are not available yet.

## Support and feedback

[Report a bug or suggest a feature](https://github.com/XDanfr/Tempo/issues) · [Sponsor XDan](https://github.com/sponsors/XDanfr) · [xdan.cc](https://xdan.cc)

## For contributors

Open the project in Android Studio with JDK 17 and Android SDK 36. The Gradle wrapper is included:

```sh
./gradlew :core:schedule:test :core:data:testDebugUnitTest :app:testDebugUnitTest :app:lintDebug :app:assembleDebug
```

Release builds use R8 minification and resource shrinking. See [architecture](docs/ARCHITECTURE.md) for the module boundaries. Outfit is bundled under the SIL Open Font Licence; its licence is included with the font assets.
