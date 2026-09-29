# V62 CI Compile Audit

## Source of correction
The V61 GitHub Actions build reached Kotlin IR/JVM code generation and failed in `ui/UniversalRemoteApp.kt` with:

`java.lang.OutOfMemoryError: GC overhead limit exceeded`

The failing generated function was `RemoteControlScreen$lambda$108`. The log therefore identifies compiler-memory pressure from the large nested Compose function rather than a syntax or unresolved-reference error.

## Structural correction
The complete remote-control UI was extracted to `ui/RemoteControlScreen.kt` and decomposed into smaller composables:

- `RemoteControlHeader`
- `RemoteControlMainPanel`
- `RemoteControlBottomControls`
- `OneHandControls`
- `StandardBottomControls`
- `MultiTvSwitcher`
- `ModeSwitcher`
- `Dpad`
- `GesturePad`
- `KeyboardPad`
- `NumberPad`

`UniversalRemoteApp.kt` now contains only the navigation-shell entry point for the remote screen. This reduces the size and capture graph of the function that Kotlin must lower and generate.

## Secondary compiler-memory protection
`gradle.properties` now provides:

- `org.gradle.jvmargs=-Xmx2048m -Dfile.encoding=UTF-8`
- `kotlin.daemon.jvmargs=-Xmx2048m`

These settings are defensive. They do not replace the structural decomposition.

## Version identity
- Human-facing release: **V62**
- Android `versionName`: **"62"**
- Android `versionCode`: **62**

## Pre-CI verification
- Version identifiers checked for exact equality.
- Kotlin source braces checked for balance.
- No duplicate `RemoteControlScreen` declaration.
- Extracted helper symbols checked for duplicate declarations.
- Project verifier run after the edits.
- ZIP integrity checked after packaging.
- Full Gradle compilation remains authoritative in GitHub Actions if the local environment does not contain a usable Gradle distribution.
