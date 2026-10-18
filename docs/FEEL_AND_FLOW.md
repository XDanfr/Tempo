# Feel & Flow — the Tempo 1.0 experience direction

Status: **design proposal**, not implemented or a fixed release promise. Features may arrive in smaller releases before 1.0. This supplements [the roadmap](ROADMAP.md) and must not imply that 0.5 is unfinished. The earlier, testable feedback/recovery slice and shared QA contract are specified in [Experience Integrity](EXPERIENCE_INTEGRITY.md); do not reimplement those independently in 1.0.

## Product principles

Tempo is an expressive, accessible timetable utility for school, college, work and everyday schedules — not a habit tracker or productivity game.

1. **Useful before playful.** Motion communicates state, position or success. No meaningless animation, cosmetic clutter or forced engagement.
2. **Local-first by default.** People can create, edit, compare, use widgets, import and export timetables without accounts or network access.
3. **No manufactured pressure.** No login streaks, XP, leaderboards, punitive reminders or rewards for unverified attendance. Elapsed time is not evidence of completed work.
4. **Discoverable, not intrusive.** Progressive hints, permanent user control and optional celebrations; no mandatory touring of unrelated features.
5. **Respect the system and user.** Honour accessibility settings (including animator duration scale and reduced motion), contrast, large text, screen readers and haptics preferences.

## 1. Unified Schedule and Week Overview

### Day-first Schedule

- Replace separate Today and Timetable destinations with a **Schedule** destination, opening on today by default.
- Swipe horizontally to move between consecutive calendar dates; tapping a date in a compact week strip uses the same spatial transition. Provide buttons/semantics so swipe is not the sole navigation method.
- Preserve selected date while switching to Overview and back; provide an easy return-to-today action.
- Show dated replacements, days off, holidays, 1–52-week cycle anchors and overlaps from the **existing shared schedule resolver**, not duplicate UI-specific recurrence rules.
- Clear live/finished/upcoming/free/break states, proportional time progress, sensible empty and completed days. No false "done" claims based purely on elapsed time.
- Avoid gesture conflicts with vertical event scroll, editor sheets, system back, nested horizontal components and TalkBack.

### Week Overview

- An actual **time-aligned, scrollable week grid**, not just a second day list. Indicate activity blocks by subject colour/icon, duration and overlap with a readable alternative view for accessibility.
- A week picker (previous/next, jump to current) with the currently selected dated week; support locale-appropriate week start where feasible.
- A summary for the **resolved selected week**: sessions per subject (counts, optionally scheduled duration), free gaps/periods and total free time, scheduled breaks and total sessions. Do not confuse breaks with free time.
- Clearly distinguish **whole week** from **remaining week**. Remaining is relative to the current device-local date/time only if the selected week includes now; past weeks show zero remaining, future weeks show all upcoming. Make this context explicit.
- Calculate frees only inside each configured day's hours; exclude overlaps/occupied intervals, holidays or days off where applicable. Decide whether a "free period count" uses existing period boundaries versus continuous gaps, and label it consistently. Do not count an interval twice in overlaps.
- Tapping a block opens its day in Schedule; the grid works with 1–52-week cycles, exception dates, days with no events and possibly non-school/weekend schedules.
- Maintain useful small-screen and tablet/foldable layouts, accessible focus order, colour-independent statuses, large fonts and empty/exception states.

## 2. Motion and micro-interactions

Establish a small, documented motion vocabulary in `core:designsystem` (tokens/behaviours, not one-size-fits-all timing).

- Spring-based day paging, date selection, block placement, theme changes and selection states, preserving perceived continuity.
- Clear pressed, saved, undo/redo, imported, exported, completed and error feedback; never animate merely for decoration. Continue the contextual bottom snackbar with Undo → Redo and dismiss for committed edits, with an accessible history fallback; do not show confirmation per keystroke.
- Use shared-element or positional continuity selectively, without motion glitches on rapid gestures or configuration changes.
- Haptics when meaningful and user-configurable; optional sound is off by default.
- Prefer subtle, predictable feedback to confetti during normal use.
- Honor device accessibility/motion settings, battery/performance concerns and no-animation fallback.

## 3. Living timetable and honest progress

- Visually distinguish past, current, next and later activities. Show progress through current scheduled intervals and through the day/week, with explicit "elapsed" or "remaining" wording.
- Maintain high fidelity between in-app views, widgets and exports using the existing resolver; presentation can differ but schedule truth must not.
- Time transitions must survive foreground/background, midnight, time/date changes, time-zone changes, cycle changes and dated exceptions.
- Free time and configured breaks have distinctive semantics; finished day/week states are calm, not nagging.
- **Weekly recap**, if added, summarises schedule facts (e.g. lesson counts, scheduled hours, free time), not inferred attendance or productivity.

## 4. Optional milestones and celebrations

- Small permanent **discovery milestones** for actions already useful: first timetable, theme customisation, widget configured, first export/import, advanced feature explored.
- No XP, streaks, public rankings, artificial unlock gates or achievements that require returning daily.
- Rounded top-of-screen notification (icon, name, short explanation), brief optional particles/confetti for meaningful milestones, haptics if enabled. Queue/suppress duplicates and avoid interruption during editing/onboarding.
- A simple Milestones page to revisit earned items; do not make it a primary tab.
- Store achievements locally and avoid uploading usage telemetry. Reinstall/restore behaviour should be explicit.

## 5. Post-onboarding discovery

- Onboarding stays short; after completion show context-sensitive, dismissible hints only when a relevant control is first encountered (e.g. swipe day, use Overview, theme, widget setup).
- Never block the user behind a tour or require milestones to continue. Provide Help/Replay hints and reset controls.
- Persist dismissals locally. Ensure TalkBack, reduced-motion and keyboard/navigation parity.
- Avoid promising unsupported functionality; add hints when the relevant feature lands.

## 6. Sharing links and optional creator profiles

**Existing foundation:** portable `.tempo.json` snapshots and explicit import previews. See [ARCHITECTURE.md](ARCHITECTURE.md) for the proposed link code and validator. Link sharing is **not implemented** at the time of writing.

### Sharing first

- Publish only through an explicit user action and a clear preview of what will become public. Preserve an account-free read/import path.
- Planned permalink shape: `https://xdan.cc/tempo/share/<code>`. The code must be validated as documented in the architecture and reveal no timetable data itself.
- Store a published **snapshot** with its own publication ID/revision and revocation control; importing yields an independent local copy. Updating a private timetable does not silently republish; deleting a share does not delete imported copies.
- Keep publication metadata minimal; define expiration, retention, abuse reporting, rate limits and removal support before launch.
- Explain that anyone holding an unlisted link can potentially access its contents; unlisted is not private/encrypted.

### Creator profiles (optional follow-on, not prerequisite)

A **Creator** identity belongs to someone who wants to publish timetables; it is separate from the personal timetables stored locally.

- Optional account/profile: handle or display name, avatar, short bio, and a chosen list of public published timetables. Profiles must be opt-in, and the default collection is private/local.
- A public shared timetable can show creator attribution when the publisher explicitly opts in; an anonymous/unlisted share must not leak private identity through identifiers, metadata or URLs.
- One account may manage publications, but it must not require uploading, synchronising or owning every local timetable.
- An imported timetable is a **forked independent copy** by default, not an auto-subscribed live feed. Any future update/subscription model must be an explicit separate feature.
- No followers, feeds, public achievements or social metrics in the initial profile scope.
- Investigate secure authentication (including passkeys where appropriate), account recovery/deletion, editable handles, impersonation/abuse moderation, privacy policy and data export before choosing backend architecture.
- Schools and minors may use Tempo: publishing defaults should avoid exposing personal schedules, locations, notes or attendance patterns. Consider a privacy-first public template that strips sensitive fields by default and requires explicit disclosure for each additional field.
- Existing portable file sharing, widgets, editing and offline use must continue to work for signed-out users.

**Ordering:** ship/test anonymous link publication and import before optional public creator profiles. A hosted service and associated maintenance, moderation and legal obligations may put profiles after 1.0.

## Delivery approach and readiness

Treat Feel & Flow as a cross-release quality workstream, not a single rushed feature. The roadmap's existing functional backlog continues independently.

| Phase | Candidate work | Exit criteria |
| --- | --- | --- |
| Foundations | Motion primitives, feedback, accessibility and gesture tests | Reduced-motion, TalkBack, font scale and rapid-interaction behaviour verified |
| Navigation | Unified day-first Schedule + date paging | No Today/Timetable duplication; all dated exceptions resolve correctly |
| Overview | Time-grid, per-subject/free summaries, whole/remaining week | Resolver parity; overlaps, breaks, holidays, empty weeks and cycle boundaries tested |
| Discovery | Contextual guidance, optional milestones, non-blocking celebrations | Never obstructs tasks, no repeated awards, can disable feedback |
| Sharing | Explicit snapshot publication, validated codes, preview/import, revoke | Privacy threat model, abuse/retention plan, service monitoring and tests |
| Profiles (optional) | Creator identity and chosen publications | Local-only use remains complete; safe profile/auth/data-deletion behaviour |

**1.0 quality bar:** coherent interaction language; polished day/week navigation; accurate dated-state summaries; accessible/responsive behaviour; fast start/scroll; reliable import/export and widgets; documentation that matches shipped capabilities. Sharing and profiles should not be claimed as 1.0 requirements until feasibility and dependencies are validated.

## Behavioural UX principles and practical review checks

The following principles are informed by the two linked videos' **publicly available descriptions and secondary transcript summaries**, not an independent frame-by-frame review. They are hypotheses to test with real users rather than claims that particular conversion tactics will work for Tempo.

| Principle | Tempo application | Do not do |
| --- | --- | --- |
| **Smart defaults / choice reduction** | Preselect useful day hours and cycle lengths where the context supports them, provide editable templates, and group advanced options behind clearly labelled secondary controls. | Silently assume everyone's schedule is a five-day school week; hide settings that people need to finish a task. |
| **Early tangible value** | Let users create, preview and personalise a timetable immediately, before any optional account or publishing step. Offer local import as an equally prominent starting path. | Delay useful features until registration, email verification or consent to publicity. |
| **Ownership through personalisation** | Invite a name, subject colours or icons during setup; show a real preview that reflects those edits. Make choices reversible. | Force a prolonged onboarding tour or make people customise things they do not care about. |
| **Honest goal progress** | Show actual completed setup steps, accurate time remaining in a scheduled interval and whole/remaining week counts. | Present fictitious head starts in onboarding, equate time elapsed with attendance, or turn the user's timetable into a compulsory daily goal. |
| **Competence over vanity badges** | Optional permanent milestones recognise useful firsts (first schedule, widget, import/export, sharing) and point to what was accomplished. | Award points for opening the app, create daily streak anxiety, or gate essential themes and controls. |
| **Delight with restraint** | Use small motion/haptic confirmations for successful edits and first-use discoveries; reserve richer confetti for rare voluntary milestones. | Repeated alerts, randomised rewards designed to prolong use, autoplay audio or blocking celebration overlays. |
| **Clarity over pressure** | Make consequences of destructive actions, overwrite and public sharing plain, with preview and undo/revoke where appropriate. | Use countdown urgency, deceptive loss framing, bait-and-switch prompts or manipulative pricing comparisons. |

### Task-based acceptance checks

- A first-time user can make or import a usable timetable without registering and without needing to understand every appearance setting.
- After onboarding, a user can identify today's next activity and swipe or otherwise navigate to tomorrow without a guided tutorial.
- A user can find and compare this week's real subject counts and free time, including exception dates, without guessing what "remaining" means.
- A user who dismisses achievements and discovery hints can complete everything with the same efficiency.
- Public sharing exposes an exact, inspectable set of fields, with a clear way back out; selecting a creator profile never exposes the owner's other private timetables.
- A reduced-motion user and a screen-reader user receive equivalent meaning, navigation and success confirmation.

## Research and design references

- [Tim Gabe — The Psychology of Premium Apps](https://youtu.be/8EAYZe6y3I8) (October 2026, full transcript provided by the app developer): audit real state transitions, cross-screen consistency and deliberate failure recovery rather than judging standalone polished screens; translated into [Experience Integrity](EXPERIENCE_INTEGRITY.md).

- [Tim Gabe — How to Design Gamification That Actually Works](https://youtu.be/LXX_qOA5D8E) (11 May 2026): useful caution against substituting XP, badges and leaderboards for intrinsically valuable behaviour; favour genuine competence feedback, completion clarity and feature restraint. Interpreted from a secondary transcript summary.
- [uxpeak — The UX Psychology Behind Apps People Can't Stop Using](https://youtu.be/2TlIg3VokY8) (2 July 2026): smart defaults, choice overload, early value before sign-up, ownership through customisation and visible progress. Some examples emphasise conversion/loss aversion; Tempo should **not** adopt manipulative pressure framing. Interpreted from a secondary transcript summary.
- Validate the designs with people relying on Tempo for education, work and personal schedules, including people who want **no gamification**.
