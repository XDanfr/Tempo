# Experience Integrity — feedback, consistency and recovery

Status: **proposed work**, not implemented. This is a cross-cutting quality initiative, not a new primary app destination. It complements the [roadmap](ROADMAP.md), [Feel & Flow](FEEL_AND_FLOW.md) and [Widget 2.0](WIDGET_2_0.md). Prioritise user trust over new animation or additional features.

Inspired by [Tim Gabe — The Psychology of Premium Apps](https://youtu.be/8EAYZe6y3I8) (user-provided full transcript, October 2026): make outcomes clear, keep behaviour coherent across screens, and design the failure path as intentionally as the success path.

## Existing foundations — preserve these

- Saved timetable edits already support session-local undo/redo via bounded collection history. Do not rebuild the model purely for visual changes.
- File imports have validation and explicit preview/replace choices; file export/import runs off the UI thread and shows progress.
- The app already has snackbars, an error dialog and atomic local storage.
- The app, widget and export share a dated schedule resolver. Maintain one source of schedule truth.
- **Do not mark tasks below as complete** merely because an adjacent primitive exists. The deliverable is end-to-end usable behaviour.

## Release slice A — 0.5.2 candidate: actionable feedback and recoverable edits

Goal: after a user commits an action, they know **what changed**, **how to undo it** and **what will happen next**. The feature should remain useful without motion.

### A1. Replace toolbar-first undo/redo with contextual snackbars

- [ ] Define a typed UI-feedback contract (success / undoable change / redoable change / info / error) and a single app-level host; remove reliance on generic free-form strings as the sole state model. Do not emit feedback directly from rendering/recomposition.
- [ ] Show a bottom Material 3 snackbar **after a successfully committed user action**: e.g. “Added subject”, “Updated session”, “Deleted break”, “Renamed timetable”. Provide **Undo** and a **close button**; optionally support safe swipe-to-dismiss after testing Android back/gesture and accessibility conflicts. Use the established Outfit typography, palette and shape rules.
- [ ] When Undo succeeds, show “Undid adding subject” (or equivalent) with **Redo** and dismiss; when Redo succeeds, show the original confirmation with Undo. Keep exact singular action wording, and avoid inaccurate generic “Saved” messages.
- [ ] Hook these to meaningful edit boundaries (save/confirm/duplicate/delete/bulk edit), **not individual text-field keystrokes**, composition updates, date paging, cancelled operations or unchanged values.
- [ ] Keep the existing bounded, session-local history behind the new UI. One visible snackbar at a time; rapid successive edits must never undo the wrong action. Explicitly define queue/replacement behaviour and bind the offered action to the correct state/revision and timetable identity.
- [ ] Snackbar timeout or dismissal **must not erase history**. Put Undo/Redo in a secondary accessible overflow action or equivalent discoverable fallback, including hardware-keyboard support where appropriate. Remove prominent top-bar controls only after fallback and accessibility parity pass.
- [ ] Use Android/Compose accessibility-aware snackbar durations, readable TalkBack announcements, minimum touch targets and a non-gesture-only dismissal action. Respect reduced motion and avoid snackbar collisions with navigation bars, IME, sheets and file progress indicators.
- [ ] Test subject/session/break/timetable add, rename, edit, delete, duplicate and batch edits, including repeated Undo/Redo; keep destructive-confirmation requirements where they are still appropriate.

**Acceptance gate:** for each supported user edit, feedback names the committed change; Undo restores the prior data without reverting an unrelated later change; Redo reapplies exactly that change; dismissed feedback does not destroy in-session recovery.

### A2. Turn generic errors into specific recovery paths

- [ ] Inventory existing failure messages (including `TempoApp.kt`, `TempoViewModel.kt`, file and updater UI). Define actionable categories: invalid input, invalid/unsupported import, storage write failure, interrupted export, network/update unavailable, and local data read failure.
- [ ] Replace the generic “Something needs attention” + “Close” dead end where a targeted resolution exists. Show a brief problem explanation, what **was or was not changed**, and a relevant **Try again**, **Choose another file**, **Review**, **Open settings**, or **Close** action.
- [ ] Never present raw exception internals as the primary user-facing copy. Preserve useful diagnostic detail for debugging without uploading private timetable contents.
- [ ] Treat cancelled file-pickers and no-op operations as cancellation, not error or success. Prevent duplicate snackbars/dialogs after rotation, lifecycle restarts or repeated taps.
- [ ] Ensure save-failure feedback is emitted only after actual persistence failure, not optimistically before a transaction succeeds; don't falsely claim data is safe when storage status is unknown.
- [ ] Add UI/unit tests for unsupported schema, malformed JSON, invalid sessions, missing file/denied URI, write failure, rapid repeat operations and cancellation.

**Acceptance gate:** every tested failure states what happened, whether the existing data was changed (when known), and offers a useful next action without losing unrelated work.

### A3. End-to-end core-flow check

- [ ] Walk: initial setup → create subject → schedule session → edit session → undo → redo → duplicate/replace day → export → import preview → switch timetables → configure widget.
- [ ] For each step, record: feedback timing/copy, screen/context preserved, back/dismiss behaviour, confirmation needs, accessibility and a deliberate failure test.
- [ ] Run on device/emulator with normal/large font, TalkBack, reduced animations, keyboard and gesture navigation; record bugs as separate reproducible issues rather than widening this release indefinitely.

**Release guardrail:** 0.5.2 is a *candidate* narrow release. If it collides with active widget development, ship the same tasks as an isolated UX slice before or alongside 0.6; do not hold up proven widget fixes.

## Release slice B — 0.6 Widget 2.0: truthful and recoverable widget states

These tasks belong beside the [widget redesign](WIDGET_2_0.md), not in a separate widget project.

- [ ] Add explicit widget-state presentation and recovery cases for deleted/missing fixed timetables, unfinished setup, no activities, holidays/days off, break/free, completed day/week, and temporarily unavailable data.
- [ ] If known data freshness cannot be trusted after a failed refresh, avoid presenting a stale lesson as confidently current. Provide a legible unavailable/refresh/configure affordance when feasible within RemoteViews/host constraints.
- [ ] Validate app→widget consistency after edit, undo, redo, cycle changes, dated exceptions, clock/time-zone changes and widget resize. Don't duplicate date resolution logic.
- [ ] Verify placement/upgrades preserve per-widget settings and reconfiguration paths. A missing fixed timetable must never silently swap to a different timetable.
- [ ] Regression-test on 4-, 5- and 6+-column launchers, light/dark, font scaling, TalkBack, restart/reboot, delayed work and disconnected/offline conditions.
- [ ] Use compatibility fallbacks for unsupported widget renderers. Error handling must not require aggressive background polling, exact alarms or new network access.

**Acceptance gate:** a widget's presentation is either grounded in the shared resolver, clearly an honest empty/completed state, or explicitly indicates a known unavailable/reconfiguration state; it does not silently invent fallback content.

## Release slice C — towards 1.0 Feel & Flow: seamless cross-screen experience

- [ ] Document one interaction-language specification in `core:designsystem`: feedback priority and placement, dialog vs snackbar, destructive confirmation, pressed/saved/failed/loading states, empty-state structure, navigation/back and reduced-motion behaviour.
- [ ] Audit Schedule, Overview, Library, settings, comparison, imports/exports and editors **as connected flows**. Standardise names, action placement, saving semantics and return paths rather than only making screenshots match.
- [ ] Preserve relevant user context across screens and return paths: selected date/week, timetable, scroll/selection position and pending editor content where safe. Check back/predictive-back cancellation and keyboard/IME.
- [ ] Provide intentional empty/exception states for first-run, no subjects, no sessions, all-day holiday, finished day/week, invalid selection and not-yet-configured widgets; each should explain or offer a next action without fabricating progress/attendance.
- [ ] Integrate restrained M3 Expressive motion/haptics to communicate state and spatial continuity. The no-animation and assistive-technology experience must convey equivalent meaning.
- [ ] Test cross-screen consistency with first-time and returning users doing real tasks, not merely visual review. Fix measurable confusion/recovery blockers before adding rare decorative moments.

**Acceptance gate:** a user can perform and recover from the same type of action in any supported destination without learning different rules; no critical action relies solely on animation, colour or gesture.

## Shared QA matrix (run throughout)

| Scenario | Expected user experience |
| --- | --- |
| First install / empty timetable | Clear starting point and obvious next action; no “done” claims. |
| Add/edit/delete/duplicate repeatedly | One correct contextual message, valid Undo/Redo and no accidental reversal of a different change. |
| Editor dismissed, back cancelled, rotation or app background | No falsely confirmed save; preserve or explicitly discard pending changes as designed. |
| Invalid/unsupported `.tempo.json` | Specific safe explanation, unchanged existing collection where guaranteed, and choose-another-file path. |
| Export denied, cancelled or storage full | Correct outcome, no duplicate success, recoverable retry. |
| GitHub update check offline / download interrupted | Explain offline or failure status and next step; never suggest the timetable data was uploaded. |
| Deleted timetable referenced by placed widget | Honest reconfigure state; never silently show another timetable. |
| Empty/free/break/holiday/replaced day, 1–52-week cycle | All app views and widgets agree with shared dated resolver. |
| Reboot, midnight, clock and time-zone change | Refresh/recovery behaves truthfully, including temporarily unavailable information. |
| TalkBack, large text, reduced motion, keyboard and gesture nav | Feedback and recovery remain perceivable, reachable and non-conflicting. |

## Delivery rules

- Treat checkboxes as implementation/QA tasks, not claims of current defects; investigate before changing working behaviour.
- Separate behavioural changes and visual polish in reviewable commits/PRs. Preserve existing data formats and local-first privacy.
- Prefer explicit, testable copy over broad labels such as “premium polish”.
- Do not add telemetry, accounts or mandatory network access for this initiative.
