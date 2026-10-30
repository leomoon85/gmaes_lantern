# Lantern Dispatch — Native Android

An offline Android management game about turning one vehicle into a mobility company. The active game uses **Java, Android Views, and a Canvas-rendered city**, with no WebView, JavaScript runtime, remote map tiles, or third-party runtime libraries. Android 8.0+ is supported; the build targets Android 16 / API 36.

The earlier browser prototype is retained in `legacy-web/` for reference and is excluded from Android packaging. Its saved campaigns use a different format and are not migrated into the native edition. Keep the old installation/data if that progress matters.

## Play

- Start Quick Play with a city, difficulty, and ambition; choose one of 20 historical campaigns; or learn through action-gated Academy tasks.
- Tap a waiting trip or enable Auto. Vehicles collect passengers, follow roads, deliver loads, and turn around between jobs.
- Grow and maintain the fleet, select service hubs per vehicle, switch between flexible dispatch and line-only operation, and improve comfort.
- Open and upgrade hubs; build express corridors, depots, lounges, parcel terminals, and charging stations.
- Hire/train/rest partners and hire managers. Training, fatigue, maintenance, dispatch management, and service quality affect operations.
- Trade fictional rival shares, manage margin and bonds, acquire companies, and open international branches.
- Read the native Lantern Ledger for sourced historical events, dated real share-price observations, and company alerts. Editions and management screens pause simulation; the selected speed resumes when you return to the city.

The city maps are original **offline schematics**, not geographic street maps. Real place names provide context. Game money and rival companies are fictional. UI text is currently English; the earlier prototype's language selector is not represented as native localization.

## Install a development build

`dist/Lantern-Dispatch.apk` is the locally signed native development APK. It retains the package and development signing key used by the earlier manual build, when that local key is available. It is suitable for local installation and testing, not a Play production upload.

The Gradle debug APK has a separate application ID (`com.lantern.dispatch.dev`) so it can coexist with the development-signed manual build.

## Build in Android Studio / Gradle

Open this directory in Android Studio. Install JDK 17 and Android platform 36. The checked-in Gradle 8.11.1 wrapper verifies its distribution checksum. Android Gradle Plugin 8.10.1 and test dependencies download on first use.

```powershell
.\gradlew.bat engineTest testDebugUnitTest lintDebug assembleDebug
.\gradlew.bat assembleRelease bundleRelease
```

Outputs are under `build/outputs/`. Release builds enable code/resource shrinking. Without signing environment variables, the release APK and AAB are unsigned.

For store signing, provide your own upload key through environment variables; never commit the key or passwords:

```powershell
$env:LANTERN_KEYSTORE = 'C:\private\upload-key.jks'
$env:LANTERN_KEY_ALIAS = 'upload'
# Set LANTERN_STORE_PASSWORD and LANTERN_KEY_PASSWORD securely in the environment.
.\gradlew.bat bundleRelease
```

The package name, version code, signing identity, and any existing Play listing must be reviewed before upload. The bundled development certificate is not a production credential.

## Build without Gradle downloads

The manual build uses installed JDK 17, platform 36, and build-tools 35.0.0. It runs Java engine/save tests, packages **only `native-assets/`**, compiles native sources, aligns, signs with the local development key, and verifies the APK:

```powershell
.\build.ps1 -Sdk 'C:\path\to\Android\Sdk' -Java 'C:\path\to\jdk-17'
.\verify-native.ps1 -Apk '.\dist\Lantern-Dispatch.apk'
```

`verify-native.ps1` rejects APKs containing HTML/JS/CSS assets or WebView runtime references. It also validates the historical dataset packaged in the APK.

## Architecture and saves

`Game.java` is the platform-independent simulation. `MainActivity` owns the foreground-only frame loop and Android lifecycle, `CityMapView` draws the city, and `GamePanels` builds native management controls. `NativeNews` reads the bundled, attributed historical dataset.

`SaveCodec` stores a versioned, size-bounded, checksummed snapshot with a restricted type allowlist and state validation. `SaveStore` writes using Android `AtomicFile` with rollback. The game saves on actions, every ten seconds while playing, and on backgrounding. In-progress trips, corridors, shares, staff, chapters, and the news archive persist. Rotation uses a Bundle snapshot, and save writes run on one ordered background executor. No simulation runs while the game is backgrounded.

## Validation and release status

This is a **native release candidate**, not a certified store release. See [RELEASE.md](RELEASE.md) for validation results and remaining gates. Tests include simulation/accounting scenarios, all campaign objective checks, six-city save roundtrips, corrupted-save rejection, and Android UI/lifecycle tests using Robolectric. Runtime Android test previews, when generated, live in `build/native-previews/` and are not physical-device screenshots.

The app requests no Android permissions, includes no analytics/ads/purchases, and makes no network requests itself. Source links open the user's browser. See [PRIVACY.md](PRIVACY.md).
