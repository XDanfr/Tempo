# Changelog

## Unreleased

- **Redesigned Settings**: a clean list in two groups, App (Appearance, Progress, Updates) and the open timetable (what it's for, dates and weeks, day hours, periods), with each opening its own page.
- Appearance always changes your own theme, even while you're viewing someone else's timetable.
- **Timetables added from a link are view-only**: they follow their owner's changes, so there's no Add button and sessions, breaks and subjects can't be edited. Timetables imported from a file, or as your own, stay editable.
- Tap a free you share and the drawer lists who's free with you, and when if it's only part of the gap.
- **In the same lesson**: when a friend has the same subject in the same room at the same time, the lesson shows them too. Lessons without a room never match.
- Checking in now shows your XP bar filling up instead of a floating +XP. Achievement notices fade into the XP they added, and how much you need for the next level.
- Achievement details open in a drawer, like the rest of the app.
- **Achievements give XP**: each one adds 25 XP, including ones you'd already earned. Tap any achievement on your profile to see its details, when you unlocked it, or how close you are.
- **Level up**: reaching a new level shows a bigger notice with a spinning level badge and more confetti. Swipe any notice up to dismiss it.
- **Quicker Add session**: pick a subject and it's added in the next free period or slot straight away; tap it to change the time or room. **Set time and details instead** opens the full editor.
- The streak bonus shows as a ×1.3 tag next to your XP and on the Home streak tile, and the progress sheet's How it works is now a short list with icons.
- Settings now sits above achievements on your profile, and the top-right avatar becomes a Settings button there.
- Fixed tapping a shared free sometimes opening the wrong day in Schedule.
- **Streak bonus**: each day of your streak adds 10% to check-in XP, up to double at 10 days. Missing a scheduled day resets the bonus. If you're ill or away, **Freeze today** in the progress sheet protects your streak and bonus.
- **Fill in your week**: setup now asks what's on in each period (or lets you add sessions day by day). If you skip it, Home shows a **Build your timetable** card until you've added some.
- Celebrations, streaks and XP are now app-wide settings, with a new **Achievement notices** switch. They're no longer saved into a timetable or exported with it, and viewing someone else's timetable keeps your own theme and progress.
- **Share links**: share any of your timetables from its ⋮ menu. Tempo encrypts it on your phone and creates a link at tempo.xdan.cc. The key is part of the link and never reaches the server. Rooms and notes are left out unless you include them, and check-ins are never shared. Copy or share the link, update the shared copy, or stop sharing at any time. Opening a link shows a preview: **Add [name]** to compare frees, or **Import as my own**. You can also paste a link with **Add from link** in the timetable switcher. Without Tempo, the link opens a private read-only web view.
- **Home**: Tempo now opens on Home. It shows a greeting, the live Now card, what's later today, who's free with you (today, or the next day with a shared free), your progress, and your latest and next achievements. Navigation is Home · Schedule · Week · Library. Your **profile** (the avatar, top right) shows your name, level, streak and every achievement, and opens Settings.
- Shared frees are named by period when they line up exactly, such as “Period 2” or “Periods 2–3”, instead of a time range, on Home and the Week tab.
- **Achievements**: 16 permanent achievements for finding your way around Tempo, such as setting up alternating weeks, adding a widget, sharing a link, a free that lines up with a friend's, and a 5-day streak. Each unlocks with a rounded notice from the top of the screen, in your theme colours, with a colourful fall of Material-shape confetti behind the text, unless celebrations are off. They never unlock or gate anything, and achievements you'd already earned are recorded quietly.
- **Shared timetables stay up to date**: after you edit a timetable you've shared, its shared copy updates itself about 30 seconds later, and only when something shared actually changed. People who added you get the changes when they open Tempo and every few hours in the background. If you stop sharing, or delete the timetable, their copy is removed and they're told why.
- The share page at tempo.xdan.cc matches xdan.cc/tempo: Outfit, the Tempo icon, and **Open in Tempo** and **Install Tempo** buttons.
- More expressive motion: the Now card's countdown ring is wavy, loading states use Material's shape-morphing loading indicator, the XP and level bars are wavy, and empty days show a slowly turning cookie shape.
- The Done for today card now just says that and shows what's next. Today's XP appears inside the streak chip.

## 0.6.0 — Schedule, Week, setup and frees with friends

- **New setup**: a welcome screen previews Tempo, then asks one question per screen. **What's this timetable for?** (School, College or university, Work or shifts, Personal) sets sensible days, hours and wording. Next come your name and days, then your hours, with alternating weeks behind a single switch. School and college timetables then set up periods. One-tap subject suggestions pick colours and icons automatically. A theme step previews your own week, an optional progress step asks about celebrations, streaks and XP, and a “You're all set” summary finishes it.
- Wording follows the timetable's purpose: “Add lesson” for school, “Add shift” for work, and so on. Purpose and celebrations can be changed in Settings. Turning celebrations off also turns off day-complete confetti.
- **Schedule** replaces the separate Today and Timetable tabs. It opens on today; swipe left or right to move between days, or tap a day in the week strip, which you can also swipe to change week. Tap the date heading to jump to any date. A **Today** button appears only when you're on another day. 
- **Week** tab: move between weeks with the arrows, and jump back with **This week**. Switch between the **Whole week** and what's **Still ahead**, see tiles for sessions, scheduled time, free time and breaks, and a by-subject breakdown with colour bars. A time-aligned grid shows every block, with overlaps side by side, past blocks faded and a now-line on today. Tap any block or day to open it in Schedule. Weekend columns appear only when something happens on them. Navigation is now Schedule · Week · Library · Settings.
- The week strip shows today's date ringed, the selected day filled, and up to three subject-colour dots for each day.
- A live **Now** card for today. It shows a ring that counts down the current session, break or free time to the second, then what's next, in the subject's colour. Before your first session it counts down to the start. When you're done it shows "Done for today" with a summary and your next session on a later day. Tap the card to open what it's about, or jump to that next day.
- A timeline with start times on a rail and a moving now-line. Earlier blocks fade, and the current block fills as time passes and is marked **Now**. Blocks show their duration ("1h 30m"), stay readable when short and no longer have chevrons; the whole block is the tap target. Free time is a compact "1h 30m free · Fill" row instead of a large empty box.
- The Now card never says the same thing twice: its small label names the moment (Now, Up next, Next session) and the title names the thing. After your last session it shows “Done for today” with when you finished and what's next, even if your day hours run later. On a day with no sessions it points to your next session instead.
- **People and shared frees** (on this device only): set **Your name** at the top of Settings. Mark any saved timetable as someone else's from its ⋮ menu (“Someone else's…”). Schedule then shows who's free with you on each free gap (“Alex too”), and the Now card says “Alex is free too until 14:40” while you're free. The Week tab adds **Frees you share**, with a toggle per person and a list of each shared free that opens its day, and highlights shared frees on the grid. Nothing leaves your device.
- With streaks on, the Week tab shows XP earned this week and how much is left to the next level.
- Fixed the Today button squashing to one letter per line when the streak chip is showing.
- **Check-ins, XP and streaks** (with Celebrations, streaks and XP on): lessons that have started, today or in the last two days, get a check button. Tap it to mark the lesson as attended and a “+10 XP” floats up; the snackbar offers Undo. Checking in to every lesson in a day adds 5 bonus XP. A streak chip in the Schedule header opens **Your progress**: your level (cosmetic only), XP, current and best streak, today's XP and how it all works. Streaks only count days that have lessons, so weekends and holidays never break them, and one missed day pauses the streak instead of resetting it. Only check-ins count, never elapsed time. The Done card shows the day's XP and streak.
- **Period setup**: school and college timetables are asked how many periods they have, then which breaks to use (Break, Lunch, Changeover) and how long each is, from quick choices or a custom number of minutes. Tempo suggests matching period times, for example four periods from 09:00 to 16:20 become Period 1 09:00–10:30, Break, Period 2 10:50–12:20, Lunch, Period 3 13:10–14:40, Changeover, Period 4 14:50–16:20. Tap any period to rename it, change its times or add a room and colour. Choosing a period when adding a lesson fills in its room.
- **Day-complete confetti**: when you open Tempo after your last session, or when it ends while Tempo is open, a short burst of Material Expressive shapes in your subject and theme colours springs from the check badge inside the Done card, with a celebratory buzz. It plays once per day per app launch and never when system animations are off.
- "Week 1 of 1" is gone: cycle weeks only appear for timetables that alternate. Holidays and changed days appear as labels.
- The Add session button shrinks to an icon while you scroll.
- The Schedule date header slides away as you scroll down through the day and springs back as soon as you scroll up.
- Editors are draggable drawers: drag down to close. Save sits top right; back, dragging down or tapping outside closes. If you’ve changed something, closing asks whether to save or discard.
- Delete buttons are red with a trash icon and act straight away. The snackbar offers Undo, so there's no extra confirmation. Deleting a whole timetable still asks first.
- Side-by-side buttons, such as New/Import and the System/Light/Dark choice, squish their neighbours with a bounce when pressed.
- Flinging a list inside a drawer no longer drags or closes the drawer when the list reaches its end. Dragging down from the top still closes it.
- Updated to the stable Material 3 1.5.0, with Expressive, and Compose BOM 2026.11.01. The project now compiles against Android SDK 37.1.
- Free gaps no longer show a “Fill” label; the whole gap is the tap target. Tapping one offers two buttons, **Session** and **Break time**, instead of a dropdown and Continue.
- **Haptics everywhere**: buttons, cards, menu items and dropdown fields give a light press; switches, checkboxes and toggle rows click on and off; chips, segmented buttons, tabs, day changes and swipes tick; saved, undone and redone edits confirm; errors buzz. Android's touch-feedback setting still applies.

- Every saved edit now confirms what changed in a snackbar, such as “Added Physics session” or “Deleted Lunch”, with **Undo**. After undoing, the snackbar offers **Redo**. An Undo tapped late never reverses a newer change, and Undo and Redo stay in the ⋮ menu after the snackbar closes.
- The top bar shows your timetable as a single chip that opens the timetable switcher. The calendar icon and the separate Undo/Redo icons are gone.
- In the timetable switcher, tap a timetable to switch to it. Rename, Duplicate, Export and Delete are in each timetable's ⋮ menu. New and Import sit side by side, and Compare appears once you have two timetables.
- Day and week images are now shared from the ⋮ menu (“Share as image”). The duplicate “Timetables & files” settings page has been removed.
- Timetables gain optional purpose, progress and setup-step fields, so existing data and files load unchanged. Exported files that use them need this version or newer.
- **New updater**: Tempo checks for new releases automatically once a day (on by default; turn it off in Settings › Updates) and posts a notification when one is available. In the app, a popup shows what's new with **Update** or **Later**. The download runs with a wavy progress bar you can hide or cancel. The update is checked against its published checksum and Tempo's signing key, then Tempo asks you to install it. Updates only download when you tap Update.
- **What's new**: people updating from 0.5.2 see a one-time sheet showing where things moved. New timetables now follow the system light or dark theme by default.
- Shared motion and haptic foundations: confirmations give a light haptic tick, and the timetable chip responds to presses.

### Compatibility

Version code 11. Timetable schema remains 3; new fields (purpose, celebrations, check-ins, setup step, person, period room and colour, profile name) are optional, so existing timetables and files load unchanged. Files exported from 0.6 that use the new fields need 0.6 or newer to import. Existing timetables keep their saved theme mode. Compiled against Android SDK 37.1; still runs on Android 8.0 and newer.

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
