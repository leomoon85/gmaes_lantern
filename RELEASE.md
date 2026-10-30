# Native Android release candidate

Fareforge version `1.0.0-rc2`, code `6`. Application ID `com.lantern.dispatch`; debug builds append `.dev`. Minimum API 26, target/compile API 36. This is a native Android game with MapLibre/OpenFreeMap street maps and an offline schematic fallback. The package identity stays unchanged to support native save/update compatibility.

## Release build

`gradlew assembleRelease bundleRelease` produces an optimized release APK and AAB. Upload signing is configured only when `LANTERN_KEYSTORE`, `LANTERN_KEY_ALIAS`, `LANTERN_STORE_PASSWORD`, and `LANTERN_KEY_PASSWORD` are provided. Unsigned output is intentional when those credentials are absent. The local manual-build APK is development-signed.

## Required before a store release

- Run the actual APK on representative Android 8, Android 15/16, small phones, and tablets. Verify gestures, font scaling/TalkBack, system bars, rotation, multitasking, pause/resume, and force-stop/save recovery.
- Complete prolonged economy/playthrough and performance/battery testing. JVM tests do not measure real device rendering, memory pressure, or thermal behavior.
- Sign with the publisher's upload identity, verify application ID/version against the intended listing, and test the update path. Browser-edition saves do not migrate into native campaigns.
- Supply final store art, support/privacy URLs, content rating, and data-safety declarations for the final signed binary. Clear the provisional game/company identity and review the historical copy.

## Verification available in this repository

Fareforge validation: engine/save suites and eight Android JVM tests passed; optimized release APK and AAB builds passed; Android lint has zero errors and two build-tool update notices. Both 64-bit MapLibre native libraries have 16 KB ELF load alignment. Live OpenFreeMap style endpoints were checked, but actual GPU-backed street rendering cannot be exercised on this host: the Android emulator lacks a hypervisor driver. JVM UI previews show the offline fallback, not verified live street tiles. Production signing and physical-device map/lifecycle testing remain release gates. CI is supplied as a template because the available token cannot create workflow files.

- `engineTest`: routing, dispatch, accounting, share/margin operations, debt, models, all campaign objectives, route policies, Academy gating, and save corruption/roundtrip checks.
- `testDebugUnitTest`: Android Views, offline Canvas drawing, navigation, newspaper pause/resume, historical date/region filters, and atomic native save operations, using Robolectric.
- `lintDebug` and release lint run with errors blocking the build. No error baseline hides findings.
- `verify-native.ps1`: verifies that the APK contains the native dataset and no browser assets or WebView references.
- The CI template builds debug/release APKs and an unsigned release bundle, runs tests/lint, and retains build/test reports. To enable it, a repository owner must install `ci/android-workflow.yml` as `.github/workflows/android.yml` using an account/token authorized to update workflows.

The host used for this work has no connected Android device and no emulator hypervisor driver. An API 35 emulator image was installed, but software emulation exited before boot. Physical-device verification remains a release gate; it is not reported as passing.
