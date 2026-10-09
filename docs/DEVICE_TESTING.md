# Feedback milestone device checks

Build checks cover schedule resolution, schema migration/round trips, lint and APK assembly. The following interaction checks still need a real Android device or emulator; CI success does not establish gesture or visual correctness.

- Fresh onboarding: set day hours, edit/remove/add usual periods, add lunch and changeover on selected days, add subjects, select Material You and outlines, finish.
- Existing v1 install with the same signing key: verify subjects, rooms, lessons, day hours and wallpaper choice survive migration. Interrupted subjects/appearance setup should resume at its matching new step.
- Tap a 30-minute free: add a session and verify its initial times match that gap; repeat with a break. No remaining free should overlap the new block.
- Edit a lunch from the timeline, change its days, and verify the repeat-day controls and overlap warning.
- Search the icon picker for maths, art, Spanish and gym; filter categories, select an icon, cancel another selection, and reopen.
- Choose Filled/Outlines in light and dark themes; verify subject labels remain legible. Increase system font size and check short-session labels.
- Gesture Back from Settings/Library/Timetable: drag part way, cancel, then complete. Cancellation must keep the current screen; completion returns to Today. Back on Today should use system exit animation.
- Gesture Back inside session/subject/period/break/icon editor sheets: cancel then complete. Dismissal must not save edits.
- Back during onboarding should return to the previous step. An open editor should dismiss before the step changes.
- Set animation duration scale to zero and check normal day/tab/sheet transitions settle immediately.

Development APK signing is currently ephemeral in CI. Signature mismatches require uninstalling an older development APK, which removes local data; configure persistent signing before distributing updates.

## Timetable files milestone

- Open the icon picker above a subject editor. Scroll to the bottom, fling upwards,
  repeat, search/filter to shorter results, dismiss the keyboard and rotate. The
  sheet must remain fixed, the grid must scroll, and Save/Cancel must stay reachable.
- In Forest, Ocean and Amber, test both light/dark: switches, chips, dropdowns,
  navigation selection, dialogs, FABs and sheet buttons must use preset colours.
  Material You should continue using wallpaper colours.
- Update an existing signed v0.2 install without clearing data. Verify subjects,
  sessions, breaks and appearance survive collection migration, then restart.
- Create two timetables with different subjects, hours and themes. Switch, rename,
  duplicate, edit the duplicate and delete the active timetable. Verify its fallback
  and that the last timetable cannot be deleted. Resume an unfinished new setup.
- Export a timetable, restart, then import it as a new timetable. Verify all names,
  colours, icons, times, locations, notes, repeated break days, periods and appearance.
  Export while rotating with the system picker open and verify the chosen snapshot.
- Import during fresh onboarding; use it for setup. Import over a populated timetable:
  cancel replacement, then confirm. Invalid JSON, future versions, broken subject
  references and files above 2 MiB must show errors without changing saved data.
- Cancel both document pickers. Check file-provider permission/write errors and
  confirm Tempo continues to work. Long filenames should remain readable.
- Choose outlined breaks/filled subjects, then the reverse; restart and export/import
  to confirm both choices persist. Old global outlines must initially apply to both.
- During lunch, verify Today shows the next subject, start time and location. After
  the final lesson, a break should say no more sessions today. Check boundary minutes.
