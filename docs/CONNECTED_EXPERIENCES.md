# Connected experiences — Wear OS and Beam my Schedule

**Status:** future proposals to investigate **after the first Widget 2.0 batch**. Documented 14 October 2026. This document does not commit these features to Tempo 0.6 or claim that experimental Remote Compose APIs work across every host.

Tempo's long-term goal is to make a real resolved schedule useful in more contexts. Reuse the existing schedule resolver and portable timetable semantics; do **not** couple the data model to a particular remote renderer.

## Shared architecture and constraints

- Derive a small **read-only schedule presentation snapshot** using the shared date/cycle/holiday/exception resolver. Include explicit timestamp, time zone where needed, selected timetable, event type and current/next boundaries; avoid inferring attendance from the clock.
- Keep the snapshot independent from UI delivery. Phone widgets, Wear Tiles, future Wear Widgets, nearby previews and the main Compose app should remain able to render it in their platform-appropriate way.
- Exclude private information by default. Only include locations, names, notes and date ranges when appropriate for that surface and the user's explicit selections.
- Avoid duplicating recurrence logic, requiring sign-in or background cloud syncing for essential offline use. Resolve schedules locally as appropriate.
- Remote Compose is an **optional rendering/document format**, not the sole source of timetable truth. Validate API availability, host capabilities, resource limits and backward compatibility before committing to it.

## Wear OS — proposed first post-widget companion milestone

**User need:** quickly see the current/next lesson, shift or appointment without taking out a phone.

### First useful experience

- [ ] Scope a minimal **Wear OS Tile** backed by the existing schedule model: current/next subject or shift, state ("Now", "Starts in", "Break", "Done today"), readable time, subject colour/icon and optional room/location, when enough space is available.
- [ ] Support missing timetable, no schedule, day off, disconnected phone and stale snapshot states. Show **last updated** rather than implying data is always fresh; don't display an inaccurate live countdown when no valid time base is available.
- [ ] Resolve dates and clock changes, holidays, A/B cycles, replacements and non-school schedules consistently with the phone.
- [ ] Make tap actions open a focused watch detail screen or the companion app only if those destinations exist. Avoid inert or tiny hit targets; check rotary/D-pad, TalkBack, large text and ambient/low-power states.
- [ ] Use on-device data where practical. For a paired-phone-first design, synchronise a small, versioned snapshot via the **Wear OS Data Layer API**, with reconnect/clock-change recovery and sensible offline behaviour. Transport is not identical to rendering: no need to send the phone's Compose UI.
- [ ] Provide a clear paired/standalone compatibility matrix, supported-watch versions and installation expectations. Test on actual hardware as well as emulators.

### Rendering decision

1. **Baseline:** evaluate the documented **Wear OS Tiles / ProtoLayout** architecture and Material 3 components for supported Wear devices. It offers a practical, purpose-built Tile experience without assuming experimental host integrations.
2. **Experimental:** independently evaluate **Glance Wear Widgets + Remote Compose** on their documented newer Wear OS renderers/SDK requirements. An in-app Remote Compose player does not prove the watch widget host supports a particular document.
3. Only share drawing/style tokens where practical. **Wear UI must adapt to round displays, safe insets, glanceability and watch accessibility** rather than replaying a phone's widget document indiscriminately.
4. **Watch-face complications are separate** from Tiles/Wear Widgets; consider a compact "next event" complication only after the basic watch surface works reliably. A Tile does not automatically appear directly on the watch face.
5. Keep experimental API/toolchain dependencies out of the production phone widget baseline until tested and maintain an independent compatibility path.

### Wear OS acceptance gate

- [ ] The user can see what's next, when it happens and optionally where, in a glance, without needing the phone screen.
- [ ] Current/next, free/break, completed and exception states match the Android phone resolver across midnight and zone changes.
- [ ] No repeated per-second phone wakeups or unnecessary reconnect polling. Updates and disconnections are observable and recover correctly.
- [ ] Correct watch shape, contrast, text size and accessibility on small/large round devices; usable fallback when disconnected.
- [ ] Document whether this is a separate watch APK/module and which features require a paired Android phone.

**Priority:** high after Widget 2.0, but not a prerequisite for Tempo 1.0.

## Beam my Schedule — later privacy-first nearby preview experiment

**User need:** two people sitting nearby can compare plans without creating accounts, sharing files manually or permanently importing someone else's timetable.

### Candidate interaction

1. **Sender:** open "Beam my Schedule", choose an existing timetable and specific dates/week/cycle preview. Review the exact disclosed fields (subject names, times, notes, locations) with **locations/notes hidden by default**. Explicitly approve sending.
2. **Nearby discovery and handshake:** try a well-supported nearby transport such as **Nearby Connections** (Google Play services dependency), which can use Bluetooth and local Wi-Fi, with both users verifying the other device/code and accepting the connection. Investigate a reasonable platform-compatible alternate share method rather than assuming every Android device has Google Play services.
3. **Payload:** derive a **bounded, sanitised preview snapshot**. Experimental track: serialise a Remote Compose display document for a compatible **player** to render inside Tempo; measure actual byte size, compression, version negotiation and whether local interactions are portable. Do not promise it will always be "tiny" or fit a particular transport payload type; choose byte/file/stream transport from measured size and API limits.
4. **Receiver:** explicitly consent to view a preview. Validate document envelope/version, supported operation profile, content size/complexity, resource limits, and renderer capabilities before decoding/rendering a **temporary sandboxed, read-only** view; do not write the foreign timetable to the user's existing local collection.
5. **End:** let either user dismiss, disconnect or revoke the session. Clear temporary data after the session; do not persist friend timetable metadata accidentally through previews/logging/analytics. Import remains a separate, deliberate action using Tempo's existing verified portable data format.

### Hard technical distinctions

- **Remote Compose is not a peer-to-peer connection protocol**. Transport (Bluetooth/local Wi-Fi), schema verification, consent and UI rendering are independent.
- A renderable document still needs to be **parsed/validated by a player**. The advantage is avoiding an import-and-merge workflow, not eliminating untrusted input handling.
- Captured Remote Compose documents and certain sender-side action metadata **must not be treated as portable executable callbacks**; only allow an explicit, safe receiver-local interaction set, e.g. paging through the preview or closing it.
- Do not send full local database envelopes or private timetable IDs by default. Encrypt/authenticate the connection through the chosen transport and require mutual confirmation when possible.
- If Remote Compose sharing proves incompatible, too big, non-portable or unsafe, consider a **small versioned JSON/CBOR-like preview DTO rendered locally**, or a QR/link/file-based read-only preview. Choose the simplest maintainable format; keep the same user value.
- A visual preview alone does not enable genuine **free-time intersection**. Only add optional schedule comparison when both users explicitly opt to exchange the minimal structured fields required. Keep a visual-preview-only mode available.

### Beam go/no-go criteria

- [ ] Two devices discover/connect with explicit consent, exchange a reasonable payload in practice, render a full timetable/date preview, and disconnect cleanly without internet or account where the chosen transport supports it.
- [ ] Profiles/data can't leak private notes/locations by default; the receiver cannot silently import or trigger arbitrary sender-defined actions.
- [ ] Validate payload size, hostile/unsupported documents, throttling/timeouts, device rotation, background interruption and failure recovery.
- [ ] Compare UX with ordinary .tempo.json transfer or QR/link sharing. Only ship Beam if it is **meaningfully simpler** for nearby comparison.
- [ ] Revisit the Remote Compose capture/player API stability before implementation; do not embed alpha experimental protocol requirements into the stable portable timetable format.

**Priority:** medium / research-stage after Widget 2.0, and ideally after the highest-value Wear OS experience. Not a blocker for ordinary sharing or future creator profiles.

## Android Auto — deliberately deferred

Android Auto has driver-distraction restrictions and application category/template limitations. Tempo's general timetable/work-shift UI should **not** be assumed eligible for Android Auto merely because Remote Compose can express responsive layouts. Do not promise a car dashboard app or build a bespoke car UI until the officially supported app categories and approval path fit this use case. Investigate calendar/ICS integration with existing supported apps first. **Priority: low / research only**, no implementation milestone.

## Order and dependencies

1. Finish the **non-scrollable, adaptive phone Widget 2.0** batch and decide if Remote Compose is genuinely usable on home screens.
2. Start a **Wear OS Tile proof of concept**, testing pairing/sync/offline support. Evaluate newer Remote Compose Wear Widgets separately.
3. Prototype **Beam my Schedule** with explicit privacy controls and measured payload/player compatibility, then decide whether a full feature is warranted.
4. Review Android Auto eligibility only if platform policy makes it relevant.

## Official references (recheck versions at implementation time)

- [Remote Compose releases](https://developer.android.com/jetpack/androidx/releases/compose-remote)
- [Glance releases](https://developer.android.com/jetpack/androidx/releases/glance)
- [Wear OS Tiles](https://developer.android.com/training/wearables/tiles)
- [Wear OS Data Layer](https://developer.android.com/training/wearables/data/overview)
- [Nearby Connections](https://developers.google.com/nearby/connections/overview)
- [Android for Cars app categories](https://developer.android.com/training/cars/apps)
