# Tempo roadmap

I’m building Tempo as a timetable maker for college, work and personal schedules within the Axis ecosystem. This roadmap describes implemented work and what I intend to add; planned features can change as I test the app and hear from users.

## 0.3 — timetables you can share as files

- [x] Multiple timetables: create, switch, rename, duplicate and delete
- [x] Complete `.tempo.json` import/export with validation and preview
- [x] Import during onboarding
- [x] Independent fill/outline styles for subjects and breaks
- [x] Next-session details during breaks
- [x] Picker scrolling and complete theme palettes
- [x] Signed release workflow, R8 and resource shrinking

## 0.4 — comparison, widgets and release updates

- [x] Compare selected timetables on aligned time intervals, including overlapping activities
- [x] Shared free-time totals inside everyone’s configured day hours
- [x] Current/next widgets with native countdowns
- [x] Fully rounded 2×1, rounded-square 2×2 and rounded-rectangle 4×2 widgets
- [x] Fixed timetable or follow the currently selected timetable
- [x] Independent widget theme or follow timetable appearance; location privacy and custom empty message
- [x] Updates after edits, selection changes, reboot and clock/time-zone changes, with periodic recovery
- [x] Optional automatic GitHub stable-release checks and downloads; verified APKs and Android installation confirmation
- [x] Release update metadata generated from the signed APK
- [x] Sponsor button and “Made by XDan” in Settings
- [X] Validate widget sizing/gestures and signed update installation on devices before release

## 0.5 — dated schedules and image exports

- [x] Shared date resolver for the app, comparison, widgets and image exports
- [x] A/B and longer cycles (1–52 weeks), anchored to a date in week 1
- [x] Inclusive holiday ranges and dated replacement schedules / days off
- [x] Session duplication and splitting, selective recurring-session batch editing
- [x] Session-local undo/redo for saved timetable and library changes
- [x] PNG day/week agenda exports with an exact preview, configurable width and privacy controls
- [x] Schema-1/2 migration and complete schema-3 portable snapshots
- [x] Calendar/clock pickers, common cycle suggestions and singular session labels
- [x] Stable scrolling editor panels with scope choices inside editing flows
- [x] Settings pages with transitions, predictive back, navigation icons and inline About content
- [x] Compact day-hours and period dialogs; independent PNG themes with System selected by default
- [x] Scrollable widget timetable/icon/location pills and shared theme cards, with location following
- [x] Editable onboarding subject cards and initial anchored cycle length

## 0.5.1 — colour and everyday polish

- [x] Expand subject colours from six to twelve, including red, orange, lemon, teal, blue and magenta; preserve existing saved colours
- [x] Add Red, Violet and Rose app themes with complete light/dark palettes, shared widget/export selectors and accurate theme previews
- [x] Animate theme colour changes, strengthen theme colour in dark surfaces, and tune Red to read as saturated red
- [x] Apply widget theme choices immediately and save widget setup when leaving for the home screen
- [x] Readable subject text and comparison fills, contrasting outlines/icons, named colour controls and explicit selection state
- [x] Friendly display dates such as “11th October 2026”, including ordinal edge cases and locale-aware alternatives; preserve ISO dates in storage and portable files
- [x] Suggest icons for recognised new subject names, while preserving saved and manually selected icons, including None
- [x] Subject and cycle fields expose keyboard Done actions; colour/icon choices dismiss text keyboards, with retained focus for hardware-keyboard colour selection

## 0.5.2 — Widget 2.0 and dropdown fix

- [x] Rebuild the widgets as one adaptive Tempo widget (Glance 1.3 alpha) with distinct compositions at measured breakpoints: compact single event, square visual hero, wide hero plus next event and a non-scrolling mini-agenda
- [x] Shared widget presentation state resolved once from the dated schedule, with explicit current/next/break/free/done states, privacy-filtered locations and refresh boundaries
- [x] Mirrored Material shape rules for standalone, paired and three-part groups, with segment and outer outlines, subject-colour or theme-shade heroes, and shape tests that check the XML tokens
- [x] Hours-and-minutes countdowns fitted to the available space, readable start durations, truthful labels for overlapping events, and large-text labels that stay readable
- [x] Direct refresh on resize, clock/time-zone changes and leaving the app; optional exact-alarm timing, documented as non-wakeup otherwise
- [x] New widget configuration screen with a live preview built from the real renderer; saved per-widget timetable, theme, icon and location settings carry over, and legacy providers and widget IDs stay installed
- [x] Adopt Glance 1.3.0-alpha02, compile SDK 37 and AGP 9.2 for the widget work; isolated Remote Compose lab and QA APKs published from CI
- [x] Fix dropdowns that did not open in signed release builds; every choice field now opens a Tempo-styled bottom sheet, with subject colour and icon on subject choices
- [x] Publish the signed 0.5.2 APK and update metadata; add a manually triggered workflow for signed Android App Bundles
- [x] Record the Widget 2.0 evidence, plus the [Experience Integrity](EXPERIENCE_INTEGRITY.md) plan

## 0.6 — one home per action, alive when you tap

Agreed November 2026 after a device review of 0.5.2: remove duplicate controls, make prominent surfaces tappable, rebuild onboarding around what the timetable is for, and add opt-in progression. Full spec: [Redesign](REDESIGN.md).

- [ ] Foundations: motion tokens, haptics, press feedback and shared components in `core:designsystem`; one sheet state in place of scattered flags
- [ ] Remove duplicates: one timetable switcher (top-bar chip), import/export only in the switcher, per-card overflow menus, no top-bar undo/redo or calendar icon, single About row
- [x] Schedule: merge Today and Timetable into one day pager with a week strip, live Now card, now-line, dimmed past, compact free gaps, no chevrons, tappable header and Now card
- [x] Haptics on every interactive control through a shared component layer
- [x] Week overview tab
- [ ] Library subject detail (tap a subject to see where it occurs)
- [x] Onboarding rebuild: purpose question (school, college, work, personal) tailoring vocabulary and defaults, one question per screen, subject suggestions, live preview, progress opt-in, finish celebration
- [x] Opt-in check-ins, XP, soft streaks that skip non-scheduled days, day-complete moment and Progress drawer
- [ ] Achievements that encourage exploring the app, setup checklist and weekly recap (after the social features settle)

## Redesign — still to plan

- [ ] Widget redesign for streaks and progress: today's check-ins, streak and XP on larger widgets, and a check-in action where hosts allow it
- [ ] Full redesign of every Settings page (grouped rows with current values, one action per row)
- [ ] Replace short dropdowns with buttons or segmented choices, as with free gaps and break types
- [ ] Add session opens a quick subject picker first, then the full editor with that subject chosen

## Social Tempo — plan

Optional, end-to-end encrypted and free-tier only. Full plan, privacy rules and Cloudflare budget: [Social](SOCIAL.md).

- [x] Profile name on device
- [x] People (timetables marked as someone else's) and shared frees on Schedule and the Week tab
- [ ] Optional “Alex also has a free at 13:10” notification before a shared free
- [x] Encrypted share links at `tempo.xdan.cc/t/<id>#<key>` (Worker + D1), app links and a web viewer — service live; app support in the next release
- [ ] Live updates, and removal from other devices when the owner stops sharing
- [ ] Groups for tracking shared frees
- [ ] Sync between your own devices

## Next — interaction feedback and safe recovery

A tightly scoped usability pass, moved from the 0.5.2 candidate slot because 0.5.2 shipped Widget 2.0 and the dropdown fix instead. Detailed behaviour, copy, interaction rules and test matrix: [Experience Integrity](EXPERIENCE_INTEGRITY.md).

- [ ] Replace prominent toolbar-first Undo/Redo with contextual **bottom snackbars** after *committed* edits (e.g. “Added subject”): **Undo** plus close/dismiss, and a corresponding **Redo** action after undo
- [ ] Keep bounded undo history available after snackbar dismissal through a secondary accessible action; ensure rapid edits, bulk actions, timetable switches, keyboard users and TalkBack never undo the wrong thing
- [ ] Centralise typed success/error feedback; trigger it at completed action boundaries rather than per keystroke, cancelled file picks, recomposition or optimistic saves
- [ ] Improve errors for malformed/unsupported imports, interrupted/failed exports, failed saves and unavailable updates with an explanation, accurate data-safety status and relevant retry/review action
- [ ] Audit setup → create → edit → undo/redo → export/import → switch timetable end-to-end, including cancellation, navigation/back, offline, rotation and accessibility cases
- [ ] Add regression tests for feedback actions, data preservation on failure, repeated Undo/Redo and lifecycle interruption

## Widget 2.0 — shipped in 0.5.2; device validation and Remote Compose still open

**Ground-up widget redesign**, not a reskin of the three existing size-specific presentations. Follow Google's canonical widget layouts and progressive-disclosure resizing: a single primary Adaptive Tempo widget should feel more useful as users expand it, moving from one legible current/next event and countdown to visual progress, another event and eventually a **non-scrollable mini-agenda**. **Scrolling is deliberately deferred until after the first batch is implemented and tested.** Preserve existing placed widgets and preferences through compatibility adapters for the current provider components; consolidating picker entries must not break upgrades. Remote Compose remains a **feasibility-first experiment**, not a release requirement. Full architecture, design constraints, development phases, breakpoints, device matrix and go/no-go criteria: [Widget 2.0](WIDGET_2_0.md).

### Design and compatibility

- [x] Develop one default adaptive widget experience with **distinct canonical compositions at measured size breakpoints**, not three fixed widget types or scaled-up text layouts; align with Google's [Canonical Widget Layouts](https://developer.android.com/design/ui/mobile/guides/widgets/layouts) and [sizing guidance](https://developer.android.com/design/ui/mobile/guides/widgets/sizing)
- [x] Make resize feel like useful progressive disclosure: **2×1 single entity** → **2×2 visual entity** → **4×2 multiple entities** → **larger agenda**, with tall/short variants based on actual dp dimensions and font scale
- [ ] Prototype brief **end-of-day particle celebrations** (optional subtle Material-shaped confetti) while keeping completion readable and static on unsupported hosts, honouring reduced motion and avoiding repeated effects on refresh
- [x] Preserve existing Android widget IDs, installed providers and per-widget settings during the ground-up renderer replacement; assessing the safe retirement of duplicate picker entries remains open
- [x] Design expressive compact, square, wide and expanded canonical compositions around one large, clearly labelled countdown, subject and relevant next activity
- [x] Use official Material 3 shape/corner values and a **strict consistent-corner rule**: standalone corners match; connected pairs mirror their inner/outer radii; middle segments of three-part groups have consistent inner radii
- [ ] Use semantic icons, restrained coloured segments and an optional compact timeline to organise available space; take visual inspiration from [Material 3 UI Widgets on Figma](https://www.figma.com/community/file/1504472410356443638/material-3-ui-widgets) and Google's widget examples
- [ ] Fix density/readability on compact widgets and adaptive behaviour on 4-, 5- and 6+-column launchers, including resizing, font scaling, landscape and accessible layout alternatives
- [x] Preserve accurate current/next, break/free, done-today, week-done and empty states; keep the existing native countdown's efficient update behaviour unless an alternative is proven
- [ ] Design honest recovery states for missing/deleted selected timetables, widget setup not finished and known unavailable/stale data, with reconfigure/refresh affordances where supported; do not silently show another timetable (see [Experience Integrity](EXPERIENCE_INTEGRITY.md))
- [ ] Verify app/widget parity after edits, Undo/Redo, cycle/exception changes, clock/time-zone changes and delayed refresh; retain battery-friendly recovery
- [x] Preserve per-widget timetable selection, Material You/custom themes, subject colours, icons, location privacy, fills, outlines and configuration previews

### Remote Compose feasibility and delivery

- [x] Prototype **Remote Compose** with isolated experimental dependencies; the lab and Glance backend probe exist, but a public home-screen host pathway rendering shapes, text, taps, size changes and countdowns is **not yet demonstrated** (an in-app player alone is insufficient)
- [ ] Test Remote Compose's **native path/shape drawing** for the mirrored component geometry, sharing the same radius tokens with XML/vector fallbacks; rendering support does not eliminate the design rule
- [ ] Inspect alpha21 `RemoteEnterTransition.Particle`/`RemoteExitTransition.Particle` on actual widget hosts; confirm whether the available enter/exit effect can reproduce a tiny falling-confetti celebration before making implementation promises
- [x] Check Glance/Glance Adaptive release maturity, host capability, Android/launcher coverage, build-tool compatibility and performance before choosing a renderer
- [x] Extract a shared widget presentation state while replacing the old layout templates and size handling; production now renders through Glance (RemoteViews underneath, Android 8+); legacy layout templates are removed
- [x] Decision recorded in the [implementation log](WIDGET_2_0_IMPLEMENTATION.md): adopt Glance with the shared presentation, **defer Remote Compose**, per [Widget 2.0's acceptance gates](WIDGET_2_0.md); do not delay visual improvements if Remote Compose support is not ready
- [ ] Validate real launcher behaviour, TalkBack, long subject names, large text, light/dark contrast, clock/time-zone transitions, battery efficiency, recovery updates and release/QA CI before rollout
- [ ] Test replay-safe once-per-day completion semantics, reduced motion, and resize/launcher-host behaviours without requiring the widget to scroll

The Expressive widget redesign shipped in 0.5.2 without scrolling; adopting Remote Compose remains conditional on successful device-level tests, and launcher, TalkBack and battery validation is still to do. **The first batch does not require scrolling or snap gestures.** This work can proceed independently of Feel & Flow.

## Towards 1.0 — Feel & Flow

I want Tempo to feel like a polished, expressive utility; any progression is opt-in. The following is a **design direction**, not a commitment to pack every feature into 1.0: suitable parts may arrive in intermediate releases alongside the existing functional backlog. Details and acceptance criteria live in [Feel & Flow](FEEL_AND_FLOW.md).

### Schedule and week-at-a-glance

- [ ] Combine Today and Timetable into a single day-first **Schedule** destination: starts on today, swipes between actual dates, offers a date strip and return-to-today shortcut
- [ ] Build a genuinely different **Overview** destination: time-aligned week grid with subject-coloured blocks, overlap handling, week selection and tap-through to a day
- [ ] Summarise the resolved week: number of sessions for each subject, scheduled breaks, free periods/gaps and free time; distinguish the **whole week** from what is **still ahead**
- [ ] Reuse the dated resolver for cycle weeks, holidays, one-off replacements, days off and weekends; calculate free time inside configured day hours and avoid double counts
- [ ] Ensure readable tablet/foldable and accessibility-friendly alternatives to the dense week grid

### Feel and feedback

- [ ] Shared Material 3 Expressive motion and feedback conventions: spring navigation, selection and editing transitions, restrained haptics and predictable state changes
- [ ] Apply one documented cross-screen interaction contract for success/error/loading, navigation/back, confirmations and empty states; carry forward [Experience Integrity](EXPERIENCE_INTEGRITY.md) rather than creating new per-screen rules
- [ ] Audit connected user flows across Schedule, Overview, Library, editing, comparison and settings; preserve date/week/timetable context and verify recovery paths
- [ ] Living timetable states for past/current/next activities, clear elapsed/remaining progress and calm end-of-day/week transitions
- [x] In-app day-complete confetti inside the Done card (once per day per launch, skipped when animations are off)
- [ ] Small, optional celebrations and **permanent discovery achievements** with rounded top-of-screen notices; opt-in XP and soft streaks from check-ins only (see [Redesign](REDESIGN.md#gamification)); no leaderboards or gated essentials
- [ ] Dismissible contextual post-onboarding hints, replayable from Help, introducing gestures and relevant customisation when needed
- [ ] Audit reduced motion, animation scale, TalkBack, large text, contrast, gesture conflicts, performance and system-back behaviour
- [ ] Consider an optional factual weekly recap (schedule counts and hours, never inferred attendance)

### Sharing and identity (superseded by [Social](SOCIAL.md))

- [ ] Explicit snapshot sharing through validated `xdan.cc/tempo/share/<code>` links, with publication preview, independent imports and revocation; anonymous recipients need no account
- [ ] Design a secure, **optional creator profile** for attributed public timetables, kept separate from private on-device timetables; no account required to use Tempo
- [ ] Define safe publication defaults, sensitive-field redaction, abuse handling, retention, authentication and account deletion before launching profiles
- [ ] Consider profiles a later phase if backend and privacy work would otherwise delay the main 1.0 experience

## After Widget 2.0 — connected experiences (proposals)

These are **future experiments, not part of the first Widget 2.0 batch**. They must reuse Tempo's existing timetable resolver and preserve offline, private, account-free usage. Research scope, feasibility gates and API references live in [Connected Experiences](CONNECTED_EXPERIENCES.md).

### Wear OS companion — high priority after widgets

- [ ] Prototype a **Wear OS Tile** for the current/next subject, shift or appointment, with a prominent time, meaningful colour/icon and optional room/location; use documented Tiles/ProtoLayout components for compatibility.
- [ ] Resolve breaks, frees, exceptions, holiday days, alternating weeks and end-of-day states consistently with Tempo on the phone.
- [ ] Explore a small, versioned **phone-to-watch schedule snapshot** over Wear OS Data Layer, retaining useful offline behaviour and showing when displayed data is stale or disconnected.
- [ ] Independently investigate newer **Glance Wear Widgets and Remote Compose** on supported watch renderers; do not assume ordinary phone widgets, Tiles and watch-face complications are interchangeable.
- [ ] Test watch shapes/insets, glanceable typography, low-power behaviour, accessibility, sync freshness and pairing on real devices; consider a **watch-face complication** only after the Tile is reliable.

### Beam my Schedule — later research-stage feature

- [ ] Prototype a **temporary, read-only, nearby timetable preview** between two consenting Tempo devices: choose date/week and fields to share, confirm recipient, render and dismiss without automatically importing into the receiver's collection.
- [ ] Research authenticated Bluetooth/local-Wi-Fi transport (such as Nearby Connections), discovery/permissions, payload sizes, connection recovery and Android-device compatibility; do not assume every device has the same services.
- [ ] Test serialising a **sanitised Remote Compose display document** and rendering it in a compatible local player; validate document size, profiles, resource limits and safe local interactions. A binary preview must still be parsed and validated, and is not guaranteed tiny.
- [ ] Keep **notes/locations hidden by default**, block arbitrary transferred actions, and never merge/store another person's timetable without a separate explicit import.
- [ ] Compare the experience with existing `.tempo.json` sharing and simpler QR/link or compact DTO previews. Advance only if nearby previewing is faster and sufficiently private; structured compare-free-time data would require separate permission.

**Android Auto:** low-priority research only. Current car app category and safety rules should be verified before any car-specific UI work. Calendar/`.ics` interoperability with supported apps is a more realistic near-term option.

## Later releases

- [ ] **After Widget 2.0 ships and is tested:** revisit scrollable agendas for the largest layouts; consider ordinary Glance `LazyColumn` first and optional Android 17+ `SnapScrollMatchHeight` only if real launcher testing demonstrates a benefit. Neither was part of the first Widget 2.0 release; see [Widget 2.0 future investigation](WIDGET_2_0.md#future-investigation--scrollable-agendas-and-snap-scrolling-after-the-first-widget-20-release)
- [x] Restore bottom-sheet drag handles and swipe-down dismissal (needs boundary-fling and keyboard checks on device)
- [ ] Landscape timetable image exports with a time-grid/map layout and dotted hour or period guides, matching preview and privacy controls
- [ ] Tap a Library subject to quickly schedule it; retain an explicit pencil action for full subject editing
- [ ] Ask when subjects occur during onboarding, with day/time and cycle-week choices, editable summaries and an option to finish later
- [ ] Overnight shifts and explicit timetable time zones
- [ ] Calendar `.ics` import/export, recurring events and calendar-app integration
- [ ] Additional PNG day-card styles and fixed-height exports
- [ ] Free tags and selective break / dated-exception batch editing
- [ ] Side-by-side weekly comparison and additional widget views
- [ ] Tablet/foldable layouts, search and further accessibility improvements
- [ ] Search/filter sessions and subjects, including cycle-week filters
- [ ] Contextual session actions and optional advanced editor details, with clear labels
- [ ] Holiday range calendar and concise previews of affected dates before bulk changes
- [ ] Remember per-timetable export preferences with an explicit privacy reset
- [ ] Task-based usability checks for first setup, one-off changes and exports; TalkBack and hardware-keyboard walkthroughs
- [ ] Selective imports, bundled assets and restore points
- [ ] Reminders, travel buffers, subject-hour summaries and history
- [ ] Explore custom HEX colour selection for subjects and themes; maintain readable text, icons and outlines across timetables, overviews, exports and widgets
- [ ] Launcher icon colours following the app theme, alongside launcher-supported Material You themed icons

### Homework and assignments

I plan to make homework and assignments an optional part of Tempo, so school timetables can track work to hand in without adding it to every work or personal schedule.

- [ ] Add assignments linked to a timetable and optionally a subject, with a title, notes, due date and optional due time
- [ ] Show upcoming and overdue assignments using subject colours, with completion tracking and filters
- [ ] Offer optional due-date reminders
- [ ] Show approaching deadlines in an optional widget section or dedicated assignment view, including future dates after the week’s lessons are all done

Tempo resolves dated local-day schedules in the device's local time. Explicit zones, overnight shifts and calendar recurrence will build on this foundation.
