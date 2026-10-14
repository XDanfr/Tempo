# Widget 2.0 implementation log

**Historical first-pass log.** The compact-only renderer and transitional larger
layouts described here have been replaced; see [the full redesign](WIDGET_2_0_REDESIGN.md).

First pass: 14 October 2026. Base: latest `origin/main`; branch `feature/widget-2.0`.

## Audit

All four planning documents were read before choosing a renderer. `WIDGET_2_0.md`
is authoritative; connected experiences are future context only.

- `PillWidget`, `SquareWidget`, `WideWidget` are installed component identities.
  All already allow horizontal/vertical resize. Keep every receiver and ID.
- `WidgetSizing` reads Android 12 size lists (bounded to 16) and legacy portrait /
  landscape option pairs, with font-scale density reduction. The renderer still
  chooses most content by provider identity, so expanding a pill cannot reveal
  the wide composition. This is the architectural seam to replace.
- XML contains native Chronometers; `WidgetArtwork` rasterises catalogue vectors.
  Colours use shared palettes / dynamic schemes and contrast-selected foregrounds.
  Fill, outline, icon, location, completion and timetable preferences live in the
  existing per-ID `widgets` SharedPreferences. No settings reset is needed.
- Configuration saves immediately and on leaving. Option previews are schematic
  Canvas drawings, not actual RemoteViews; compact previews need replacement.
- `weekOverview`, `widgetDayComplete`, `widgetDayPlan` already implement week bounds,
  date exceptions and agenda rules. Presentation must consume them, not reimplement
  cycles, holidays or recurrence.
- WorkManager handles edits, recovery and 15-minute periodic work; inexact boundary
  alarms and boot/time/zone/package receivers restore updates. Native monotonic
  countdowns must remain; no new polling or animation work.
- Existing tests cover sizing, agenda/overlaps, locations, contrast and core dated
  schedules. Host inflation, resizing and TalkBack need device-level checks.

## Official API research and decision

**Decision: defer production Remote Compose; retain RemoteViews and Chronometer.**
This is a first-pass decision, subject to real-host evidence, not a claim that
Remote Compose cannot render on any Android launcher.

Rechecked official sources:

- [Remote Compose alpha21](https://developer.android.com/jetpack/androidx/releases/compose-remote):
  document creation, player, paths, text, colours and state APIs exist. July's
  alpha14 removed a direct SDK 37 requirement, but the alpha21 Compose authoring
  POM pulls Compose 1.11.0. Check the entire graph, not just its own AAR metadata.
- [Glance](https://developer.android.com/jetpack/androidx/releases/glance): 1.2.0
  stable translates to RemoteViews; 1.3 alpha notes require SDK 37 / AGP 9.2.
- [Glance Adaptive alpha03](https://developer.android.com/jetpack/androidx/releases/glance-adaptive):
  public APIs are still forthcoming; alpha02 notes SDK 37.1 transitive requirements.
  A template-first library announcement is not a usable public Tempo host contract.
- [Glance interop](https://developer.android.com/develop/ui/compose/glance/interoperability):
  `AndroidRemoteViews` can retain XML including native countdowns. A migration to
  stable Glance is possible but adds no necessary capability for this first pass.
- [Canonical widgets](https://developer.android.com/design/ui/mobile/guides/widgets/layouts)
  and [sizing](https://developer.android.com/design/ui/mobile/guides/widgets/sizing):
  use a single entity at compact sizes and reveal supplementary content with room.
- [Responsive RemoteViews](https://developer.android.com/develop/ui/views/appwidgets/layouts):
  Android 12 size maps and pre-12 orientation pairs are suitable for the fallback.
- [Material shapes](https://m3.material.io/styles/shape/corner-radius-scale):
  named corner tokens; mirrored connected surfaces share smaller inward corners.
- [Particle transition API](https://developer.android.com/reference/kotlin/androidx/compose/remote/creation/compose/modifier/RemoteEnterTransition):
  an element enter/exit transition does not establish a falling confetti emitter.

The isolated prototype will use low-level document creation and the View player
so it need not introduce Compose 1.11 into the production graph. An in-app player
is explicitly **not** evidence of launcher support. No production renderer switch
or particle animation is justified without host, countdown and accessibility gates.

## Progressive disclosure contract

| Usable dimensions (dp adjusted for font scale) | Composition | First pass |
| --- | --- | --- |
| Short (<136 high) | Compact subject + labelled countdown | New renderer |
| Narrow (<280 wide), 136+ high | Visual entity | Existing fallback, new classification |
| Wide (280+), 136+ high | Hero + related event | Existing fallback, new classification |
| Tall narrow (300+ high) / wide (230+ high) | Non-scrollable agenda | Existing agenda, redesign next |

Thresholds are initial content budgets, not measured launcher guarantees or cell
counts. Compact density must adapt below these budgets and with large fonts.
Keep legacy picker entries until upgrade/device testing supports consolidation.

## Implemented in this pass

`WidgetPresentation` resolves a widget's schedule once before rendering its size
variants. It carries explicit state, subject/block, privacy-filtered location,
countdown target, refresh boundary, progress and the existing day plan. It reuses
`weekOverview`, `widgetDayComplete` and `widgetDayPlan`. No recurrence logic moved
out of `core:schedule`. Rendering does not schedule additional updates.

All three installed provider components now select composition from dp width,
height and font scale. Compact sizes use the new single-entity renderer. Expanding
an existing pill can reveal the existing visual/agenda fallback; shrinking square
or wide instances reaches the same compact renderer. **The larger compositions
are transitional existing layouts, not the completed Widget 2.0 redesign.** No
provider was removed/disabled, no new picker entry was added, and no preference
keys or package/signing settings changed. Keep the three picker entries until an
upgrade/launcher test supports consolidation.

The compact renderer has a prominent native countdown and independent visible
`Left` / `In` label (`Time left` / `Starts in` in the split layout), rather than
relying on Chronometer format strings for the label. At short/wide sizes the
subject and timer occupy mirrored connected segments. Smaller sizes use one
surface, remove secondary context and reduce text budgets before adding clutter.
Long titles can ellipsize at minimum width; full subject text remains in the
TextView for accessibility. Timer width is measured using the actual Outfit font,
including hour counts and system SP conversion. Very small physical widgets at
extreme font scales still require device review; the size classifier is not a
readability guarantee.

Shape rules use Material's extra-large 28dp outside corners, extra-small 4dp
inside corners and 4dp gaps. Tests cover standalone shapes, horizontal/vertical
pairs, middle segments and XML token/corner parity. The pinned Material 3
1.5.0-alpha04 source JAR's `ShapeTokens.kt` confirms these values. The outer
clipping drawable uses the system widget radius on API 31+, with a 28dp fallback.
RTL reverses physical segment corners. Host clipping still needs launcher tests.

Subject fills, theme colour, outlines, dynamic/preset palettes, icon inheritance,
explicit None icons and location privacy continue through the existing settings.
No app-per-second work was introduced. Terminal states hide the countdown;
completion uses a calm static title. Optional particle effects remain unimplemented.

Configuration now inflates the actual compact RemoteViews XML with the selected
schedule/settings and app theme, with preview tap actions disabled. Existing
option-card sketches remain for the larger transitional layouts; replacing them
is part of the next design pass. The preview countdown uses a fresh wall clock
when rebound, so recomposition does not reset elapsed time.

## Artifact inspection and experiment results

Inspection used the published Google Maven POMs, AAR metadata and source JARs,
not API names alone:

| Artifact | Observed direct AAR minimum | Result |
| --- | --- | --- |
| Remote creation Compose alpha21 | SDK 35, AGP 8.6 | Inspected; authoring graph not compiled |
| Compose UI Android 1.11.0 (authoring dependency) | SDK 35, AGP 8.6 | Inspected; do not infer SDK 37 from version alone |
| Glance appwidget 1.3.0-alpha02 | SDK 37, AGP 9.1 | Incompatible with Tempo's SDK 36 baseline |
| Glance Adaptive appwidget alpha03 | SDK 37, AGP 9.1 | Incompatible baseline; public template APIs still forthcoming |
| Remote creation Android + View player alpha21 | Compatible with current toolchain | Separate APK compiled and linted on SDK 36 |

The Glance release notes describe additional Compose/toolchain requirements
(including AGP 9.2); direct AAR minima are not a promise about the full graph.
Conversely, **Remote Compose itself has not been proven to require a toolchain
upgrade**. The low-level lab deliberately avoids changing Tempo's Compose graph.

The opt-in `experiments/remote-compose` APK uses the embedded AndroidX profile.
It serialises native rounded paths using the same geometry source, styled text,
Android 12+ palette colours and a document click area. Buttons change event state
and create two sizes; it reports the serialized byte count. This is document
recreation, not host-driven adaptive expressions. Both `RemoteComposePlayer` and
its listener are library-group restricted in alpha21; a clearly scoped suppression
exists **only** in the lab. That restriction is an additional adoption gap.

A Robolectric API 35 native-graphics test created the Activity, serialized/loaded
four size/state combinations, exercised the state/size buttons and drew the player.
The resulting local images were inspected: mirrored paths, Physics, the state
label and the sample time render. This verifies **local embedded-player rendering
only**, not a public launcher pathway. The lab uses synthetic content and does not yet consume `WidgetPresentation`; snapshot parity is a future renderer gate. The sample clock is static. Document tap
hit testing, real device accessibility, host-driven ticking, automatic palette
changes, icons, native Material special silhouettes and particle enter/exit
transitions have **not** been demonstrated. The SDK 36 public `android.widget.RemoteViews` API was also inspected with `javap`: it exposes no Remote Compose-named bridge. This is evidence about the current public SDK surface, not proof that every newer platform/launcher lacks an integration. No production renderer switch exists.

Stable Glance 1.2 is documented to author RemoteViews, support actions/state and
embed XML via `AndroidRemoteViews`; that offers a potential migration route which
can keep Chronometer. No Glance code or dependency was added to Tempo in this pass,
and no claim of a working Tempo Glance/Remote Compose launcher integration is made.

**Recommendation:** adopt the shared presentation + adaptive RemoteViews/XML/vector
architecture for production. Defer Remote Compose, rather than reject its design
potential. Retest a public host/Glance pathway when available, requiring the same
snapshot, countdown efficiency, accessibility, capability fallback and upgrade
continuity gates. Keep the native countdown even if a future hybrid renderer draws
more expressive decorative geometry.

## Verification and evidence

Local raster evidence (generated from Android layouts/player, not design mock-ups):

- [Minimum compact, 1.5× text](screenshots/widget-2.0-compact-large-text.png)
- [Short wide compact](screenshots/widget-2.0-compact-split.png)
- [Embedded Remote Compose lab](screenshots/widget-2.0-remote-local.png)

Robolectric tests cover compact inflation/rendering at 140×56, 240×72 and
320×100dp, font scales 1.0 and 1.5, API 26 and 35, current/empty/setup/missing
states, separate upcoming/break/day-completion labels, countdown ellipsis, privacy
and per-ID settings retention. These are simulated Android tests, **not emulator
or launcher tests**. Pure tests cover boundaries, progress, free gaps, weekly
completion, sizing and reusable geometry. Existing dated resolver tests remain.

No device or emulator was attached (`adb devices` empty); there is no KVM device
in this workspace. Actual launcher placement, widget IDs across APK upgrades,
Android 12 responsive-map switching without app work, large-text/RTL clipping,
TalkBack reading/ticking, inexact alarms, Doze/battery saver, reboot/zone/time
changes and process/host recreation remain device acceptance gates. Android 16
Remote Compose widget compatibility and a public capability/fallback mechanism
are unverified. No particle appearance, reduced-motion behaviour or once-per-day
replay semantics have been tested because no animation ships.

## Next implementation tasks

1. Design and compare current/upcoming/break/free/completed states for square,
   wide and expanded canonical compositions, including tall narrow variants.
   Retain the hero's subject + labelled countdown and review 4/5/6-column measurements.
2. Replace the square fallback with a meaningful Material countdown/progress
   silhouette and contextual next event; keep labels and SP budgets accessible.
3. Replace the wide fallback with a deliberate hero + one related event, sharing
   mirrored corner tokens and respecting fill/outline/privacy preferences.
4. Build the expanded non-scrollable mini-agenda from the existing day plan and
   dated resolver, with measured row budgets and honest overflow counts.
5. Move remaining larger-layout bindings into snapshot-based renderers, retire
   unreachable legacy pill branches and replace their configuration/picker previews.
   Audit whether next-day taps need a focused date route beyond the existing
   timetable-open action.
6. Add actual AppWidgetHost/launcher tests and upgrade fixtures before deciding on
   one preferred picker entry. Retain all legacy receivers and saved IDs regardless.
7. Run the documented Android/launcher/accessibility/power matrix, capture real
   resize videos and binder/update measurements, and revisit Remote Compose's
   production gate independently. Scrolling and connected experiences remain deferred.

### Build environment notes

The workspace initially had Java 21 but no Android SDK or JDK 17. SDK 36,
build-tools 36.0.0 and platform-tools were installed outside the repository;
Gradle provisioned JDK 17. Proxy configuration and CA trust for that downloaded
JDK were environment-only changes. One combined build exhausted the workspace's
memory while idle Gradle/Kotlin daemons were still resident. Stopping those and
using one worker / a 3 GiB Gradle heap recovered validation. No SDK, AGP, Kotlin,
application ID, release signing or production dependency upgrades were made.

### Final first-pass results

Base main commit: `9c66a9b7fee6ae42eba8d2dcc3eca4427cc18e2c`.

| Check | Result |
| --- | --- |
| `:core:model:test` | 6 passed |
| `:core:schedule:test` | 43 passed |
| `:core:data:testDebugUnitTest` | 29 passed |
| `:app:testDebugUnitTest` | 45 passed (including API 26/35 native-graphics cases) |
| Isolated lab `testDebugUnitTest` | 1 passed, four local size/state raster captures |
| App `lintDebug`, `lintRelease` | Both passed: 0 errors, 94 warnings each |
| Isolated lab `lintDebug` | Passed: 0 errors, 13 warnings |
| App `assembleDebug`, `assembleQa`, `assembleDebugAndroidTest` | Passed |
| App `assembleRelease` | Passed with R8 and resource shrinking; unsigned APK |
| Isolated lab `assembleDebug` | Passed on unchanged SDK 36 / AGP / Kotlin |
| `git diff --check` | Passed |
| Real launcher / connected instrumentation / signed installation | Not run: no attached device/emulator or release signing setup |

**124 tests passed in total.** Lint warnings include hardcoded text and API-gated
widget attributes; zero errors does not mean warning-free. Compiler warnings in
existing app UI remain. Native-library strip messages did not fail packaging.
The final normal production validation completed successfully with the optional
experiment excluded. These are local checks; GitHub CI was not triggered.

Reproduction commands (with SDK/JDK installed and normal Maven access):

```sh
./gradlew :core:model:test :core:schedule:test :core:data:testDebugUnitTest
./gradlew :app:testDebugUnitTest :app:lintDebug :app:assembleDebug \
  :app:assembleDebugAndroidTest :app:lintRelease :app:assembleRelease :app:assembleQa
./gradlew -PtempoRemoteCompose=true :experiments:remote-compose:testDebugUnitTest \
  :experiments:remote-compose:lintDebug :experiments:remote-compose:assembleDebug
```

For the memory-limited workspace the production command also used
`--max-workers=1 -Dorg.gradle.jvmargs='-Xmx3g -Dfile.encoding=UTF-8'
-Pkotlin.compiler.execution.strategy=in-process`. Reports are under each module's
`build/test-results` and `build/reports`; generated PNGs are under
`app/build/reports/widget-previews` and the lab's `build/reports/remote-previews`.

The final release APK's badging confirms `cc.xdan.tempo`, min SDK 26, target SDK
36, compile SDK 36 and unchanged version identity. Source comparison confirms the
provider manifest, widget preference implementation, all core modules, version
catalogue and wrapper are unchanged from main. The compact renderer honours
saved themed-lesson fills on resize, does not rasterise hidden icons and permits
a second subject line in the 240×72dp split layout. A native raster regression
check protects the themed-lesson preference.

Implementation commits: `0dfcba9` (audit), `9ec7ed3` (presentation/geometry),
`dae680e` (isolated lab), `1b684a5` (compact renderer/preview), `f0ceb08`
(rendering tests/labels), `b5dceda` (theme preference/wrapping).
