# Architecture

- `core:model`: serialisable, platform-independent values. Minutes since local midnight are used only for weekly templates; resolved dates will carry explicit zones.
- `core:schedule`: pure Kotlin resolution, gaps and conflicts. UI, widgets and exports must consume this engine.
- `core:data`: repository and atomic local storage. The first single-timetable milestone uses a versioned DataStore snapshot; Room replaces this when multiple timetables, queries and occurrence history arrive. No synchronous disk writes on the UI thread.
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
