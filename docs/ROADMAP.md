# Tempo roadmap

## Milestone 1: editable weekly timetable
- [x] Android project and CI
- [x] Subject, session, day window and usual-period models
- [x] Shared gap and overlap resolver with tests
- [x] Versioned atomic local persistence
- [x] Resumable onboarding with period/break editing; demo shortcut removed
- [x] Readable day view with proportional sessions and subtle dashed frees
- [x] Subject editing, optional icons and session editing
- [x] Independent day hours and usual period templates
- [x] Forest, Ocean, Amber and Material You themes

## Following milestones
- [x] Multiple timetables: switch, create, rename, duplicate and delete
- [ ] Compare selected timetables in aligned or overlaid views, showing shared free periods alongside lessons and activities
- [ ] Room for occurrence queries and history
- [ ] Multiple-week timetables: configurable A/B or longer rotating week cycles, with a start date and week selector
- [ ] Dated exceptions, holidays, overnight shifts and explicit zones
- [x] Break/lunch/changeover blocks and conversion of a free slot into a session or break
- [ ] Free tags, arbitrary splitting, batch edits, duplication, undo/redo
- [ ] Tablet/foldable week layouts, search, accessibility and reduced motion verification
- [ ] Glance: now/next, today, fixed day, week and free-time widgets
- [ ] Per-widget themes, privacy, empty messages and battery-friendly update policy
- [x] Versioned `.tempo.json` files: complete snapshots, validation, import preview and onboarding entry
- [ ] Selective import, bundled assets and restore points
- [ ] Share links at `xdan.cc/tempo/share/<code>` with marked, validated codes and a storage service
- [ ] Calendar `.ics` integration: import/export, recurring events, time zones and calendar-app handoff
- [ ] PNG day/week/day-card renderer with preview and configurable dimensions
- [ ] Reminders, travel buffers, subject-hours summaries and version history
- [x] Outfit font bundling with its licence; rounded adaptive and monochrome icon
- [ ] Launcher icon colours follow the selected app theme, with launcher-supported themed icons for Material You
- [x] Signed release workflow, R8 and resource shrinking
- [ ] Device verification of release builds

The demo is fictional; it is not a reconstruction of Dan's timetable. Axis is the ecosystem, Tempo is the app. Schedule items are sessions, never cards.
