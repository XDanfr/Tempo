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
