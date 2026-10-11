# Tempo 0.5 UX decisions

The aim is to reduce decisions while someone is viewing a timetable, and make
the consequences of an edit clear where the edit happens. Keep Tempo's existing
Outfit type, Material components, theme cards and subject colours.

## Implemented

- Calendar and clock pickers replace format-dependent typing. The clock picker
  retains an explicit 24:00 end-of-day action, so this improvement does not remove
  valid schedules. Split starts at the midpoint and can be adjusted.
- 1/2/4-week suggestions cover common patterns; a numeric field preserves custom
  1–52-week cycles. Suggestions never silently change a saved schedule.
- Settings groups tasks into Timetables & files, Appearance, Schedule and Updates,
  with About at the bottom of the home page. Icons and chevrons mark navigation.
  A circular Back action returns to the list; predictive Back reveals it during
  the gesture and restores the current page if cancelled.
- Today and Timetable show the resolved schedule without a persistent editing
  toggle. The session/break editor's labelled Applies to choice states either
  Repeating schedule or Only the actual date. Existing date replacements lock
  the scope rather than implying they can edit recurring data.
- Undo/redo appear only when available, reducing disabled toolbar controls.
- Editor flings stay inside a fixed panel. Close, Cancel, Back and outside taps
  remain available; Save stays outside the scrolling body.
- Short day-hours and usual-period editors wrap their content in centered dialogs;
  long editors keep the fixed panel that passed the boundary-fling regression.
  Time fields stack at narrow widths so clock values remain on one line.
- Widget timetable choices and icon/location options use scrollable pills. Custom
  themes reuse the app theme cards. Location following tracks the real timetable
  display preference, while old widget overrides remain readable.
- Setup asks for an anchored cycle without adding a saved onboarding step. Subject
  cards expose colour, icon and default location; tapping edits the same subject.
  Continue stays above the keyboard, and proceeding clears field focus.
- PNG appearance uses the same recognisable theme cards and mode chips as app
  appearance. It is independent of app appearance, starts in System mode and
  includes the selected theme in the exact preview/save snapshot.
- Session, subject and break summaries use singular forms for one item.

These choices apply recognition instead of format recall, useful defaults with
an escape hatch, grouping related tasks, and moving secondary decisions into
their task context. See NN/g's [recognition and recall](https://www.nngroup.com/articles/recognition-and-recall/)
and [progressive disclosure](https://www.nngroup.com/articles/progressive-disclosure/).
[uxpeak's public Playbook overview](https://www.uxpeak.com/pages/playbook) is a
reference for the requested broader UI/UX direction; these are Tempo-specific
decisions, not claims of reproducing a paid playbook.

## Next UX work

The roadmap records search/filtering, contextual session actions, optional
advanced details, holiday-range selection, bulk-change impact previews, and
privacy-aware export preferences. Test these with first setup, a one-off date
change and an export task before adding further navigation or hidden controls.
Include TalkBack, keyboard and narrow/foldable layouts in those checks.
## Settings gesture refinement

Completing predictive Back keeps the outgoing settings page visible while it slides off the gesture edge, then returns to the settings home. Cancelling the gesture restores the same page. Subject and break fill/outline choices use pills so both options remain visible without opening a menu.

Validated with the debug build, app unit tests and debug lint, plus six setup refinement tests on the Android 16 phone emulator. The release regression checks that navigation has not occurred 80 ms after release, then completes after the exit animation. Gesture cancellation is covered separately. This checks dispatched predictive-back events; a physical edge gesture was not rechecked in this pass.

