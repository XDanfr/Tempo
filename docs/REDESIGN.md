# Tempo redesign — one place for everything, alive when you tap

Status: **agreed direction (November 2026)**, implementation starting with foundations and feedback. This document turns [Feel & Flow](FEEL_AND_FLOW.md) and [Experience Integrity](EXPERIENCE_INTEGRITY.md) into concrete screen decisions. Where it differs from those documents, this one wins.

## Why

Reviewing 0.5.2 on a device showed three recurring problems:

1. **Duplicate controls.** The same action lives in several places, so people have to learn which one is "right".
2. **Dead surfaces.** Large, prominent cards and headers look tappable but do nothing, while small chevrons on every row compete for attention.
3. **Static feel.** No haptics, ad-hoc animation timings, a "now" card that only updates every 30 seconds and generic error dialogs.

## Rule 1 — every action has exactly one home

| Action | 0.5.2 entry points | Single home after redesign |
| --- | --- | --- |
| Switch / manage timetables | Top-bar calendar icon, timetable-name dropdown, Settings → Timetables & files | **Timetable-name chip in the top bar** opens the timetable switcher sheet. Calendar icon and the Settings “Timetables & files” page removed. |
| Import / export `.tempo.json` | Timetable sheet, Settings → Timetables & files, onboarding | **Timetable switcher sheet** (Import, and Export inside each timetable's overflow menu). Onboarding keeps "Import" only as a first-run starting path. Settings section removed. |
| Rename / duplicate / delete timetable | Three text buttons on every card | **Overflow menu (⋮) per card**; tapping the card switches to it. |
| Today vs Timetable | Two tabs rendering the same day list | **One Schedule tab** that opens on today and pages between dates. |
| Day navigation | Arrow row + date + "Today" button, weekday tab row, big day header | **One week strip** (swipe or tap a day) plus the header date, which opens a date picker. "Today" appears only when away from today. |
| Undo / redo | Top-bar icons | **Snackbar after each committed edit** (Undo → Redo), with history also in the Schedule overflow menu. |
| Compare timetables | Timetable sheet button; own screen with weekday chips *and* date arrows | **Timetable switcher sheet** entry; the Compare screen reuses the Schedule week strip instead of two day controls. |
| Add session | Extended FAB on two tabs; tap free block | **FAB on Schedule** (collapses to an icon while scrolling) and **tap a free gap**, which pre-fills its times. Both are kept on purpose: one is general, the other is contextual. |
| PNG image export | Settings → Timetables & files | **Schedule overflow menu → "Share as image"** (it's about what you're looking at). |
| About / version / sponsor | "Your Tempo · Part of Axis" header plus "Made by XDan · Part of Axis" footer | **One About row** at the bottom of Settings. |

## Rule 2 — if it looks tappable, it is; if it is tappable, it looks it

- **Now card** → opens the current or next session (or the free gap's "fill" sheet).
- **Header date** → date picker. **Week chip** ("Week A", "Week 2 of 3") → cycle settings; hidden entirely for 1-week cycles.
- **Session / break block** → editor; the whole block is the target, **no chevrons**. Long-press → quick actions (duplicate, split, delete), with an accessible alternative in the editor.
- **Free gap** → "Fill this gap" sheet with the exact times.
- **Library subject** → subject detail: colour, icon, location, weekly hours and every scheduled occurrence, each tappable to its day. A pencil edits the subject.
- **Week overview block** → that day in Schedule.
- **Milestone / streak chip** → Progress page.
- Pressed states: every tappable surface scales slightly (0.97) with a spring and a light haptic tick. Non-tappable cards never get elevation or a chevron.

## Navigation

Bottom bar: **Schedule · Week · Library · Settings** (four items, no duplication).

- **Schedule**: day pager starting today. Top bar: timetable chip (left), overflow (right: Undo/Redo history, Share as image, Jump to date).
- **Week**: time-aligned week grid with colour blocks, plus a summary of the whole week and what's still ahead (sessions per subject, scheduled hours, free time). Tap a block to open that day in Schedule.
- **Library**: subject grid tiles (colour fill, icon, weekly hours). FAB "New subject". No hero copy.
- **Settings**: grouped list — Appearance, Schedule (cycle, holidays, day hours, periods), Progress & celebrations, Widgets, Updates, About. Each row shows its current value ("Forest · Dark") so you rarely need to open it to check.

## Schedule screen

- **Header**: weekday + friendly date (tappable), week chip only when cycles exist, a slim day-progress bar when viewing today.
- **Now card** (today only): subject colour, icon, name, location, a live countdown ("32 min left") updating every second while visible, and a progress ring. States: current lesson, break, free, up next, done for today, nothing today. "Done for today" shows tomorrow's first session as the next action instead of a slogan.
- **Timeline**: a thin time rail with a moving **now-line**. Past blocks dim to about 60% opacity; the current block shows a progress fill. Block height is proportional but clamped, so a 20-minute break doesn't become a sliver.
- **Free gaps**: a compact row ("1h 30m free · tap to fill") instead of large empty dashed boxes; gaps over 3h collapse further.
- **Paging**: horizontal pager between real dates with the week strip in sync; the spring respects the animator duration scale.
- **Empty and holiday days**: illustrated state with one clear action ("Add a session" or "Edit holiday").

## Onboarding (rebuilt)

One question per screen, a real preview that fills in as you go, and a finish moment.

1. **Welcome**: animated preview of a timetable, "Get started" and "Import a timetable".
2. **What's this timetable for?** School · College/University · Work/Shifts · Personal. This choice tailors the interface (below) and sets smart defaults.
3. **Name and days**: name prefilled from the purpose ("School", "Work"); day chips preset (Mon–Fri for school, editable).
4. **Your hours**: start/end with presets; "Different hours on some days?" can be expanded.
5. **Periods and breaks** (school/college only; skipped for work and personal, available later in Settings).
6. **Subjects / activities**: one-tap suggestion chips per purpose (Maths, English, Science… or Meeting, Shift, Gym…) with auto colour and icon; custom add.
7. **Make it yours**: theme picker with a live preview of *their* week.
8. **Progress (optional)**: "Track lessons and get gentle streaks?" → Off (default), Celebrations only, or Celebrations + streaks and XP. Explains that attendance is only counted when they check in.
9. **Done**: short celebration, first milestone ("Your week, in view"), then the Schedule screen with one contextual hint.

Advanced options (repeating cycles, anchor dates) sit behind "My weeks alternate", not in the first screen.

### Purpose-tailored interface

| Purpose | Vocabulary | Defaults and emphasis |
| --- | --- | --- |
| School | Lessons, subjects, periods | Periods and breaks on, Mon–Fri, homework (when shipped) promoted |
| College / University | Sessions, modules, rooms | Locations shown, cycle question offered |
| Work / Shifts | Shifts, tasks, sites | Periods off, flexible hours, "hours scheduled this week" summary |
| Personal | Activities, places | Minimal setup, periods off, icons prominent |

Purpose is stored per timetable, changeable in Settings, and affects only labels, defaults and emphasis; it never hides features.

## Gamification

Progression and celebrations are **opt-in** (chosen in onboarding, changeable in Settings → Progress & celebrations). Earlier principles still hold: no leaderboards, no paywalled or gated essentials, no punishment, and **elapsed time is never treated as attendance**.

- **Setup checklist** (always available, dismissible): "Get set up 3/6" card on Schedule — add subjects, fill a week, pick a theme, add a widget, export a backup, try Week overview. Each step links straight to where it's done.
- **Discovery milestones** (permanent, local): first timetable, theme, widget, backup, comparison, holiday, first full week. Shown in a rounded notice at the top of the screen, with optional confetti; queued and never shown during editing.
- **Check-ins** (progression only): when a lesson ends, its block offers "✓ Attended" / "Skip". One tap, undoable, never a notification nag. Work timetables use "Done".
- **XP**: awarded for checked-in lessons and completed work items (later: assignments). Levels are cosmetic only: a level badge and celebratory accents, never feature unlocks.
- **Soft streaks**: counted in *scheduled* days you checked in to, so weekends, holidays and days off never break a streak. One missed day pauses it rather than resetting it ("streak paused — pick it up tomorrow"). No streak-loss notifications.
- **Day complete**: when the last block ends, the Now card turns into a calm completion state (matching the widget's "Done for today"), with a single subtle celebration once per day and a summary ("3 lessons · 4h 10m").
- **Weekly recap**: factual card on the first day of the week: scheduled hours, sessions per subject, check-ins if enabled.
- **Progress page** (from Settings or the streak chip): milestones, level, streak history. Not a bottom-bar tab.

All progress is stored locally and included in backups only if the user chooses.

## Foundations (`core:designsystem`)

- **Motion tokens**: `TempoMotion` springs (spatial fast/default/slow, effects) instead of scattered stiffness values; every animation reads them, so reduced-motion can turn them into snaps.
- **Haptics**: `rememberTempoHaptics()` with `tick` (selection), `press`, `toggle`, `confirm` (committed edit), `reject` (error) and `celebrate`; respects the system touch-feedback setting (an in-app toggle may follow). `ui/HapticComponents.kt` mirrors the interactive Material components with the same names, so every screen in the `ui` package gets haptics by default; `Modifier.hapticToggleable` covers whole-row toggles.
- **Press feedback**: `Modifier.pressScale()` with spring scale and haptic tick.
- **Components**: `TempoCard`, `SettingsRow` (icon, title, current value, chevron only for navigation), `EmptyState`, `TempoTopBar` with timetable chip, `FeedbackHost` (typed snackbars with Undo/Redo), `TopNotice` (milestones).
- **App state**: replace the ~15 sheet flags in `TempoApp` with one `Sheet` sealed type held in the ViewModel, so sheets survive rotation and can't stack by accident.

## Delivery order

1. **Foundations and feedback**: motion/haptics/press tokens, typed feedback with Undo → Redo snackbars, top-bar undo/redo and calendar icon removed, timetable sheet with overflow menus. *(done)*
2. **Schedule**: merged day pager, week strip, live Now card, time rail, compact gaps, no chevrons, tappable header. *(done; Week tab still to come, so the bottom bar is Schedule · Library · Settings for now)*
3. **Week overview** tab.
4. **Onboarding rebuild** with purpose, tailored vocabulary and the progress opt-in.
5. **Gamification**: checklist, milestones, check-ins, XP, soft streaks, day complete, recap, Progress page.
6. **Library and Settings**: subject tiles and detail, grouped settings with values, single About row, duplicate file controls removed.

Each step ships separately and keeps the existing data formats; new fields (purpose, progress) are additive and optional in the timetable schema.
