# Widget 2.0 — composition and configuration replacement

The first pass only replaced compact rendering. This pass replaces the entire
presentation tree and configuration surface. Existing receivers, preference keys,
resolver and boundary/recovery scheduling are compatibility foundations.

## Design before rendering

| Composition | Structure | Priority |
| --- | --- | --- |
| Compact | Connected subject and countdown segments where wide enough; single entity at minimum width | Subject + live labelled time |
| Square | Status/subject above a square Material Cookie9Sided countdown silhouette | Subject, dominant time, optional location |
| Wide | Connected subject + timer hero, one uncontained next-event line | Current event, dominant time, next event |
| Expanded | Same hero above an ordered connected vertical mini-agenda | Hero, next events, finish and honest overflow |

Current / upcoming / break use the same hierarchy with truthful status and target.
Missing / setup / empty / complete replace the live timer with calm status content.
No gradient, scrolling, inert action icon or decorative animation. Subject colours
organise the event; special silhouettes identify the time/status. Rectangular
segments share 28dp outer / 4dp inner corners, including vertical middle segments.

Settings are one provider-independent screen: live size preview, timetable,
appearance and detail sections. All options apply after resize; no pill/square/wide
branching. Existing IDs and all preference keys remain intact. A new Adaptive
Tempo picker entry is preferred; old entries stay installed for upgrade safety.

## Rendering boundary and toolchain

Official release pages rechecked 14 October 2026: Glance 1.3.0-alpha02 remains the
latest alpha; Adaptive is 1.0.0-alpha03; Remote Compose is 1.0.0-alpha21. Adaptive
still says public APIs are forthcoming. Source inspection confirms its template
registry, archetype selection and widget delegate are library-group restricted.

The owner explicitly chose latest-alpha adoption over retaining SDK 36 tooling.
The branch now uses **Glance 1.3.0-alpha02**, compile SDK **37.0**, **AGP 9.2.1** and
**Gradle 9.4.1**. AGP's built-in Kotlin replaces `org.jetbrains.kotlin.android`;
existing Kotlin 2.2.21 compiler plugins remain. Resource-value generation is now
explicit because AGP 9 disables it by default. Wrapper checksum is pinned to the
published Gradle checksum. Android minimum **26** and target **36** are unchanged.
No package, version identity, release signing or timetable engine was changed.

Unlike the previous XML templates, composition, sizing, text, progress and icon
placement are authored in Glance. XML interop is limited to a **fitted native
TextView for duration text**; Glance places reusable XML drawable surfaces directly. Each update
composes an immutable schedule snapshot once per reported size, with explicit
activity PendingIntents; no live session or per-second app work is needed.

### Source findings that change the earlier feasibility assessment

The first pass inspected the SDK36 surface and the embedded player, but did not
inspect Glance alpha's full Remote Compose backend. The alpha02 published source
contains a real bridge: `DrawInstructionRemoteViews` uses
`RemoteViews.DrawInstructions.Builder` and `RemoteViews(drawInstructions)`.
`GlanceRemoteComposeProfile` selects `WIDGETS_V7`. The backend override is a
**library-group restricted debugging API**, gated by Glance at API36+.
This is a concrete platform pathway to test, not proof of operation on a launcher.

The same translator handles rows, columns, boxes, images and text, but its
`translateEmittable` dispatch has no `EmittableAndroidRemoteViews` / Chronometer
case. Unknown elements become spacers. Consequently forcing Tempo's production
composition through that backend would silently lose the live countdown.
**Production uses the alpha's RemoteViews path** until equivalent host ticking,
accessibility and failure detection are proven. This is a renderer limitation,
not an excuse to retain the old visual templates.

### Isolated experiment

`-PtempoRemoteCompose=true` includes a separate APK with:

- Existing low-level Remote Compose alpha21 document / embedded-player experiment.
- Glance alpha02 + Adaptive alpha03 template registration, tier selection,
  event-state changes and two sizes, using restricted APIs only inside the lab.
- An RV / Remote Compose switch for local comparison. The sample time is explicitly
  static. The switch uses the alpha's restricted backend debug override.
- A clearly named **EXPERIMENT · Remote Compose probe** launcher widget for
  API36+ host testing. Its static sample and tap open the lab. It has no background
  periodic work and no production widget data or IDs. Generation failure or an
  older OS produces an explanatory native fallback. A host-side asynchronous
  rendering failure cannot be caught by the provider; do not treat that fallback
  as production capability detection.

The prototype does not claim a live countdown, particles, stable Adaptive APIs or
successful real-launcher drawing. Adaptive has an actual registry in the published
source, but it is still restricted, rather than a supported public production API.

## Official evidence

- [Glance releases](https://developer.android.com/jetpack/androidx/releases/glance)
- [Adaptive releases](https://developer.android.com/jetpack/androidx/releases/glance-adaptive)
- [Remote Compose releases](https://developer.android.com/jetpack/androidx/releases/compose-remote)
- [AGP 9.2 compatibility](https://developer.android.com/build/releases/agp-9-2-0-release-notes)
- [AGP built-in Kotlin migration](https://developer.android.com/build/migrate-to-built-in-kotlin)
- [Canonical layouts](https://developer.android.com/design/ui/mobile/guides/widgets/layouts)
- [Glance interop](https://developer.android.com/develop/ui/compose/glance/interoperability)

Source findings above are from published Google Maven source JARs at exactly the
pinned versions, especially `Backend.kt`, `NormalizeCompositionTree.kt`,
`remotecompose/GlanceRemoteComposeTranslator.kt`, `DrawInstructionRemoteViews.kt`,
and Adaptive's `AppWidgetTemplateRegistry.kt` / `AppWidgetSizeTiers.kt`.

## Captures and verification

These are rendered Android views from the production Glance composition, not
concept art or screenshots from a real launcher:

- [Square visual entity](screenshots/widget-2.0-rewrite-square.png)
- [Wide hero and next event](screenshots/widget-2.0-rewrite-wide.png)
- [Expanded static agenda](screenshots/widget-2.0-rewrite-agenda.png)
- [Dark theme, long subject and 1.5× text](screenshots/widget-2.0-rewrite-large-text.png)

Local Robolectric checks cover API26/35, 140×56 through 360×350 and tall 180×420dp,
font scale 1.0/1.5, current/upcoming/break/day-complete/week-complete, missing/setup/
empty states, native countdowns, location privacy and per-ID settings retention.
Shape asset tests include mirrored horizontal ends and vertical middle segments.
The badge shrinks independently of typography; at smaller accessible sizes its
label moves outside the silhouette to keep contrast on a known background.
Tall narrow single-event widgets retain the square/visual composition.

The isolated lab has four passing checks: one embedded-player raster test,
Adaptive template selection/state tests on API35 and API37, and Remote Compose
**draw-instruction generation and parceling** on simulated API37. This does not
verify that an actual launcher accepts or draws the document, dispatches its tap,
or supports a live timer. The lab builds and lints on the upgraded toolchain.
Its API37 simulation uses a Java21 test launcher (APK bytecode remains Java17).

No device/emulator is attached in this workspace. Launcher resize/placement,
upgrade continuity, accessibility, battery/Doze/reboot recovery, Material You on
a device and real-host Remote Compose behaviour remain device acceptance gates.
The renderer keeps explicit subject-icon None and privacy choices. A progress bar
is a schedule-boundary snapshot; it deliberately does not imply smooth ticking.

CI now builds an installable `Tempo-QA` artifact and a separate
`Tempo-Remote-Compose-Lab` artifact on the feature branch. The latter contains the
experimental Adaptive and RC probe, not production widget IDs or user data.
Both are debug-signed testing APKs; release signing has not been changed.

## Launcher feedback: duration, timing and resize

A device screenshot exposed truncated hour-long clocks in the visual badge and
wide hero. Fitted text leaves now measure the actual inflated TextView font and
reserve space for rounding. Compact height budgets account for large text as
well as the subject line. Following the owner's subsequent request, **all timed
states** use hours/minutes: `Starts in 1h 29m` before an event and `45m left` during
lessons and breaks (separate panels retain the Time left label). Long waits use
days/hours. The previous active-only Chronometer has been removed, so the format
no longer changes depending on whether the timetable is free or occupied.

A static duration cannot tick itself in an ordinary RemoteViews host. The public
Chronometer API formats a running clock, not `xh ym`; Glance cannot supply an
arbitrary host-side text updater. Previously an inexact alarm then queued
WorkManager. [Android's alarm guidance](https://developer.android.com/develop/background-work/services/alarms/schedule)
allows inexact delivery up to an hour late on Android 12+, even before additional
battery-saving delays. This invalidates relying on that alarm for fresh minute
labels after unlocking.

Delivered alarms, clock changes and screen-on/unlock signals now render directly
with an eight-second budget; superseded requests are cancelled and failures use
WorkManager recovery. A process-registered receiver observes minute ticks and
user-present signals while the process survives; it skips ticks until a displayed
label/schedule actually needs updating. It is unregistered when no widgets remain.
Leaving Tempo also requests an immediate refresh. These signals are opportunistic:
Android does not provide a reliable launcher-visibility callback and a dead/cached
process cannot be assumed to receive every runtime broadcast promptly.

The optional **Accurate widget timing** control in widget settings opens Android's
Alarms & reminders special access page. Nothing requests this access automatically.
When allowed, one `setExact(RTC, ...)` PendingIntent alarm covers the earliest
minute/hour-label transition or schedule boundary across all placed widgets.
RTC is non-wakeup: the app does not wake the device for each minute, use exact
idle alarms, poll each second or run a keep-alive foreground service. Android 8–11
supports this without special access. On Android 12+, denial/revocation uses the
inexact path and the UI explains possible delayed labels. Grant broadcasts refresh
and reschedule; periodic work recovers after process death/reboot or revocation.
Doze recovery, OEM restrictions and visible unlock-to-refresh latency still need
device testing. The app cannot promise instant refresh under all battery policies.

Resize broadcasts render only the affected widget directly with `goAsync`,
coalescing superseded gestures. Per-widget rendering locks and cancellation checks
prevent an older resize from publishing after a newer one. The regular worker
shares this renderer and remains the retry/recovery path. Resize rendering has an
eight-second budget and does not move the global alarm. Launchers control when
they deliver size changes and apply RemoteViews.

Regression coverage includes current/upcoming/break duration rendering, fitted
hour-long text at 1×/1.5×/2× text, rounding/refresh transitions for all timed states,
clock receiver registration and due-time filtering, exact/inexact timing access
and non-wakeup batching, and preserving other widgets/settings/alarms on resize.
Captures are production Glance snapshots in Robolectric, not real launchers.

Local follow-up validation: 55 app tests pass on simulated API26/35, debug lint
passes, and the QA APK builds. The configuration preview refreshes at its next
label/schedule boundary while resumed and refreshes immediately when reopened.
