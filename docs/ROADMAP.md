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
- [x] Readable subject text and comparison fills, contrasting outlines/icons, named colour controls and explicit selection state
- [x] Friendly display dates such as “11th October 2026”, including ordinal edge cases and locale-aware alternatives; preserve ISO dates in storage and portable files
- [x] Suggest icons for recognised new subject names, while preserving saved and manually selected icons, including None
- [x] Subject and cycle fields expose keyboard Done actions; colour/icon choices dismiss text keyboards, with retained focus for hardware-keyboard colour selection

## Later releases

- [ ] Restore bottom-sheet drag handles and swipe-down dismissal without coupling content flings to changing sheet anchors; include boundary-fling and keyboard regression checks
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
- [ ] Share links at `xdan.cc/tempo/share/<code>` with marked, validated codes and a storage service
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
