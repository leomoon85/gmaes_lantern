# Lantern Dispatch native edition — privacy

The native game stores campaign progress and local settings in the application's private storage on the device. It does not send that information to a server. It requests no Android permissions and includes no analytics, advertising SDK, account system, purchases, or location collection.

Historical event and price records are bundled with the app. Choosing an original-source link opens the device's browser; that external website's privacy practices then apply. The game does not embed those websites.

Cloud backup and device transfer are disabled for app data. Uninstalling the app removes its local campaign. Starting a new company asks before replacing the current native campaign. Earlier browser-edition data uses a different format and is not read by the native edition.

This document describes the code in this repository. Before publication, the store listing and data-safety declarations must describe the exact binary being distributed and provide the publisher's support contact.
