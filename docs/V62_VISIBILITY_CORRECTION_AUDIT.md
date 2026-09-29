# V62 Visibility Correction Audit

## Issue
The V62 Compose compiler-memory refactor moved several top-level composables from `UniversalRemoteApp.kt` to `RemoteControlScreen.kt`, but five declarations remained `private` while their caller remained in `UniversalRemoteApp.kt`.

Affected functions:
- `RemoteControlHeader`
- `RemoteControlMainPanel`
- `RemoteControlBottomControls`
- `MultiTvSwitcher`
- `ModeSwitcher`

Kotlin top-level `private` declarations are file-private, so the split introduced unresolved references across the two files.

## Correction
The five declarations are now package-visible by removing `private`. No function body or runtime behavior was changed.

## Pre-CI checks
- Project verifier: PASS (`Project invariants: OK`)
- `UniversalRemoteApp.kt` delimiter balance: PASS
- `RemoteControlScreen.kt` delimiter balance: PASS
- Heuristic cross-file scan for private top-level UI functions: PASS
- Version identity remains V62 / `versionName = "62"` / `versionCode = 62`

## Build limitation
This environment does not contain a usable Gradle executable or Gradle wrapper JAR. Therefore a real Kotlin/Gradle compile was not performed here. GitHub Actions remains the authoritative compile verification.
