# Fareforge Android code review

Reviewed commit: `0be62f8` (native Android release candidate). Review date: 31 October 2026.

## Conclusion

The game is suitable for further development and internal testing, but should not be released publicly yet. The implementation has useful foundations: a separate Java simulation, atomic saves, attributed offline history, native controls, and a playable map fallback. The confirmed issues below affect navigation, campaign economics, objectives, and staffing. Existing tests passing does not establish production readiness.

Scope: native Java sources, manifest/resources, historical dataset and collection logic, Gradle/build/signing scripts, save codec/store, native and engine tests, privacy/release documentation, and CI template. The archived web prototype is excluded from the Android build and from these release findings. This is a code and JVM-runtime review, not a physical-device or legal/historical-source audit.

## Findings

### 1. [P1] Cancelling a new company leaves a paused screen without a menu

Location: `src/com/lantern/dispatch/MainActivity.java:963`, with the generic confirmation at line 772.

With an existing save, choose a new campaign and press the confirmation's Cancel button. `startNew()` dismisses the selection dialog while `home` remains true. The generic confirmation's negative button only dismisses the confirmation. Nothing restores the home menu or clears `home`, so simulation and saving remain disabled. Back can exit the activity rather than return to a usable menu.

Confirmed in a temporary Robolectric probe: after the negative button's dismissal, `home == true` and `modalOpen() == false`.

Fix: give this confirmation an explicit cancellation destination that restores the home menu. Treat menu navigation as a defined state transition rather than relying on dialog dismissal callbacks. Add a regression test that exercises the complete selection/cancel path and then resumes the existing company.

### 2. [P2] Modern campaigns replay the 2020 stock crash at startup

Location: `src/com/lantern/dispatch/Game.java:1896`, and historical initialization at line 738.

Every new scenario initializes `historyStage` to zero, even when its start year is 2021, 2022, or 2023. The first tick runs all elapsed historical thresholds and applies the 2020 crash to the newly initialized market. Earlier messages are overwritten by the final 2022 message, hiding why prices changed.

Confirmed: scenario index 18 starts in 2023; its first 0.1-second tick changes a rival share price from 55 to 26 and advances the history stage to four.

Fix: initialize the historical baseline for the selected start date. Apply one-time economic shocks only when the active campaign crosses the corresponding date. Define separately whether a campaign starting during lockdown begins with depressed valuations.

### 3. [P2] Passenger trips satisfy cargo objectives

Location: `src/com/lantern/dispatch/Game.java:1549`; objective checks at lines 2131, 2154, and 2184.

Every completed ride increments `cargoTons` by vehicle capacity, including commuter and executive passenger trips. Players can complete the 120/140-ton haul objectives without hauling parcels. Capacity is counted as transported cargo even though no loaded quantity exists.

Confirmed: one starter passenger trip produces `rides=1, cargoTons=2`.

Fix: define passenger and parcel quantities separately. Only eligible parcel deliveries should increase cargo tonnage, using the actual load. Clarify whether airport trips carry passengers, cargo, or both, then cover each class in objective tests.

### 4. [P2] Vehicles operate without drivers, and mergers skip assignment

Locations: `src/com/lantern/dispatch/Game.java:1468`, line 1872, and automatic dispatch at line 2058.

Dispatch checks vehicle availability without requiring an assigned driver. Missing drivers receive neutral speed and income multipliers, whereas fatigued assigned drivers receive penalties. This makes skipping recruitment advantageous. Merger-created vehicles also bypass the assignment logic used by `buyTaxi()`, leaving already recruited unassigned drivers waiting.

Confirmed: two vehicles with only one driver complete five trips when the staffed vehicle is restricted to an empty route. Separately, acquiring a four-vehicle rival leaves a previously recruited waiting driver assigned to taxi ID zero.

Fix: decide whether staffing is mandatory. If so, enforce it in the shared assignment method for both manual and automatic dispatch, assign waiting drivers after all fleet changes, and show staffing status in Fleet and Team. If vehicles are autonomous, make that an explicit era/tier unlock with different costs rather than silently treating all vehicles as autonomous.

### 5. [P2] The map toggle cannot select offline mode while a renderer is loading or failed

Location: `src/com/lantern/dispatch/WorldMapView.java:125`.

When `streets` is true, `loaded` is false, and `streetMap` exists, `toggleStreets()` retries the style and returns before changing `streets`. Repeated taps during a slow/failed connection keep retrying instead of selecting Offline. The fallback is visible, but the player cannot express a stable offline preference through this control.

Fix: make the toggle always change the selected mode. Put Retry on a separate action, stop unnecessary rendering/retries while offline, and persist the preference. Cover loading, failure, recovery, and late callbacks in map state tests. This finding follows the control flow; live MapLibre rendering was not available for runtime confirmation.

### 6. [P3] Save validation accepts states that crash normal UI operations

Location: `src/com/lantern/dispatch/SaveCodec.java:78`; model indexing at `Game.java:1233`.

Validation checks many fields but omits `businessModel` and several other values used as indices or numeric simulation state. For example, setting `businessModel=99`, encoding, and decoding succeeds; calling `modelName()` on that restored game throws `ArrayIndexOutOfBoundsException`. Company route/index state, district supply/demand array lengths, and driver identity/assignment consistency also need invariant checks.

This is a resilience gap, not evidence of an externally exploitable vulnerability: current saves are private and there is no import flow. Ordinary random damage is already covered by the checksum.

Fix: validate every persisted invariant before accepting a restored game, with explicit schema migration for future changes. Consider bounded deserialization allocation/depth protections in addition to the existing class allowlist and file-size limit.

## Enhancements in recommended order

1. **Make news change decisions.** The 14 historical event records are largely editorial; their `angle` text explicitly distinguishes them from economic changes. Apart from the fixed year-based rules, reading an event does not create an operational decision. Add clearly labelled fictional choices inspired by history: meet safety standards, change contracts, defer expansion, invest in electric vehicles, or accept a charter. Show cost, duration, expected effect, and the resulting consequence. Keep real historical facts distinct from invented consequences.
2. **Show actual campaign progress immediately.** The main mission card shows six generic growth chapters before exposing the selected campaign objective. Display the campaign target and its requirements alongside the current task. Explain hidden worth, ride, ownership, staffing, and hub requirements. Define whether end years are deadlines or settings: currently `year()` clamps at the end year while play and victory checks continue indefinitely. Resolve scenario definitions requesting seven hubs when only six exist, rather than silently clamping them.
3. **Give transport classes distinct economics.** Capacity currently contributes to cargo totals and display, but does not represent an actual passenger/load quantity. `waitPolicy` is stored but unused. Introduce explicit loads, boarding policy, class-compatible vehicles and facilities, and route utilization. Remove inactive settings until implemented so the UI describes real behavior.
4. **Improve the information hierarchy.** The floating goal card and dispatch cards compete with a fixed map toolbar, especially on small screens and with large fonts. Use a collapsible mission card and dispatch sheet; show a clear selected hub/vehicle detail panel. Test touch reachability, TalkBack, 200% fonts, landscape, and cutouts on devices. Current screenshot tests do not establish usability or prevent clipped/overlapping controls.
5. **Make geography more convincing.** All cities use the same logical road grid and interpolated geographic routes. This is explicitly documented as simulated corridors, so it is not a road-navigation bug. City-specific network geometry, recognizable crossings, directional vehicle icons, and tier-specific corridor styling would make the street map matter to play. Avoid presenting interpolated lines as real driving routes.
6. **Balance staff, maintenance, and rivals through playthroughs.** Resting a driver instantly resets fatigue without time or cost; recruiting has no ongoing payroll; rival income and request removal are periodic rather than tied to completed rival journeys. Add meaningful staffing and service downtime, predictable rival behavior, and balancing simulations across every difficulty. Present a recoverable failure state instead of endless automatic financial rescues if challenge is the intent.
7. **Improve feedback and pacing.** Show credited income rather than the gross fare as the primary dispatch reward, with the commission split available. Prefer small income animations, subtle optional haptics, and meaningful map activity. Batch routine reports and let players control news interruption frequency; a 20-simulation-second interval becomes approximately five real seconds at 4x speed when reports are queued.
8. **Strengthen saves and release automation.** Add a previous-known-good save slot, clear recovery actions, and visible last-save status. Keep production upload signing separate from the development APK. Install the supplied CI workflow in the actual workflow directory, make required checks gate releases, and distribute versioned signed artifacts through a release process rather than replacing a large APK in source history.
9. **Reduce implementation coupling.** Split the roughly 1,000-line activity into screen/navigation, presentation, and lifecycle responsibilities. Keep rewards and campaign progression in the game layer rather than mutating money directly in click handlers. Centralize staff assignment, map state, and persisted invariants to prevent manual, automatic, merger, and restore paths from diverging. Move remaining visible strings to Android resources before offering additional languages; `setLanguage()` currently stores a code without translating the UI.

## Verification and limits

- Existing Java engine and save suites passed during review.
- A temporary Java probe reproduced passenger/cargo counting, modern-campaign startup shock, unstaffed operation, merger assignment omission, and acceptance of an invalid business model.
- A temporary Android JVM probe reproduced the cancelled-new-company navigation state after allowing dialog dismissal to complete. Its assertion deliberately checks the faulty state; a passing probe confirms the bug, not a fix.
- All eight existing Android UI tests passed when rerun separately (zero failures or errors).
- Review probes are under ignored `.build/` and do not alter application source or the shipped APK. No fixes are included in this review document.
- No physical Android device or working emulator was available. GPU map rendering, actual network recovery, frame pacing, battery, memory pressure, system UI behavior, and installation/update behavior remain unverified on hardware.
- The workflow remains a template under `ci/`, not an active GitHub Actions workflow. Production signing, store declarations, prolonged gameplay testing, and device QA remain release gates.
