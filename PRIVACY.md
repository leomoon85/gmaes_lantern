# Fareforge — privacy

The native game stores campaign progress and settings in the application's private storage. Company finances, trips, and saves are not uploaded. The app includes no advertising SDK, accounts, purchases, or analytics service. It does not request device location permission or use the device's GPS location.

Street maps use MapLibre Native and OpenFreeMap. The app requests the normal INTERNET and ACCESS_NETWORK_STATE permissions. When online street maps are used, the provider receives ordinary network requests, including the requested map area and network metadata such as IP address. Styles and fonts may also be requested from the map provider. Attribution remains visible in the native map. See [OpenFreeMap](https://openfreemap.org/) and [MapLibre](https://maplibre.org/). The original offline city remains playable if online maps cannot load. No API key, billing account, or player registration is needed for the selected map provider.

Historical event and price records are bundled with the app. Choosing an original-source link opens the device's browser; that external website's privacy practices then apply. The game does not embed those websites.

Cloud backup and device transfer are disabled for app data. Uninstalling the app removes its local campaign. Starting a new company asks before replacing the current native campaign. Earlier browser-edition data uses a different format and is not read by the native edition.

This document describes the code in this repository. Before publication, the store listing and data-safety declarations must describe the exact binary being distributed and provide the publisher's support contact.
