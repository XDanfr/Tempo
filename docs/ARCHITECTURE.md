# Architecture

- `core:model`: serialisable, platform-independent values. Minutes since local midnight are used only for weekly templates; resolved dates will carry explicit zones.
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

Home-screen widgets use RemoteViews and the native Chronometer countdown instead of per-second app work. Three providers request fixed 2×1, 2×2 and 4×2 launcher footprints (`resizeMode="none"`). Each uses its own initial/preview layout. Reconfiguration uses the launcher’s hold-to-configure action; the layouts have no menu button. A compact layout serves the pill; larger widgets expose more detail. Per-widget preferences are independent of portable timetable snapshots. Fixed timetable IDs never fall back to another timetable when deleted: the widget asks to be configured again.

A WorkManager refresh reads the same DataStore collection and shared resolver. Edits and active selection changes trigger refreshes while the app is visible. Reboot, clock changes and package replacement restore refresh work; a 15-minute periodic job provides recovery. Inexact boundary alarms change displayed activities without requesting exact-alarm access. Android may delay these updates. Countdown bases use monotonic time; dated starts use the device’s current zone.

The updater uses stable GitHub releases only. Metadata carries a format version, version name/code, APK filename and SHA-256. Downloads are confined to official tagged release assets. Before opening the installer, Tempo checks the checksum, package ID, increasing version code and exact signing-certificate set. It does not silently install, access repository secrets, or send timetable contents. Signing-key rotation would require explicit lineage support in a later version. Automatic work uses an unmetered network; APK downloads use DownloadManager. Installation is through a narrow FileProvider URI and Android’s per-app installation permission.


Comparison is opened from the timetable manager as a full-screen view. Side-by-side columns share one vertical time axis and horizontal scroll position; overlapping occupied blocks use stable greedy interval lanes. Together mode lists each timetable’s activities with subject colours/icons and owner labels alongside shared gaps.

Widgets use a week-bounded overview: Sunday is the last day of the current weekly template, and Monday restores upcoming activities. After the final lesson, the widget shows All done instead of pulling in next week. A timetable with no lessons retains its custom empty text. The general app overview still looks ahead across week boundaries. RemoteViews contain coloured activity surfaces, vector icons rasterised from the app’s icon catalogue and native drawable fills/outlines with fixed dp radii, native countdowns, and a wide layout with a day agenda alongside the current/next activity. `widgetDayPlan` anchors this agenda to the hero’s date, includes live overlaps and breaks before the last lesson, excludes completed activities and free gaps, and supplies the remaining lesson count and finish time. The available height/font scale determines whether one or two larger agenda rows fit, with an overflow count on the last row. Completed/empty weeks and single-period days use the single-surface layout, so the root fill occupies the full widget without a nested frame. The agenda has no day heading or synthetic Last period card. Per-widget icon visibility can inherit the timetable; lesson cards use exact subject fills regardless of timetable block style.

The pill and square default to a brighter tint of the selected theme’s primary fixed colour; its independent lesson-background preference is persisted alongside the existing theme, icon and block-style overrides. No timetable-name banner consumes widget space.

`widgetDayComplete` is distinct from weekly completion: it requires a next lesson on a later date within this week and no unfinished lessons today. Square widgets can choose a message-only end-of-day state. `tempo_widget_done_today` centres completion above a subject-coloured next-lesson card; `tempo_widget_done_today_wide` puts the completion heading/badge on the left and next lesson/icon/countdown on the right; the future-day agenda is hidden during this completion state. Weekly All done overrides both. Widget outline visibility is independent of filled/outlined block style. Text/icon foregrounds are selected for contrast against the actual fill.

Pill/square background selection is independent of timetable block style: selecting Lesson colour always uses the subject fill. Their outline switch controls the border; the wide layout independently supports subject fills or darker theme shades and a border switch. Borders use the same primary role and 2dp width as the preview. In wide completion states, the theme-matching completion content/root has no outline; the next-lesson panel still honours the border switch.

Square/wide primary content is centred, with a native expressive badge and a room pill. Secondary period titles use 18sp and details 13sp, with at most two rows (one in shorter cells). Layout thresholds account for system font scale. All three widget sizes use the established pill theme palette, with .70 dark/.82 light primary-fixed tint. Lesson fills use the app’s exact subject colour in both modes unless a wide widget selects themedLessons; its current/upcoming fills are separate darker theme shades. Single-lesson wide widgets also have an inset lesson panel. Completion badges use the widget theme’s primary-fixed roles, independent of the next lesson. `tempo_widget_pill_done` centres an expressive badge beside a larger All done title. Normal pill geometry is retained. The pill can opt into a message-only Done for today state through doneForDayMessage; weekly All done always overrides it. The horizontal completion badge uses center_vertical layout gravity to align with the title. Weekly completion titles are 28–36sp on square/wide and 22sp on the pill. Background, border, wide lesson colours and daily-completion choices use selectable preview cards; timetable/theme/icon selectors keep multi-option dropdowns.
