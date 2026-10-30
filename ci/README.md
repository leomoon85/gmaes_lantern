# Android CI template

`android-workflow.yml` is ready to install as `.github/workflows/android.yml`. It runs native engine tests, Robolectric UI tests, lint, and debug/release builds, retaining the artifacts and reports.

The GitHub token available during this work lacks `workflow` permission. The template is kept here so the native game can be pushed without changing account permissions. A repository owner can enable the workflow through GitHub's UI or a credential authorized to update workflows. No remote CI run is claimed.
