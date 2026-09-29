> Historical document. It is retained for traceability and must not override README.md, ARCHITECTURE.md, IMPLEMENTATION_STATUS.md, CURRENT_STATUS.md, or SECURITY.md.

# V31.1 Engineering Audit

## Applied fixes
- Removed the duplicate `ProtocolRuntime` declaration from `ProtocolManagerFactory.kt`.
- Kept `ProtocolRuntime.kt` as the single canonical runtime type and expanded it to own both `ProtocolManager` and `ProtocolBindingRegistry`.
- Hardened remote protocol repository wiring: a remote repository is enabled only when both an HTTPS endpoint and a trusted public key are configured.
- Added architecture regression tests for shared Android TV protocol identity and built-in binding availability.
- Bumped Android version metadata to `versionCode 32` / `versionName 0.31.1`.

## Preserved subsystems
The Android TV, discovery, pairing, AI, voice, IR, Bluetooth, protocol, security, diagnostics and integration-test source trees were retained. No broad deletion was performed.

## Remaining limitation requiring a real Android build
`gradle/wrapper/gradle-wrapper.jar` is still absent from the uploaded project and no system Gradle is available in this environment, so `assembleDebug` could not be executed here. This is a build-artifact/bootstrap issue, not something that should be solved by inventing a wrapper JAR.

## Architectural note
`RemoteViewModel` still uses the legacy `TvTransportRegistry` for command dispatch. It was not removed blindly because the current `ProtocolBindingRegistry` is declarative and does not yet contain concrete command adapters for every legacy vendor transport. Removing it now would regress Samsung/LG/Sony/Philips/Roku command support. The safe next refactor is to migrate each concrete transport behind `ConnectionAdapter`/binding interfaces, then retire `TvTransportRegistry` after parity tests pass.
