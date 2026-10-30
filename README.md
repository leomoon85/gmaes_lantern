# Fareforge — Mobility Tycoon

A native Android management game about turning one vehicle into a mobility company. The active game uses Java, Android Views, and **MapLibre Native with OpenFreeMap street maps**. There is no WebView or browser game. Android 8.0+ is supported; the build targets Android 16 / API 36. The original offline city stays playable when internet or the street renderer is unavailable.

The Fareforge redesign adds a midnight/amber identity, an illustrated start screen, original icon navigation, floating chapter objectives, live dispatch cards with patience indicators, and full-screen phone management panels. The application ID and native save format retain their previous identities so existing native progress can survive an update signed with the same certificate.

### Free maps, without billing setup

MapLibre Native is the on-device renderer; OpenFreeMap supplies real street data/styles based on OpenStreetMap. The provider currently offers its public instance free without accounts or API keys. No Google Cloud project, Google API key, or billing account is required. Online street maps require internet and depend on the provider's availability. Copyright attribution is retained. The game does not implement bulk offline map downloads or turn-by-turn road navigation; drawn service corridors and vehicle movement are gameplay abstractions between geographic city hubs. Use **Streets** to retry or switch map mode, and **Fit city** to reset the camera. See [OpenFreeMap](https://openfreemap.org/) and its [mobile-app instructions](https://openfreemap.org/quick_start/).

The earlier browser prototype is retained in `legacy-web/` for reference and is excluded from Android packaging. Its saved campaigns use a different format and are not migrated into the native edition. Keep the old installation/data if that progress matters.

## Play

- Start Quick Play with a city, difficulty, and ambition; choose one of 20 historical campaigns; or learn through action-gated Academy tasks.
- Tap a waiting trip or enable Auto. Vehicles collect passengers, follow roads, deliver loads, and turn around between jobs.
- Grow and maintain the fleet, select service hubs per vehicle, switch between flexible dispatch and line-only operation, and improve comfort.
- Open and upgrade hubs; build express corridors, depots, lounges, parcel terminals, and charging stations.
- Hire/train/rest partners and hire managers. Training, fatigue, maintenance, dispatch management, and service quality affect operations.
- Trade fictional rival shares, manage margin and bonds, acquire companies, and open international branches.
- Read The Mobility Post for sourced historical events, dated real share-price observations, and company alerts. Editions and management screens pause simulation; the selected speed resumes when you return to the city.

The fallback map is an original offline schematic. Online maps use real geographic hub locations; the game economy, rivals, and operating corridors are fictional. UI text is English; the earlier prototype's language selector is not represented as native localization.

## Install a development build

`dist/Fareforge.apk` is the locally signed development APK. It retains the package and local development signing key from the earlier native edition. It is suitable for installation and testing, not a Play production upload.

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

## Build an installable development APK

`build.ps1` uses Gradle so MapLibre and its Android dependencies are packaged correctly. It runs the engine/save tests and lint, builds an optimized release APK, then signs it with the local development key. Use JDK 17, platform 36, and build-tools 35.0.0. Initial dependency resolution requires internet; use `-Offline` after dependencies are cached.

```powershell
.\build.ps1 -Sdk 'C:\path\to\Android\Sdk' -Java 'C:\path\to\jdk-17'
.\verify-native.ps1 -Apk '.\dist\Fareforge.apk'
```

`verify-native.ps1` rejects APKs containing HTML/JS/CSS assets or WebView runtime references. It also validates the historical dataset packaged in the APK.

## Architecture and saves

`Game.java` is the platform-independent simulation. `MainActivity` owns the foreground-only frame loop and Android lifecycle, `CityMapView` draws the city, and `GamePanels` builds native management controls. `NativeNews` reads the bundled, attributed historical dataset.

`SaveCodec` stores a versioned, size-bounded, checksummed snapshot with a restricted type allowlist and state validation. `SaveStore` writes using Android `AtomicFile` with rollback. The game saves on actions, every ten seconds while playing, and on backgrounding. In-progress trips, corridors, shares, staff, chapters, and the news archive persist. Rotation uses a Bundle snapshot, and save writes run on one ordered background executor. No simulation runs while the game is backgrounded.

## Validation and release status

This is a **native release candidate**, not a certified store release. See [RELEASE.md](RELEASE.md) for validation results and remaining gates. Tests include simulation/accounting scenarios, all campaign objective checks, six-city save roundtrips, corrupted-save rejection, and Android UI/lifecycle tests using Robolectric. Runtime Android test previews, when generated, live in `build/native-previews/` and are not physical-device screenshots.

Street maps use normal network permissions, with no GPS/location permission. There are no ads, purchases, or analytics. Map requests go to the map provider; campaign state stays on the device. Source links open the user's browser. See [PRIVACY.md](PRIVACY.md).
