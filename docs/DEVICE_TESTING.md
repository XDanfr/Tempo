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
