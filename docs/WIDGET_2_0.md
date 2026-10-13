# Widget 2.0 — Expressive home-screen widgets and Remote Compose exploration

Status: **planned / feasibility-first**, proposed for **0.6** (not a blocker for Feel & Flow or 1.0). Last researched: **14 October 2026**. This is a planning document, not a claim that Remote Compose is already shipping in Tempo.

## Why

Real user feedback: the 2×1 widget feels oversized for its amount of information, yet the text is small. Larger widgets often use their extra area for more text rather than a stronger hierarchy. Different home-screen grids, particularly grids with more than four columns, make nominally identical widget footprints look very different.

**Goal:** recognisably Material 3 Expressive widgets that make the current/next subject, time remaining, and day context legible at a glance. Shapes should structure information, not decorate arbitrary cards. The app, not the home screen, remains the home for rich shape morphing and animation.

**Out of scope for first pass:** a separate timetable engine, a new account/service, second-by-second app wake-ups, artificial productivity mechanics, rebuilding the main app UI, requiring Android 16, or dropping Android 8–15 users.

## Existing Tempo foundation (verified on main before planning)

- App: Android 8+ (minSdk 26), target/compile SDK 36, AGP 8.13.2, Kotlin 2.2.21; Material 3 1.5.0-alpha04 in the app.
- Widgets: three AppWidgetProviders (pill 2×1, square 2×2, wide 4×2); traditional RemoteViews and XML layouts; native Chronometer and subject/icon support.
- **Actual current provider XML is resizable horizontally and vertically**, with size-aware rendering and Android 12 size lists. The older paragraph in ARCHITECTURE.md saying resizeMode=none is outdated and must be corrected during implementation/audit.
- WidgetSizing currently uses width/height thresholds and fontScale; preserve its test coverage while making thresholds content-driven and responsive to usable dimensions.
- Shared weekOverview / widgetDayPlan resolve schedule state. Widget settings support follow/fixed timetable, light/dark/system mode, Material You/presets, icons, locations, lesson backgrounds, fills/outlines, the outer outline and completion-state preferences. Preserve user configuration and old widget IDs.
- Existing update pipeline: WorkManager recovery, boundary refresh events, time/time-zone/reboot handling and native monotonic Chronometer. Do not replace with a polling loop.

Pointers: app/src/main/java/cc/xdan/tempo/widgets/TempoWidgetProvider.kt, WidgetSizing.kt, WidgetSettings.kt, WidgetConfigureActivity.kt, app/src/main/res/xml/widget_*.xml, app/src/main/res/layout/tempo_widget*.xml and docs/ARCHITECTURE.md.

## Important technical distinction: do not conflate the APIs

- **RemoteViews/XML today:** production-compatible baseline, already works in Tempo. Supports layered views, shape/VectorDrawables, drawable tinting, varying text size, touch actions and native ticking Chronometer.
- **Glance 1.2.0 stable / 1.3.0-alpha02:** a Compose-style widget authoring API with RemoteViews interoperability; it does **not** mean arbitrary Compose UI runs in a home-screen launcher. The 1.3 line has Remote Compose-related changes, but upgrading Glance does not automatically give every launcher new rendering features.
- **Remote Compose 1.0.0-alpha21 (7 October 2026):** remote UI document creation and rendering with growing path, layout, text, colour, state and animation APIs. Public library APIs do not by themselves establish a usable launcher-host pipeline on all phones.
- **Glance Adaptive 1.0.0-alpha03 (23 September 2026):** Google describes it as template-first Remote Compose widgets for multiple surfaces; its release notes explicitly state that further public APIs are still to come. Do not assume it is production-ready for generic Android home screens.
- Check device/host renderer support empirically. Android version alone does **not** prove support; test the exact supported integration and fallback. A self-hosted RemoteComposeView demonstration inside Tempo is useful for shape tests but **does not count** as proof of a working launcher widget.
- Keep alpha dependencies isolated to a prototype branch or module. Check SDK/toolchain compatibility and build implications first (Glance 1.3 alpha notes mention compile SDK 37 / AGP 9.2 requirements). Do not destabilise app builds merely to try the renderer.

Sources (official, revisit when implementing):
- Remote Compose releases: https://developer.android.com/jetpack/androidx/releases/compose-remote
- Glance releases: https://developer.android.com/jetpack/androidx/releases/glance
- Glance Adaptive releases: https://developer.android.com/jetpack/androidx/releases/glance-adaptive
- Responsive RemoteViews and size options: https://developer.android.com/develop/ui/views/appwidgets/layouts
- Glance/RemoteViews interoperability: https://developer.android.com/develop/ui/compose/glance/interoperability
- Launcher radius and backgrounds: https://developer.android.com/develop/ui/compose/glance/create-app-widget

## Design system: strict, intentional geometry

Use documented Material 3 corner-radius sizes as the initial token scale: 4, 8, 12, 16, 20, 28, 32 and 48dp, plus the full/pill shape. Choose tokens to communicate grouping/hierarchy rather than picking new radii for every card. Exact values and system corner handling should be checked against the target Material dependency and Android widget host; these are design-token candidates, not an automatic assumption about launcher clipping.

**Hard rule from design review:**
1. A standalone rounded rectangular surface uses **one radius on all four corners** (or all-full for pills/circles). No arbitrary diagonally mismatched corners.
2. Two adjacent members of one connected horizontal group use **mirrored geometry**: equal larger radii on outside corners and equal smaller radii at both inward-facing edges; reverse for the other member. For vertically connected groups, apply the same rule rotated 90 degrees.
3. A three-or-more-member row has mirrored ends; every middle member uses the **same smaller inward radius on all four corners**. For a two-dimensional grouping, explicitly define matching shared edges and reject mismatched transitions.
4. Use a consistent small inter-segment gap, typically 2–4dp as design exploration; do not fake unrelated cards as one group. Connected segments may differ in width/colour, but their shared edge geometry must correspond.
5. Device/system widget background radius and clipping take priority at the **outermost host boundary** (Android 12+ system_app_widget_background_radius; earlier versions get a fallback). Leave sufficient inset around decorative geometry.
6. Special silhouettes (official MaterialShapes-style cookies/clovers/flowers) are **independent objects** or meaningful status/countdown badges, not permission to vary an ordinary rectangle's corners arbitrarily.
7. Accessible reading order, contrast and information hierarchy are more important than making every element a shape.

Implementation options: shared semantic geometry/tokens; XML shape backgrounds for ordinary rectangles; vector paths or pre-rendered raster only for intentionally irregular silhouettes where necessary; Remote Compose path drawing if supported by the proven host integration. Evaluate image scaling/tinting and avoid blurred or stretched vector designs. Prefer named tokens and tested assets over runtime-generated decorative backgrounds.

Inspiration, not a component library copied wholesale: https://www.figma.com/community/file/1504472410356443638/material-3-ui-widgets ; Google's expressive toolbar widget sample in https://github.com/android/platform-samples ; contemporary Google widgets and open-source expressive widget implementations. Avoid indiscriminate gradients, extra containers and meaningless icons.

## Visual/layout contract

**Priority for all sizes:** (a) active or next event, (b) clearly labelled countdown, (c) additional context only when it fits. The first glance should communicate "Physics — 24m left" or "Computer Science — starts in 8m". A countdown is not a plain supporting footnote.

| Available footprint | Planned treatment | Information priority |
| --- | --- | --- |
| Narrow/short (nominal 2×1) | Two mirrored connected surfaces or one readable pill; subject and bold time on separate zones | Subject, live countdown/status; compact icon only when it fits |
| Small square (nominal 2×2) | Large countdown as circular/cookie badge, subject below/alongside; status icon with purpose | Current/next, countdown, optional room |
| Wide (nominal 4×2) | Mirrored two-part hero (subject + dominant timer), optionally a separate symmetrical next-up strip | Current/next, countdown, then what follows |
| Tall/wide expanded 4×3+ | Day-progress timeline or short ordered lesson blocks with current segment highlighted | Hero + upcoming lessons/free/breaks + optional finish time |

Names like 2×1/4×2 are **picker labels**, not hard-coded pixel assumptions. Determine layout from available width and height in dp and font scale, with a small set of stable buckets and adaptive typography. On launchers with five, six or more columns, allow the same 4×2 widget to use horizontal space without awkward blank areas or clipping; keep the compact widget readable where physical cell sizes shrink. A user should be able to resize into a coherent neighbouring layout, not a stretched screenshot.

Exploration should create accurate design mock-ups for **all four schedule states** and **all core sizes** before rewriting the renderer. Test non-square circles and vector scaling before choosing a silhouette for a live timer.

### State, behaviour and user preferences

- **Current:** prominent remaining-time Chronometer or equivalent verified host-driven timer, clear "left" wording, meaningful progress if cost/host allows.
- **Upcoming:** "starts in" and the next event; no ambiguous unlabeled number.
- **Break/free:** accurately distinguish scheduled breaks from calculated free gaps and optionally show time until next lesson; do not invent a subject.
- **Done today / week done / nothing scheduled / timetable deleted:** preserve existing week-bounded semantics, message overrides and reconfiguration prompts. No silent jump to next week.
- Preserve the explicit selected timetable or follow-active preference, subject fills versus themed fills, icon/location settings, independent outline styles and day-complete settings. Migrate any new display-style field with a safe default; do not reset placed widgets.
- Offer **Expressive (default for new configuration)** and **Minimal (optional)** when justified by prototypes. Both use the same geometric rules; do not offer endless style variants or force a redesign onto an existing user's widget without migration review.
- Actions: tap hero to open the relevant timetable/day; additional tappable regions only where reasonably sized and predictably mapped. Do not expose inert chevrons or fake controls.
- Widget preview/configuration must reflect actual display style, theme, light/dark mode, countdown typography, outlined/fill options and supported sizes.

## Architecture and proof-of-concept workstream

**Shared presenter:** use the existing schedule resolver once to derive a small, renderer-agnostic WidgetPresentation state: event kind/title/icon/colour/location, previous/current/next boundaries, progress fraction, target timestamp, status labels, accessibility description and theme. Both renderers must consume equivalent snapshots. Do **not** fork schedule semantics into Remote Compose.

**Stable renderer:** keep current RemoteViews/AppWidgetProvider, native Chronometer, size mapping and XML/vector drawable assets. Glance may be considered as a separate intermediate refactor only if it measurably reduces complexity; it is not a prerequisite for Expressive visuals.

**Experimental renderer:** in an isolated feature/prototype, compile a Remote Compose document containing a mirrored surface, Material shape/path, styled live or time-dependent text, theme values, click action and a layout that adapts to two reported sizes. Test in a real home-screen widget host; document which APIs are available, runtime support, build cost and update behaviour. Add a renderer switch only after support detection is reliable. If there is no supported public host integration, limit the prototype to an in-app preview and **do not ship it as a launcher renderer**.

**Go/no-go gate:** only consider a Remote Compose home-screen production path when all of the following are demonstrated:
- A documented public, redistributable host/Glance pathway works on at least one real launcher without private APIs.
- Shape paths, theme, text, accessibility, taps, responsive sizing and state transitions render correctly on device; a native or host-driven countdown is accurately testable without app-per-second wakeups.
- There is a reliable capability/failure path that leaves every existing widget functional; unsupported OS/hosts use the stable renderer without user intervention.
- Toolchain/dependency requirements, security, resource use and CI builds are acceptable for Tempo's minimum Android version and release cadence.
- A QA build shows no material regression against current RemoteViews behaviour.

If the gate fails, **ship the Expressive redesign on RemoteViews/XML/vector assets** for 0.6 and defer Remote Compose adoption to a later release. The purpose of the spike is to learn, not to block the UI improvements.

## Delivery phases and acceptance criteria

### Phase A — audit and reference designs
- [ ] Audit screenshots and measurements from 4-column, 5-column and 6+ column launchers; capture min/max dp dimensions, padding, font scales, existing overflow issues and retained settings.
- [ ] Document official Material 3 shape/corner tokens; create a small reusable geometry specification with unit tests for standalone, two-part, three-part and rotated group shapes.
- [ ] Make comparison mock-ups of 2×1, 2×2, 4×2 and expanded formats in current, upcoming, free/break and finished states. Prioritise subject identification and glanceable timer; review against the Figma design references.
- [ ] Validate long names ("Computer Science"), unknown icons, privacy-hidden locations, TalkBack and light/dark dynamic colours.

### Phase B — Remote Compose feasibility spike (before a renderer migration)
- [ ] Build a separately gated alpha21 Remote Compose prototype; check SDK/AGP dependencies and do not upgrade production dependencies solely for the experiment.
- [ ] Prove official host integration and launcher display (not just a RemoteComposeView inside the app) with more than one size, shape, text, colours and taps.
- [ ] Record API gaps (including live countdown, dynamic colour, preview, accessibility and clocks), compatible Android/launcher versions, screen captures and measured update costs.
- [ ] Apply the go/no-go gate above and explicitly record **adopt / defer / reject** with evidence and an incremental migration/fallback design.

### Phase C — shared state, visuals and responsive layout
- [ ] Extract testable WidgetPresentation state without duplicating the core resolver.
- [ ] Produce reusable material shape assets/geometry tokens and mirrored component backgrounds with no arbitrary corner differences.
- [ ] Rebuild compact, square and wide compositions around large labelled timers, semantic icons and clear secondary information; expanded sizes can reveal a short timeline.
- [ ] Implement dp/font-scale responsive buckets and resize transitions; preserve native Chronometer if the chosen host rendering cannot match its efficient behaviour.
- [ ] Ensure actual system widget clipping, gutters, tinting and outline widths are correct across supported OS versions.

### Phase D — settings, reliability, testing, rollout
- [ ] Keep existing placements, per-widget preferences, theme/outline options, screenshots and configure/preview flows working; safely migrate any newly introduced style choice.
- [ ] Keep time-zone, date/time, reboot, app update, holiday, A/B cycle, replacement day, overlap, break and week-completion behaviour identical to the schedule engine.
- [ ] Add unit/screenshot/instrumentation checks for rendering states, corner-mirroring, resizing and accessibility; run debug/QA/release CI with R8/resource shrinking before release.
- [ ] Test Android 8/11/12/14/16 where devices or emulators are available; 4/5/6+ columns, landscape, foldable/tablet, different launchers, system font sizes 1.0/1.3/1.5+, TalkBack, light/dark, Material You (12+), theme overrides, power saver and Doze.
- [ ] Verify widget refreshes at lesson boundaries without exact-alarm permission, no repeated per-second app wakeups, no excessive binder payloads, no stale/corrupt displays after host recreation and no accidental private timetable data exposure.
- [ ] Document known launcher-specific limitations and only roll out the Remote Compose renderer behind verified compatibility; keep a rollback path.

## Done means

- In ordinary use, the 2×1 primary subject and time-to-next/current are readable without opening the app; it is not merely a tiny-text label in a huge card.
- The 4×2 feels deliberately composed on 4-, 5- and 6-column home screens; size changes alter information density, not just stretch text.
- Group corners are provably consistent with the mirrored rule; the widget outer corners obey launcher clipping.
- Material You and all current widget theme/outline preferences remain functional, accessible and contrast-safe.
- Current/next, breaks/free periods, completion and missing timetable states are accurate; countdown remains efficient and correctly labelled.
- Production support remains at Android 8+ regardless of Remote Compose feasibility.
- A technical decision record documents Remote Compose results and whether/why it shipped or remained experimental.

## Release decision

**Candidate 0.6:** visual design, responsive layout and user-feedback fixes, backed by the stable renderer. Include Remote Compose only if Phase B passes all gates. If not, keep the proof of concept and schedule another evaluation when Glance Adaptive/launcher integration provides suitable public APIs. This milestone is independent of the wider Feel & Flow design direction and does not require accounts or other unrelated features.
