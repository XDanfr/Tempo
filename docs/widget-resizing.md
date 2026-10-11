# Widget resizing

All three widgets resize horizontally and vertically. The pill has a 140 × 56 dp resize minimum; square has 110 × 110 dp; the 4×2 has 140 × 110 dp. Cell spans are initial placement hints, not layout measurements.

Android 12+ receives exact layouts for up to 16 distinct, positive launcher-reported sizes. If sizes are absent, and on older Android versions, minimum/maximum option ranges supply portrait and landscape rectangles. Options changes enqueue a refresh with the latest launcher options.

Layout decisions use both dimensions and account for enlarged system fonts. The 4×2 uses a side-by-side agenda at 280 dp effective width and 150 dp effective height, a stacked agenda below that width at 300 dp height, and a single lesson panel otherwise. Later periods are bounded to one or two rows with a remaining-period count. At very small effective heights, only the title remains. Completion screens hide badges and the next-lesson panel before shrinking text further. Text and countdowns have bounded lines and end truncation.

## Outlines and compatibility

The 4×2 has separate Subject outlines and Outer outline controls. The existing `outline` preference retains its subject-outline meaning; `outerOutline` is stored separately and cleaned up on deletion. Existing saved widgets initially inherit their old outline choice for the new outer setting. New widgets start with the outer outline off. Saving makes the settings independent. The outer outline is available during lessons, completion and empty states, using the existing theme outline colour and surface shape. Existing timetable, theme, icon, location and lesson-colour settings remain intact, including the legacy `outlined` preference.

## Validation and launcher checks

Sizing unit tests cover tall/narrow, short/wide, enlarged fonts and row-density thresholds. Build and unit-test checks do not verify launcher rendering.

Before release, check both Android 26–30 and Android 31+ launchers:

- Add all three widgets on four-, five- and six-column grids; drag each edge through its permitted sizes.
- Rotate the launcher and confirm it selects the appropriate orientation layout.
- Try 110 × 110, 140 × 56, 160 × 360, 280 × 150 and 400 × 110 dp, including enlarged system text.
- Check active lessons, upcoming lessons, breaks, day completion, week completion, an empty timetable and a removed timetable; include long names and locations.
- Verify stacked and side-by-side agendas show the remaining-period count when rows are omitted.
- Reconfigure the 4×2 through all four outer/subject-outline combinations in light, dark and Material You themes.
- Update an existing installation and verify saved preferences; reopen configuration and save, then remove a widget and confirm its preferences are cleaned up.
- Confirm widget taps still open the selected timetable and live countdowns update.

The supplied screenshots informed the narrow completion and pill changes. No phone/emulator launcher was available for visual verification during implementation.
