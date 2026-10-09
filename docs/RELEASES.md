# Signed releases

Release APKs and app bundles use the application ID `cc.xdan.tempo`, R8 code
shrinking/optimization/obfuscation, and optimized resource shrinking. Normal CI
checks both debug and unsigned release variants without signing secrets.

## One-time signing setup

Use a dedicated Tempo release keystore. Keep an offline backup of the keystore,
alias and passwords: future APK updates must use the same signing key.
If you need a new key, create it locally with JDK `keytool` (passwords are prompted):

```sh
keytool -genkeypair -v -keystore tempo-release.jks -alias tempo \
  -keyalg RSA -keysize 4096 -validity 10000
```

In GitHub **Settings → Secrets and variables → Actions → New repository secret**,
add these four secrets:

| Secret | Value |
| --- | --- |
| `TEMPO_KEYSTORE_BASE64` | Base64 encoding of the whole `.jks` file |
| `TEMPO_KEYSTORE_PASSWORD` | Keystore password |
| `TEMPO_KEY_ALIAS` | Key alias, e.g. `tempo` |
| `TEMPO_KEY_PASSWORD` | Password for that private key |

Generate the Base64 value locally; do not commit it or paste it into issues:

```sh
python3 -c 'import base64,pathlib; print(base64.b64encode(pathlib.Path("tempo-release.jks").read_bytes()).decode())'
```

## Build and download

After merging the workflow, open **Actions → Signed release → Run workflow** and
select `main`. Other branches are skipped. Missing secrets fail explicitly;
the workflow never substitutes a debug key. Download `Tempo-release-<run number>`
from the completed run for the signed APK, signed AAB, and R8 mapping/reports.
Keep the mapping with each distributed version for crash deobfuscation.
This workflow builds artifacts; it does not publish to Google Play or create a
GitHub Release.

Increment `versionCode` in `app/build.gradle.kts` for each distributed update,
and update `versionName` when changing the displayed release version. Test the
minified APK on a device using [the device checklist](DEVICE_TESTING.md), including
creating a timetable, restarting the app, and restoring the saved data.

Existing CI debug APKs have a different certificate, so moving from a debug build
to the first signed release requires uninstalling it (which deletes its local
data). Subsequent releases signed with the same key can update in place.

## Local release builds

Set `TEMPO_KEYSTORE_PATH` to the absolute keystore path and set the three password/
alias variables above in your shell, then run:

```sh
./gradlew :app:assembleRelease :app:bundleRelease
```

With none of these environment variables set, release builds are unsigned for CI
validation. A partial signing configuration is rejected. Never commit keystores,
passwords, or signing properties.
