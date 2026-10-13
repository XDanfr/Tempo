# Changelog

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
