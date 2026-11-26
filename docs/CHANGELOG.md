# Changelog

## Unreleased — redesign foundations and Schedule

- **Schedule** replaces the separate Today and Timetable tabs. It opens on today; swipe left or right to move between days, or tap a day in the week strip, which you can also swipe to change week. Tap the date heading to jump to any date. A **Today** button appears only when you're on another day. Navigation is now Schedule · Library · Settings.
- The week strip shows today's date ringed, the selected day filled, and up to three subject-colour dots for each day.
- A live **Now** card for today. It shows a ring that counts down the current session, break or free time to the second, then what's next, in the subject's colour. Before your first session it counts down to the start. When you're done it shows "Done for today" with a summary and your next session on a later day. Tap the card to open what it's about, or jump to that next day.
- A timeline with start times on a rail and a moving now-line. Earlier blocks fade, and the current block fills as time passes and is marked **Now**. Blocks show their duration ("1h 30m"), stay readable when short and no longer have chevrons; the whole block is the tap target. Free time is a compact "1h 30m free · Fill" row instead of a large empty box.
- "Week 1 of 1" is gone: cycle weeks only appear for timetables that alternate. Holidays and changed days appear as labels.
- The Add session button shrinks to an icon while you scroll.
- **Haptics everywhere**: buttons, cards, menu items and dropdown fields give a light press; switches, checkboxes and toggle rows click on and off; chips, segmented buttons, tabs, day changes and swipes tick; saved, undone and redone edits confirm; errors buzz. Android's touch-feedback setting still applies.

- Every saved edit now confirms what changed in a snackbar, such as “Added Physics session” or “Deleted Lunch”, with **Undo**. After undoing, the snackbar offers **Redo**. An Undo tapped late never reverses a newer change, and Undo and Redo stay in the ⋮ menu after the snackbar closes.
- The top bar shows your timetable as a single chip that opens the timetable switcher. The calendar icon and the separate Undo/Redo icons are gone.
- In the timetable switcher, tap a timetable to switch to it. Rename, Duplicate, Export and Delete are in each timetable's ⋮ menu. New and Import sit side by side, and Compare appears once you have two timetables.
- Day and week images are now shared from the ⋮ menu (“Share as image”). The duplicate “Timetables & files” settings page has been removed.
- Shared motion and haptic foundations: confirmations give a light haptic tick, and the timetable chip responds to presses.

## 0.5.2

- Widget 2.0 styles: a new adaptive Tempo widget plus redesigned 2×1, square and wide widgets with connected event and countdown shapes, optional segment and outer outlines, subject-colour or theme-shade heroes, and a refreshed configuration screen with a live preview.
- Widgets show hours and minutes during lessons and breaks, fit countdowns to their space, show readable start durations, label overlapping events truthfully, keep large-text labels readable, and refresh straight after resizing, clock changes or leaving the app. Exact-alarm timing is offered where Android allows it.
- Dropdowns now open in signed release builds. Every choice field opens a bottom sheet styled like the rest of Tempo, and subject choices show each subject's colour and icon.

### Compatibility

Version code 10. Timetable schema remains 3. Existing widgets keep their configuration.

## 0.5.1

- Twelve named subject colours, adding Red, Orange, Lemon, Teal, Blue and Magenta to the original six. Colour choices wrap and expose their names and selection state as a radio group to accessibility services. Choosing a colour, opening the icon picker or using the subject fields' Done key dismisses the text keyboard to keep the controls visible. Hardware-keyboard colour selection keeps focus on the chosen control.
- Red, Violet and Rose themes, in light, dark and system modes. Red now uses a clear saturated red, and dark surfaces carry more of each theme's colour. Theme colour roles animate when switching presets or modes. Theme cards preview the selected mode's actual palette, including wallpaper colours on Android 12 and newer. The same presets are available for widgets and PNG exports.
- Theme options use a two-column grid, with Forest and Material You together on the first row. Widget Custom theme opens a popup with the same grid and mode controls; choices apply immediately and stay applied when the popup closes. Leaving widget setup saves the current settings automatically, including when returning to the home screen.
- Subject card text chooses a readable foreground. Outlines and icons adapt to their background, and comparison cards use readable text on their blended fill. Imported translucent subject colours use the visible blended background when checking text contrast, including widget lesson panels. Improved contrast for text on dimmed fixed theme accents.
- Friendly dates throughout date controls, timetable headings, schedule settings, editing scopes and PNG headings. English dates use ordinals; other languages follow the device locale's long-date convention.
- Recognised names suggest icons for new subjects. Saved icons and manual choices, including None, survive name changes.
- Cycle-length input exposes a Done action that dismisses its numeric keyboard before continuing or saving.

### Compatibility

Version code 8. Timetable schema remains 3, with existing schema-1/2 migration unchanged. Existing subject ARGB values, IDs and saved theme names remain unchanged. New theme names require 0.5.1 or newer when importing an exported timetable into another device; older Tempo versions cannot understand them. Material You still requires Android 12 or newer and retains the existing Forest fallback on older devices.

Custom HEX entry, sheet gestures, onboarding scheduling and landscape image exports remain planned for later releases.
