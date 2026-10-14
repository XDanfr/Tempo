# Architecture

- `core:model`: serialisable, platform-independent values. Day intervals use minutes since local midnight; cycle anchors, holiday ranges and exceptions use ISO local dates. Explicit zones remain future work.
- `core:schedule`: pure Kotlin resolution, gaps and conflicts. UI, widgets and exports must consume this engine.
- `core:data`: repository and atomic local storage. A versioned DataStore collection stores multiple complete snapshots atomically; Room can replace this when occurrence queries and history require it. No synchronous disk writes on the UI thread.
- `core:designsystem`: shared palettes and reusable Compose styling.
- `app`: onboarding, editor, settings and navigation. Split feature modules as those flows grow.

One coherent change per commit. Add behavioural tests around schedule edge cases and import validation. Do not introduce placeholder controls for unimplemented features. Advanced recurrence, overnight shifts and DST require dated occurrence models before being exposed in the UI.

No accounts or analytics. Network permission is used only for opt-in GitHub release updates. Timetables are never uploaded by the updater. No licence has been assumed; the owner can choose one before public distribution.

AI assistance: initial project and implementation authored with OpenAI Codex, reviewed through build checks and domain tests. Device testing remains necessary.

Material 3 Expressive is pinned to `1.5.0-alpha04`: stable 1.4 hides its Expressive theme APIs. The experimental dependency is isolated in the design system and should be revisited as APIs stabilise.

## Feedback milestone

Schema 2 adds recurring scheduled breaks and outlined block appearance. Version 1 is migrated before strict decoding; subjects, sessions, day hours, legacy wallpaper selection and interrupted onboarding are preserved. The new onboarding has four steps, so v1 subjects/appearance steps move to v2 steps 2/3.

Editors use one reusable Material modal bottom sheet. Material owns editor predictive-back animations. Top-level tab navigation uses `PredictiveBackHandler` with a preview of Today and cancellation recovery; Today itself does not consume system Back. Onboarding returns to the previous step. Native Compose animation duration scaling is retained.

Breaks occupy the same scheduling engine as sessions, preventing them becoming frees; all real overlaps remain visible with warnings. Frees are calculated, so tapping one creates a session or break using that gap's exact time range rather than mutating a fictitious free record.

## Timetable files and collections

The existing `tempo_v1` store and `timetable` key are retained. Single schema-1/2
snapshots migrate into a collection with the stable ID `local`; the first write
saves the collection envelope. Collection IDs are independent of subject/session
IDs, so imported or duplicated snapshots cannot collide with another timetable.
Transforms capture their target ID before launching and run inside one DataStore
transaction. Deleting the active timetable selects the first remaining entry;
the last timetable cannot be deleted. Switching or replacing a timetable resets
its editors to avoid carrying unsaved state into the new snapshot.

Portable JSON has `format: cc.xdan.tempo.timetable`, `formatVersion: 1`, and one
complete `timetable` object. Files include subject colours/icons/locations,
sessions and notes, repeated breaks, day hours, periods and appearance. Imports
also accept legacy schema-1/2 snapshots. They reject unknown versions/fields,
invalid IDs/references/times/colours, damaged UTF-8, files above 2 MiB and nesting beyond 32.
Decode precedes preview; users choose a new timetable or explicitly confirm
replacement. Empty onboarding can use an imported timetable directly. File I/O
runs off the main thread through Android's document picker; no storage permission
is required. Export captures the selected snapshot before opening the save picker.

`Appearance.breakStyle` is optional for old snapshots: missing values inherit the
old global `blockStyle`. The two controls materialise independent styles when
edited. The icon picker uses a fixed-height sheet with a single scrolling grid
and no sheet drag gestures; all other editors retain their drag behaviour.

## Future link sharing

The intended route is `https://xdan.cc/tempo/share/<code>`. This milestone does
not publish links or implement a server. A later service should store the same
portable envelope and return a code such as `t1_<token>_<check>`: `t1` marks the
format, `token` encodes 16 random bytes as 22 unpadded Base64URL characters, and
`check` is the first eight lowercase hex characters of SHA-256 over `t1_<token>`.
The client and server must validate the whole anchored syntax, decode the token
canonically and verify the check before looking up a snapshot. The check detects
copying errors; it is not an authentication signature. Never encode timetable
contents or names into the URL. Publishing should be explicit, with an import
preview on receipt and a way for the publisher to remove a shared snapshot.


## Comparison, widgets and updates

Comparison splits all resolved schedules on their combined boundaries and retains every occupied block. An interval is shared free time only when every timetable has a calculated Free block there; outside-day time is not implicitly available.

Home-screen widgets are authored in Glance 1.3.0-alpha02 and translated to RemoteViews.
One Adaptive Tempo picker entry and the three retained compatibility receivers use
the same snapshot renderer. The legacy provider identities and per-ID preference
keys stay installed; no conversion of placed widgets or preferences is required.
Reported dp sizes and font scale select distinct compact, visual square, wide
hero/next-event and expanded static agenda compositions. A Glance composition
snapshot runs per reported size, then Android 12+ size maps or pre-12 orientation
pairs handle host selection. Only the native Chronometer uses XML interop; all
layout, text, image and progress placement is authored in Glance. Actions use
explicit activity intents rather than session-bound lambda callbacks.

A WorkManager refresh reads the same DataStore collection and shared resolver.
Edits and active selection changes trigger refreshes while the app is visible.
Reboot, clock changes and package replacement restore refresh work; a 15-minute
periodic job provides recovery. Inexact boundary alarms change displayed activities
without exact-alarm access. Android may delay these updates. Countdown bases use
monotonic time; no per-second application work or bitmap countdown text is used.
Per-widget settings are independent of portable snapshots; deleted fixed tables
ask for reconfiguration rather than silently switching to the active timetable.

The updater uses stable GitHub releases only. Metadata carries a format version, version name/code, APK filename and SHA-256. Downloads are confined to official tagged release assets. Before opening the installer, Tempo checks the checksum, package ID, increasing version code and exact signing-certificate set. It does not silently install, access repository secrets, or send timetable contents. Signing-key rotation would require explicit lineage support in a later version. Automatic work uses an unmetered network; APK downloads use DownloadManager. Installation is through a narrow FileProvider URI and Android’s per-app installation permission.


Comparison is opened from the timetable manager as a full-screen view. Side-by-side columns share one vertical time axis and horizontal scroll position; overlapping occupied blocks use stable greedy interval lanes. Together mode lists each timetable’s activities with subject colours/icons and owner labels alongside shared gaps.

Widgets retain the week-bounded `weekOverview`, `widgetDayComplete` and
`widgetDayPlan` semantics. `WidgetPresentation` resolves schedule state, targets,
boundaries, progress, locations and ordered later activities before rendering.
`WidgetSizing` and `WidgetLayoutPlan` own the content budgets, independently of
provider identity or launcher column count. The agenda stays on the hero's date,
includes overlapping lessons/breaks, and has no scrolling or invented free periods.

Rectangular geometry uses shared Material 28dp outer and 4dp inner tokens. Horizontal
pairs mirror under RTL; vertical agendas have matching ends and identical middle
segments. The square's timer uses the pinned Material3 `Cookie9Sided` silhouette,
rasterised at its actual square size; text and countdown remain native accessible
views. Subject colours, theme shades, Material You, presets, location/icon
inheritance and outlines are retained. Progress is a boundary-refresh snapshot,
not a per-second animation. All configuration sizes preview the actual Glance
renderer. One settings screen applies to every current and future size.

The opt-in `experiments/remote-compose` APK contains Remote Compose alpha21's
embedded player, a Glance alpha02 / Adaptive alpha03 registry probe and a separate
experimental launcher placement. Adaptive registry and backend override APIs are
library-group restricted; those suppressions remain isolated to the lab. No
production library restriction is suppressed. Glance's Remote Compose translator
does not translate `AndroidRemoteViews` / native Chronometer, so the production
composition intentionally uses the RemoteViews backend. A draw-instruction
constructor in SDK 37 is evidence of an API path, not verified launcher support.

Following explicit approval to prioritise the latest Glance alpha, the branch uses
compile SDK 37.0, AGP 9.2.1, Gradle 9.4.1 and AGP's built-in Kotlin support (the
existing Kotlin 2.2.21 compiler plugins remain). Target SDK 36, min SDK 26, package
names and signing configuration are unchanged. See [redesign results](WIDGET_2_0_REDESIGN.md)
and the historical [first-pass log](WIDGET_2_0_IMPLEMENTATION.md).
