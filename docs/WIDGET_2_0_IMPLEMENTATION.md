# Widget 2.0 implementation log

First pass: 14 October 2026. Base: latest `origin/main`; branch `feature/widget-2.0`.

## Audit

All four planning documents were read before choosing a renderer. `WIDGET_2_0.md`
is authoritative; connected experiences are future context only.

- `PillWidget`, `SquareWidget`, `WideWidget` are installed component identities.
  All already allow horizontal/vertical resize. Keep every receiver and ID.
- `WidgetSizing` reads Android 12 size lists (bounded to 16) and legacy portrait /
  landscape option pairs, with font-scale density reduction. The renderer still
  chooses most content by provider identity, so expanding a pill cannot reveal
  the wide composition. This is the architectural seam to replace.
- XML contains native Chronometers; `WidgetArtwork` rasterises catalogue vectors.
  Colours use shared palettes / dynamic schemes and contrast-selected foregrounds.
  Fill, outline, icon, location, completion and timetable preferences live in the
  existing per-ID `widgets` SharedPreferences. No settings reset is needed.
- Configuration saves immediately and on leaving. Option previews are schematic
  Canvas drawings, not actual RemoteViews; compact previews need replacement.
- `weekOverview`, `widgetDayComplete`, `widgetDayPlan` already implement week bounds,
  date exceptions and agenda rules. Presentation must consume them, not reimplement
  cycles, holidays or recurrence.
- WorkManager handles edits, recovery and 15-minute periodic work; inexact boundary
  alarms and boot/time/zone/package receivers restore updates. Native monotonic
  countdowns must remain; no new polling or animation work.
- Existing tests cover sizing, agenda/overlaps, locations, contrast and core dated
  schedules. Host inflation, resizing and TalkBack need device-level checks.

## Official API research and decision

**Decision: defer production Remote Compose; retain RemoteViews and Chronometer.**
This is a first-pass decision, subject to real-host evidence, not a claim that
Remote Compose cannot render on any Android launcher.

Rechecked official sources:

- [Remote Compose alpha21](https://developer.android.com/jetpack/androidx/releases/compose-remote):
  document creation, player, paths, text, colours and state APIs exist. July's
  alpha14 removed a direct SDK 37 requirement, but the alpha21 Compose authoring
  POM pulls Compose 1.11.0. Check the entire graph, not just its own AAR metadata.
- [Glance](https://developer.android.com/jetpack/androidx/releases/glance): 1.2.0
  stable translates to RemoteViews; 1.3 alpha notes require SDK 37 / AGP 9.2.
- [Glance Adaptive alpha03](https://developer.android.com/jetpack/androidx/releases/glance-adaptive):
  public APIs are still forthcoming; alpha02 notes SDK 37.1 transitive requirements.
  A template-first library announcement is not a usable public Tempo host contract.
- [Glance interop](https://developer.android.com/develop/ui/compose/glance/interoperability):
  `AndroidRemoteViews` can retain XML including native countdowns. A migration to
  stable Glance is possible but adds no necessary capability for this first pass.
- [Canonical widgets](https://developer.android.com/design/ui/mobile/guides/widgets/layouts)
  and [sizing](https://developer.android.com/design/ui/mobile/guides/widgets/sizing):
  use a single entity at compact sizes and reveal supplementary content with room.
- [Responsive RemoteViews](https://developer.android.com/develop/ui/views/appwidgets/layouts):
  Android 12 size maps and pre-12 orientation pairs are suitable for the fallback.
- [Material shapes](https://m3.material.io/styles/shape/corner-radius-scale):
  named corner tokens; mirrored connected surfaces share smaller inward corners.
- [Particle transition API](https://developer.android.com/reference/kotlin/androidx/compose/remote/creation/compose/modifier/RemoteEnterTransition):
  an element enter/exit transition does not establish a falling confetti emitter.

The isolated prototype will use low-level document creation and the View player
so it need not introduce Compose 1.11 into the production graph. An in-app player
is explicitly **not** evidence of launcher support. No production renderer switch
or particle animation is justified without host, countdown and accessibility gates.

## Progressive disclosure contract

| Usable dimensions (dp adjusted for font scale) | Composition | First pass |
| --- | --- | --- |
| Short (<136 high) | Compact subject + labelled countdown | New renderer |
| Narrow (<280 wide), 136+ high | Visual entity | Existing fallback, new classification |
| Wide (280+), 136+ high | Hero + related event | Existing fallback, new classification |
| Tall narrow (300+ high) / wide (230+ high) | Non-scrollable agenda | Existing agenda, redesign next |

Thresholds are initial content budgets, not measured launcher guarantees or cell
counts. Compact density must adapt below these budgets and with large fonts.
Keep legacy picker entries until upgrade/device testing supports consolidation.
