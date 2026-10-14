# Remote Compose alpha21 local-player lab

Build separately, with Tempo's unchanged SDK 36 / AGP / Kotlin:

```sh
./gradlew -PtempoRemoteCompose=true :experiments:remote-compose:assembleDebug
adb install experiments/remote-compose/build/outputs/apk/debug/remote-compose-debug.apk
adb shell am start -n cc.xdan.tempo.remoteexperiment/cc.xdan.tempo.remote.RemoteComposeLabActivity
```

This APK is excluded from normal project configuration and the production APK.
It uses low-level native document paths and text, the same pure corner-rule source
as the widget (copied at build time), system palette colours on Android 12+, two
sample sizes and event-state changes. Tap the document or the state button to
switch state. Time is **static**, not a proven Remote Compose countdown. The
profile is explicitly the **embedded AndroidX player** profile. It is not a
launcher profile. No private framework APIs or widget receivers are used.

The alpha21 `RemoteComposePlayer` View class and its action listener are library-group restricted; the suppression
is explicit and confined to this experiment. Even a successful local-player test
cannot satisfy the public-API production gate. Material shape tokens are ordinary
path geometry here, not a claim of a native Material widget component library.
Subject-catalogue icons, responsive host expressions, accessibility, particle
transitions, automatic dynamic-colour updates and live countdowns still require
further experiments. In particular, recreating documents at two sizes tests
serialization; it does not prove host-driven adaptive sizing.

AAR metadata and build results are recorded in
[the implementation log](../../docs/WIDGET_2_0_IMPLEMENTATION.md).
