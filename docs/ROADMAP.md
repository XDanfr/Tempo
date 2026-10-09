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
- [ ] Validate widget sizing/gestures and signed update installation on devices before release

## Later releases

- [ ] Multiple-week timetables: A/B or longer cycles, anchored to a date
- [ ] Holidays, dated exceptions, overnight shifts and explicit timetable time zones
- [ ] Calendar `.ics` import/export, recurring events and calendar-app integration
- [ ] PNG day/week/day-card exports with preview and configurable dimensions
- [ ] Free tags, splitting, batch editing, duplication and undo/redo
- [ ] Side-by-side weekly comparison and additional widget views
- [ ] Tablet/foldable layouts, search and further accessibility improvements
- [ ] Selective imports, bundled assets and restore points
- [ ] Share links at `xdan.cc/tempo/share/<code>` with marked, validated codes and a storage service
- [ ] Reminders, travel buffers, subject-hour summaries and history
- [ ] Launcher icon colours following the app theme, alongside launcher-supported Material You themed icons

Tempo currently uses weekly templates in the device’s local time. Multiple-week cycles and dated exceptions will use a shared dated scheduling model before being exposed across the app, widgets and exports.
