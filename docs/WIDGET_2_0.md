# Widget 2.0 — Adaptive, canonical Material 3 Expressive widgets

Status: **implementation begun / feasibility-first**, proposed for **0.6** (not a blocker for Feel & Flow or 1.0). See [the canonical composition and configuration rewrite](WIDGET_2_0_REDESIGN.md) and the [historical first pass](WIDGET_2_0_IMPLEMENTATION.md). Last researched: **14 October 2026**. This is a planning document, not a claim that Remote Compose is already shipping in Tempo.

Implementation decision, 14 October 2026: the owner subsequently approved the
SDK/AGP upgrade required for latest Glance alpha adoption. The feature branch now
uses compile SDK 37.0 / AGP 9.2.1 / Glance 1.3.0-alpha02. The original SDK36
constraint below records the starting baseline; min SDK26 and target36 remain.
Adaptive / Remote Compose host acceptance gates still apply.

## Why

Real user feedback: the 2×1 widget feels oversized for its amount of information, yet the text is small. Larger widgets often use their extra area for more text rather than a stronger hierarchy. Different home-screen grids, particularly grids with more than four columns, make nominally identical widget footprints look very different.

**Goal:** a ground-up redesign of the widget presentation and responsive layout architecture, not a reskin of the existing pill, square and wide layouts. Tempo should feel at home on Android, using canonical widget patterns, meaningful expressive shapes and a clear current/next countdown. A user should feel they gain useful information as they resize rather than seeing stretched text. The app, not the home screen, remains the home for rich shape morphing and animation.

**Out of scope for first pass:** a separate timetable engine, a new account/service, second-by-second app wake-ups, artificial productivity mechanics, rebuilding the main app UI, requiring Android 16, or dropping Android 8–15 users. The ground-up scope is **presentation, composition, layout classification, preview and rendering architecture**; preserve the proven schedule/data logic.

## Product direction: one adaptive widget, progressive disclosure

The main new launcher-pickers experience should be **one Adaptive Tempo widget**, initially presented at a useful compact footprint and freely resizable horizontally/vertically. The user picks the timetable/theme once and can resize to reveal more capability. A smaller layout is never merely a clipped larger layout; it is a deliberately composed representation of the **same widget data and actions**.

The concept follows Google's [canonical widget layouts](https://developer.android.com/design/ui/mobile/guides/widgets/layouts) and [widget sizing/breakpoint guidance](https://developer.android.com/design/ui/mobile/guides/widgets/sizing), as illustrated by the user's referenced [Android widget design video](https://youtu.be/VnjgKzAa0ws) (specifically the single-entity concept and visual progression when resizing). Google's [Widget Canonical Builder](https://goo.gle/widget-canonical) and [Android Platform Samples](https://github.com/android/platform-samples) are primary reference material. Distinguish these authoritative guidelines from design concepts in the community [Material 3 UI Widgets Figma pack](https://www.figma.com/community/file/1504472410356443638/material-3-ui-widgets).

### Important lifecycle / migration constraint

**An AppWidgetProvider cannot simply morph into a different installed provider on resize.** The installed instance retains its provider component and ID; only its rendered layout and content switch at breakpoints. Existing Tempo installs have **three distinct provider components** (`PillWidget`, `SquareWidget`, `WideWidget`) and independent per-ID settings. Adding one unified provider for new placements must **not orphan old instances or reset their configuration**. Keep the legacy receivers and make them use the same new adaptive rendering pipeline; decide whether duplicate picker entries can safely be retired or reduced after verifying actual launchers and upgrade behaviour. Do not disable old providers unless installed widget continuity is proven.

### Canonical composition hierarchy

1. **Single-entity / compact** (typically 2×1): one current/next event and a prominent **labelled** countdown, with optional icon only if it remains readable. This is the primary Tempo widget identity. Two connected pieces are allowed when they clarify subject versus time and obey mirrored corners.
2. **Visual single-entity / square** (typically 2×2): promote the countdown to a circular/cookie-like display or another intentional Material shape; add an appropriate event state and a useful next-time detail where possible.
3. **Multi-entity / wide** (typically 4×2): a deliberately split hero (subject + countdown/progress) and **one supplementary entity** such as next event, without making the current event smaller.
4. **Expanded dashboard / agenda** (4×3+, 2×3 or tablet equivalents): reveal an ordered mini-timeline, multiple upcoming sessions, free/break blocks or finish-time detail only if sufficient width/height. It must remain glanceable, not become a full app page.
5. **Aspect-ratio and font-scale variations**: tall but narrow widgets may reveal a vertical stack; wide but short widgets stay a horizontal hero. Do not infer a layout only from nominal cell count or treat a tall widget like a wide one.

**Progressive disclosure:** retain the subject/status/countdown through every size, then reveal progress, next event, and finally the mini agenda as space permits. The widget should feel more useful on expansion without promising gimmicky “unlocks”. Resizing must never introduce misleading activity claims, delayed timer updates or accidental taps.

**Sizing/breakpoints:** test with the actual bounds and pixel-density-independent reported dimensions; size buckets should be based on usable dp width *and* height, adjusted for font scale and icon/text measurements, not fixed grid-cell names. Google's published handheld reference ranges are roughly 109–306dp wide for 2-column footprints and 245–624dp wide for 4-column footprints; cells vary considerably with device, launcher, orientation and grid settings. Do not blindly use those ranges as final breakpoints. Prefer a small explicit responsive layout map for Android 12+ (e.g. RemoteViews size-map) and compatible option-size fallback for older hosts. Min/max constraints and visual inset must be tested, including Google's guidance that the widget fill its allotted host bounds; don't introduce arbitrary transparent outer margins that make it look misaligned.

### Ground-up work versus preserved foundations

**Replace or redesign:** the current distinct XML visual templates, ad-hoc per-size branches, text-dense wide agenda presentation, size-bucket contract, picker previews and widget configuration's preview UI. Extract a renderer-agnostic presentation snapshot and implement deliberate compact/square/wide/tall canonical compositions. Avoid mechanically translating every old layout into Glance.

**Keep and modernise:** the domain schedule resolver, persistent widget IDs/settings, Chronometer-driven efficiency, provider refresh/recovery architecture, user-selected timetables, Material You/presets, subject icons/colours, outline and privacy settings, dates/cycle/exception behaviour, tests for those behaviours and Android 8+ compatibility.

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
- **Remote Compose 1.0.0-alpha21 (7 October 2026):** remote UI document creation and rendering with growing path, layout, text, colour, state and animation APIs, including public `RemoteEnterTransition.Particle`/`RemoteExitTransition.Particle` and `RemoteModifier.animateEnterExit`. The Particle transition is documented as an **element entrance/exit effect**, not as a configurable falling-confetti emitter; prototype the intended appearance instead of promising it. Public library APIs do not by themselves establish a usable launcher-host pipeline on all phones.
- **Glance Adaptive 1.0.0-alpha03 (23 September 2026):** Google describes it as template-first Remote Compose widgets for multiple surfaces; its release notes explicitly state that further public APIs are still to come. Do not assume it is production-ready for generic Android home screens.
- Check device/host renderer support empirically. Android version alone does **not** prove support; test the exact supported integration and fallback. A self-hosted RemoteComposeView demonstration inside Tempo is useful for shape tests but **does not count** as proof of a working launcher widget.
- Keep alpha dependencies isolated to a prototype branch or module. Check SDK/toolchain compatibility and build implications first (Glance 1.3 alpha notes mention compile SDK 37 / AGP 9.2 requirements). Tempo currently compiles with SDK 36; do not upgrade it merely for features deferred beyond the first Widget 2.0 release. Do not destabilise app builds merely to try the renderer.

Sources (official, revisit when implementing):
- Remote Compose releases: https://developer.android.com/jetpack/androidx/releases/compose-remote
- Glance releases: https://developer.android.com/jetpack/androidx/releases/glance
- Glance Adaptive releases: https://developer.android.com/jetpack/androidx/releases/glance-adaptive
- Responsive RemoteViews and size options: https://developer.android.com/develop/ui/views/appwidgets/layouts
- Glance/RemoteViews interoperability: https://developer.android.com/develop/ui/compose/glance/interoperability
- Launcher radius and backgrounds: https://developer.android.com/develop/ui/compose/glance/create-app-widget
- Glance snap-scroll requirements, `LazyColumn`, `SizeMode.Exact`, and fallback: https://developer.android.com/develop/ui/compose/glance/build-ui#snap-scrolling
- Remote Compose particle entry/exit API: https://developer.android.com/reference/kotlin/androidx/compose/remote/creation/compose/modifier/RemoteEnterTransition
- Remote Compose transition modifiers: https://developer.android.com/reference/kotlin/androidx/compose/remote/creation/compose/modifier/package-summary

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

Implementation options: **favour Remote Compose's native shapes, paths, clipping and layout expressions for mirrored corners when a real compatible widget renderer is proven**; the library handles the actual geometry/rendering, not the **design rule** (which we must still implement using shared shape tokens and test). Remote Compose does not automatically choose matched radii for neighbouring components. Keep matching XML/VectorDrawable assets and/or pre-rendered fallback for the standard RemoteViews path. Evaluate scaling/tinting and avoid blurred/stretched assets or two independent geometry systems that gradually diverge. Prefer named tokens and tested assets over runtime-generated decoration.

Inspiration, not a component library copied wholesale: https://www.figma.com/community/file/1504472410356443638/material-3-ui-widgets ; Google's expressive toolbar widget sample in https://github.com/android/platform-samples ; contemporary Google widgets and open-source expressive widget implementations. Avoid indiscriminate gradients, extra containers and meaningless icons.

## Visual/layout contract

**Priority for all sizes:** (a) active or next event, (b) clearly labelled countdown, (c) additional context only when it fits. The first glance should communicate "Physics — 24m left" or "Computer Science — starts in 8m". A countdown is not a plain supporting footnote. **These are separate canonical compositions built around one adaptive widget instance, not three fixed widget identities.**

| Available footprint | Planned treatment | Information priority |
| --- | --- | --- |
| Narrow/short (nominal 2×1) | Two mirrored connected surfaces or one readable pill; subject and bold time on separate zones | Subject, live countdown/status; compact icon only when it fits |
| Small square (nominal 2×2) | Large countdown as circular/cookie badge, subject below/alongside; status icon with purpose | Current/next, countdown, optional room |
| Wide (nominal 4×2) | Mirrored two-part hero (subject + dominant timer), optionally a separate symmetrical next-up strip | Current/next, countdown, then what follows |
| Tall/wide expanded 4×3+ | Day-progress timeline or short ordered lesson blocks with current segment highlighted | Hero + upcoming lessons/free/breaks + optional finish time |

Names like 2×1/4×2 describe reference grid footprints, **not separate widget types or hard-coded pixel assumptions**. Determine layout from available width and height in dp and font scale, with a small set of stable buckets and adaptive typography. On launchers with five, six or more columns, allow the same 4×2 widget to use horizontal space without awkward blank areas or clipping; keep the compact widget readable where physical cell sizes shrink. A user should be able to resize into a coherent neighbouring layout, not a stretched screenshot.

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

## Optional micro-celebration: finished scheduled day

A new **scheduled-day-complete state** may trigger a *very small* one-shot flourish, **only if actual Remote Compose host animation is supported and reliable**. Google's Remote Compose API exposes `RemoteEnterTransition.Particle` and `RemoteExitTransition.Particle` (1.0.0-alpha21) for state-layout visibility transitions via `RemoteModifier.animateEnterExit`. These are **not documented as a general-purpose custom-colour falling-confetti particle emitter**, so investigate the actual effect before committing to the desired falling-cookie/clover/diamond appearance.

Design intent: when the last scheduled lesson ends, retain the existing truthful **"Done for today"** state with a calm static icon/title, possibly a few Material expressive shapes falling briefly over otherwise legible content. Use the selected widget theme/subject colour accents and keep animation **short (target under ~1 second), sparse, local to the widget, silent and non-blocking**. Nothing should animate indefinitely or on every app/widget refresh.

- [ ] Prototype `RemoteEnterTransition.Particle` on state change and inspect whether it creates a suitable *falling* celebration or a different entrance/exit visual. If unsuitable, don't claim it delivers custom confetti; investigate a documented host-supported alternative separately.
- [ ] Gate the effect on **actual rendered state transition**, not a scheduled lesson completion timestamp alone. Android may delay or suppress background widget updates, and Tempo cannot guarantee the widget was visible when the state changed.
- [ ] Avoid replays on ordinary Chronometer updates, theme changes, app/launcher restarts, rebindings, resize, next-day rollover or scrolling back to a completed page. Aim for at-most-once per **widget ID + resolved local date**, and verify lifecycle behaviour; if reliable single-play cannot be achieved, prefer the static celebration.
- [ ] Respect system reduced-motion/animation-disabled settings, user expectations and power saver, and provide a simple widget option to disable celebrations where justified. Avoid sudden flashing and keep accessibility text and tap targets steady.
- [ ] **No celebration of inferred attendance/productivity:** finishing the scheduled day means the clock has passed its last scheduled event; Tempo cannot assert the person attended or completed work. Do not introduce rewards, streaks or noisy completion prompts.
- [ ] On RemoteViews or older unsupported hosts, render the same **static expressive completion shape** without fake animated-GIF loops, high-frequency redraws or mandatory effects.
- [ ] Test multiple timetable cycles, changes to the final lesson, empty days, time-zone changes, DST, fast-forward clock, long periods off-screen, TalkBack, font scale and dark/light/Material You.

**Release gate:** the particle flourish is an optional enhancement, not a release blocker; the first batch stays non-scrollable. If a suitable animation cannot ship safely, keep the ground-up redesign and a static completion shape. Review scrolling only **after** the first widget batch has been implemented and tested.

## Architecture and proof-of-concept workstream

**Shared presenter:** use the existing schedule resolver once to derive a small, renderer-agnostic WidgetPresentation state: event kind/title/icon/colour/location, previous/current/next boundaries, progress fraction, target timestamp, status labels, accessibility description and theme. Both renderers must consume equivalent snapshots. Do **not** fork schedule semantics into Remote Compose.

**Stable renderer:** retain current RemoteViews/AppWidgetProvider components for upgrade continuity, native Chronometer and XML/vector drawable assets; **replace their visual composition and sizing rules**, not just their colours. Create a single adaptive presentation entry point used by all legacy providers and, if feasible, one preferred new picker provider. Glance may be considered as a separate intermediate refactor only if it measurably reduces complexity; it is not a prerequisite for Expressive visuals.

**Experimental renderer:** in an isolated feature/prototype, compile a Remote Compose document containing a mirrored surface, Material shape/path, styled live or time-dependent text, theme values, click action and a layout that adapts to two reported sizes. Test in a real home-screen widget host; document which APIs are available, runtime support, build cost and update behaviour. Add a renderer switch only after support detection is reliable. If there is no supported public host integration, limit the prototype to an in-app preview and **do not ship it as a launcher renderer**.

**Go/no-go gate:** only consider a Remote Compose home-screen production path when all of the following are demonstrated:
- A documented public, redistributable host/Glance pathway works on at least one real launcher without private APIs.
- Shape paths, theme, text, accessibility, taps, responsive sizing and state transitions render correctly on device; a native or host-driven countdown is accurately testable without app-per-second wakeups.
- There is a reliable capability/failure path that leaves every existing widget functional; unsupported OS/hosts use the stable renderer without user intervention.
- Toolchain/dependency requirements, security, resource use and CI builds are acceptable for Tempo's minimum Android version and release cadence.
- A QA build shows no material regression against current RemoteViews behaviour.

If the gate fails, **ship the Expressive redesign on RemoteViews/XML/vector assets** for 0.6 and defer Remote Compose adoption to a later release. The purpose of the spike is to learn, not to block the UI improvements.

## Delivery phases and acceptance criteria

### Phase A — audit, canonical specification and reference designs
- [ ] Audit the video/screenshot's single-entity canonical idea and Google's canonical widget layouts, sizing/breakpoint rules, official Figma Widget Canonical Builder and public platform code samples; compare with community Material 3 Expressive designs. Avoid claiming the screenshot is a working Tempo layout.
- [ ] Define the one-adaptive-widget picker/legacy-provider transition and document how existing IDs/settings survive app upgrades and resizing.
- [ ] Write the progressive-disclosure state matrix: compact -> square/visual -> wide/multi-entity -> expanded agenda, including tall-narrow and short-wide variants.
- [ ] Audit screenshots and measurements from 4-column, 5-column and 6+ column launchers; capture min/max dp dimensions, padding, font scales, existing overflow issues and retained settings.
- [ ] Document official Material 3 shape/corner tokens; create a small reusable geometry specification with unit tests for standalone, two-part, three-part and rotated group shapes.
- [ ] Make comparison mock-ups of 2×1, 2×2, 4×2 and expanded formats in current, upcoming, free/break and finished states. Prioritise subject identification and glanceable timer; review against the Figma design references.
- [ ] Validate long names ("Computer Science"), unknown icons, privacy-hidden locations, TalkBack and light/dark dynamic colours.

### Phase B — Remote Compose feasibility spike (before a renderer migration)
- [ ] Build a separately gated alpha21 Remote Compose prototype; check SDK/AGP dependencies and do not upgrade production dependencies solely for the experiment.
- [ ] Prove official host integration and launcher display (not just a RemoteComposeView inside the app) with more than one size, shape, text, colours and taps.
- [ ] Test alpha21 particle *enter/exit* transitions against the intended end-of-day confetti; capture video and decide whether the effect actually resembles falling expressive shapes without unsupported assumptions.
- [ ] Record API gaps (including live countdown, dynamic colour, preview, accessibility and clocks), compatible Android/launcher versions, screen captures and measured update costs.
- [ ] Apply the go/no-go gate above and explicitly record **adopt / defer / reject** with evidence and an incremental migration/fallback design.

### Phase C — ground-up widget presentation and responsive layout
- [ ] Create one primary adaptive widget UX/renderer, rather than shipping three independent visual designs; preserve old provider receivers as compatibility adapters.
- [ ] Choose small canonical layout buckets based on measured usable dp width/height and font scale; verify breakpoint switching in both resize directions and after launcher restart, orientation changes and app updates.
- [ ] Reveal information progressively: event+timer -> visual progress/next -> additional next-up/agenda information. Preserve the hero's clear hierarchy at every size.
- [ ] Evaluate replacing the three legacy picker entries with a single preferred new entry without removing existing users' placed widgets; document launcher-specific compromises.
- [ ] Extract testable WidgetPresentation state without duplicating the core resolver.
- [ ] Use Remote Compose path/shape/clip drawing for mirrored groups wherever the real widget host supports it, backed by a single shared geometry/token contract and equivalent XML/vector fallback; test paired and 3+-member groups across themes.
- [ ] Rebuild compact, square and wide compositions around large labelled timers, semantic icons and clear secondary information; expanded sizes can reveal a short timeline.
- [ ] Implement dp/font-scale responsive buckets and resize transitions; preserve native Chronometer if the chosen host rendering cannot match its efficient behaviour.
- [ ] Ensure actual system widget clipping, gutters, tinting and outline widths are correct across supported OS versions.

### Phase D — settings, reliability, testing, rollout
- [ ] Test Android's host resize event/size-list semantics, RemoteViews size-map switching, launcher padding and zero-extra-outer-inset alignment; verify the app does not need to wake for purely host-selected layout changes.
- [ ] Test whether resize preserves the configured timetable, appearance and action mappings without removing/readding the widget.
- [ ] Keep existing placements, per-widget preferences, theme/outline options, screenshots and configure/preview flows working; safely migrate any newly introduced style choice.
- [ ] Keep time-zone, date/time, reboot, app update, holiday, A/B cycle, replacement day, overlap, break and week-completion behaviour identical to the schedule engine.
- [ ] Add unit/screenshot/instrumentation checks for rendering states, corner-mirroring, resizing and accessibility; run debug/QA/release CI with R8/resource shrinking before release.
- [ ] Test Android 8/11/12/14/16 where devices or emulators are available; 4/5/6+ columns, landscape, foldable/tablet, different launchers, system font sizes 1.0/1.3/1.5+, TalkBack, light/dark, Material You (12+), theme overrides, power saver and Doze; test with reduced-motion animations disabled and enabled.
- [ ] Verify widget refreshes at lesson boundaries without exact-alarm permission, no repeated per-second app wakeups, no excessive binder payloads, no stale/corrupt displays after host recreation and no accidental private timetable data exposure.
- [ ] Document known launcher-specific limitations and only roll out the Remote Compose renderer behind verified compatibility; keep a rollback path.

## Done means

- In ordinary use, the 2×1 primary subject and time-to-next/current are readable without opening the app; it is not merely a tiny-text label in a huge card.
- **The same configured widget** changes from a compact single-entity view into a more informative visual/multi-entity view when resized; it also gracefully removes supplementary content when shrunk, without resetting state or losing the countdown.
- The 4×2 feels deliberately composed on 4-, 5- and 6-column home screens; size changes alter information density, not just stretch text.
- Group corners are provably consistent with the mirrored rule; the widget outer corners obey launcher clipping.
- The initial expanded layout reveals a useful, readable **non-scrollable** mini-agenda as space allows; it offers no inert pagination or swipe hints. Scroll feasibility remains a post-release research topic.
- Day-complete state has a clear static expressive outcome everywhere, with a brief optional particle flourish only on demonstrably supported hosts and without excessive replay.
- Material You and all current widget theme/outline preferences remain functional, accessible and contrast-safe.
- Current/next, breaks/free periods, completion and missing timetable states are accurate; countdown remains efficient and correctly labelled.
- Production support remains at Android 8+ regardless of Remote Compose feasibility.
- A technical decision record documents Remote Compose results and whether/why it shipped or remained experimental.

## Release decision

**Candidate 0.6:** deliver the first batch of **non-scrollable** canonical widgets, visual design, responsive layouts and user-feedback fixes, backed by the stable renderer. Include Remote Compose only if Phase B passes its widget-host gates. If not, keep the proof of concept and schedule another evaluation when Glance Adaptive/launcher integration provides suitable public APIs. **Only after the widgets ship and are tested should we revisit normal scrolling and optional snap paging.** This milestone is independent of the wider Feel & Flow design direction and does not require accounts or other unrelated features.

## Future investigation — scrollable agendas and snap scrolling (after the first Widget 2.0 release)

**Deferred by design decision, 14 October 2026.** Do not implement, prototype, or add scroll gestures as an initial Widget 2.0 acceptance requirement. Deliver the first batch of compact, square, wide and expanded **non-scrollable canonical layouts**, test them on real launchers, collect feedback and only then decide whether any expanded widget actually needs scrolling. A static mini-agenda may show a few useful next activities, but it must never advertise unsupported swipe actions.

If real-world testing shows the largest formats run out of useful visible room, consider two optional directions: a **Daily Agenda** that scrolls through today's remaining sessions, and a **Week/Day Overview** that can move among resolved dates. Both must reuse Tempo's schedule engine, display clear time/date/context, and preserve the compact widget's quick-glance purpose. **Do not assume either is necessary or that both should ship.** Prefer testing an ordinary vertical Glance `LazyColumn` first, then snap paging only when a genuinely page-based composition benefits.

Technical notes to revisit **later**, not during the first batch:
- Glance `VerticalScrollMode.SnapScrollMatchHeight` currently requires **Glance 1.3.0-alpha02+, compileSdk 37, Android 17+ runtime**, `SizeMode.Exact`, and a `LazyColumn` whose pages match the **widget container height**; it is not generic per-row snapping or a horizontal carousel. Check the [official snap-scroll guide](https://developer.android.com/develop/ui/compose/glance/build-ui#snap-scrolling) again when revisiting; APIs and support may change.
- `VerticalScrollMode.Normal` can provide useful non-snapping scrolling on older compatible systems; the fallback should be readable and usable rather than a broken gesture hint. Whether to keep a completely static layout may prove better on smaller formats.
- Validate launcher scroll gesture interception, tap handling, TalkBack, stable item IDs, position after periodic widget updates, resize, restarts, font scale, RTL, and power/battery cost. On supported newer hosts, test actual snap-to-full-page behaviour. Keep a route back to the primary current/next lesson.
- Make a **new go/no-go decision after the initial widget release and user testing**. Defer any additional SDK/toolchain upgrade or code spike justified *only* by scrolling until that decision.


