# Architecture

- `core:model`: serialisable, platform-independent values. Minutes since local midnight are used only for weekly templates; resolved dates will carry explicit zones.
- `core:schedule`: pure Kotlin resolution, gaps and conflicts. UI, widgets and exports must consume this engine.
- `core:data`: repository and atomic local storage. A versioned DataStore collection stores multiple complete snapshots atomically; Room can replace this when occurrence queries and history require it. No synchronous disk writes on the UI thread.
- `core:designsystem`: shared palettes and reusable Compose styling.
- `app`: onboarding, editor, settings and navigation. Split feature modules as those flows grow.

One coherent change per commit. Add behavioural tests around schedule edge cases and import validation. Do not introduce placeholder controls for unimplemented features. Advanced recurrence, overnight shifts and DST require dated occurrence models before being exposed in the UI.

No accounts, analytics or network permissions. No licence has been assumed; the owner can choose one before public distribution.

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
invalid IDs/references/times/colours, files above 2 MiB and nesting beyond 32.
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
