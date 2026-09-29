# V63 — Private App / Public Protocol Repository Separation

- Kept the Android application repository private and removed its dependency on GitHub Pages.
- Replaced the private-repository Pages job with a verified cross-repository publication job targeting `hadihaddadian/universal-tv-remote-protocols`.
- Restricted the private workflow to `contents: read`; cross-repository publication uses only the `PROTOCOL_REPOSITORY_TOKEN` Actions secret.
- Added a Pages deployment workflow to the generated public protocol repository using the current GitHub Pages artifact/deploy actions.
- Added an empty-target-repository bootstrap path and a deterministic synchronization step so the public repository contains only the generated protocol distribution.
- Corrected the stale debug APK artifact name to `universal-tv-remote-v63-debug`.
- Unified release identity at V63 / `versionName = "63"` / `versionCode = 63`.

# V62 — JVM Unit-Test Failure Correction

- Corrected the V62 test-suite failures exposed after production compilation succeeded.
- Added a concrete JVM `org.json` test dependency so protocol-package JSON tests do not execute against Android framework stubs.
- Centralized the protocol command-to-capability contract and added all catalogued commands, including playback, input, number-pad, and guide commands.
- Updated the protocol validator and consistency tests to use the same canonical command-capability mapping.
- Corrected Android TV connection orchestration so an `android_tv_remote_v2` fingerprint enters the explicit pairing stage before connection.
- Kept the release identity unified at V62 / `versionName = "62"` / `versionCode = 62`.

# V62 — Compose Compiler Memory Hardening

- Promoted the active release to **V62** with unified identity: human-facing **V62**, Android `versionName = "62"`, Android `versionCode = 62`.
- Decomposed the large `RemoteControlScreen` Compose tree from `UniversalRemoteApp.kt` into `ui/RemoteControlScreen.kt`.
- Split remote UI generation into smaller composables for the header, main control panel, one-hand controls, standard controls, TV switcher, mode switcher, D-pad, gesture pad, keyboard pad and number pad.
- This directly targets the V61 CI failure where Kotlin IR/JVM code generation exhausted compiler memory with `OutOfMemoryError: GC overhead limit exceeded` in `RemoteControlScreen$lambda$108`.
- Added a conservative Kotlin/Gradle heap configuration as a secondary protection; structural decomposition remains the primary fix.

# V61 — CI Compile Regression Correction

- Promoted the correction release to **V61** so the failed V60 artifact is not silently overwritten.
- Unified release identity: human-facing **V61**, Android `versionName = "61"`, Android `versionCode = 61`.
- Fixed the missing closing scopes in `ui/UniversalRemoteApp.kt` introduced by the V60 one-hand layout change.
- This specifically resolves the CI cascade beginning with unresolved `ConnectionErrorScreen`/`HomeScreen` and the subsequent `private is not applicable to local function` and `Expecting '}'` errors.

# V60 — Unified Versioning & Feature Completion

- Unified release number: **V60**. Human-facing release, Android `versionName`, and Android `versionCode` are all `60`.
- Removed the previous `V59 / versionName 59 / versionCode 60` split to eliminate ambiguous release references.
- Voice screen now uses the real Android TV voice transport already implemented in the project, including runtime microphone permission handling and active-session state.
- One-hand mode now has a concrete UI effect by constraining the active content width and aligning it to the screen edge.
- Favorite TVs are surfaced first in device lists while retaining deterministic alphabetical ordering.
- Voice UI no longer claims that the transport is “coming soon”; it reflects the actual implemented transport and its capability gate.

# V59 — CI Compile Regression Fixes

- Fixed all compile errors reported by the V58 GitHub Actions Kotlin build.
- Added the required `NsdManager.DiscoveryListener.onServiceLost` implementation.
- Restored the missing public `AtvKey` enum used by the Android TV Remote v2 facade.
- Corrected PEM private-key parsing to cast the parsed object to `PEMKeyPair` before conversion.
- Added the missing `TvCapability.NUMBER_PAD` domain capability used by AI command policy.
- Corrected remote protocol package validation to compare model patterns against `ProtocolPackageManifest`.
- Corrected macro execution wiring to use the `MacroCommandExecutor` interface.
- Fixed Compose imports/API usage: `rememberSaveable`, pointer-drag handling, `ElevatedCard` click parameter order, and the missing `RemoteViewModel` passed to the Apps screen.
- Promoted the active release to V59 with integer-only human-facing versioning: `versionName = "59"`; Android `versionCode = 60`.

# V58 — Engineering Correction & Release Hardening

- Fixed `DiagnosticReportFormatterTest` to construct `TvFingerprint` with explicit named arguments and correctly separated address/port fields.
- Added a repository invariant that rejects positional `TvFingerprint(...)` construction in production/test Kotlin sources.
- Added a release-version invariant: the human-facing `V<integer>` release marker in `README.md` must exactly match Android `versionName`.
- Promoted the active release to V58 with integer-only version metadata (`versionName = "58"`, `versionCode = 59`).
- Preserved V57 and earlier entries below as historical records.

## V57 — Universal Remote Capability Expansion

- Added playback transport commands, numeric keypad, guide and input commands.
- Added device favorites and curated local macros.
- Added safe copyable diagnostics with bounded history and deterministic redaction.
- Added capability-aware remote controls for playback/input/numeric features.
- Added diagnostic export support in the UI for troubleshooting without exposing secrets.
- Added a deterministic protocol capability audit test covering executable transport bindings and capability declarations.
- Kept the documented physical-TV and CI execution limitations explicit.

## V55 — Engineering Consolidation

- Promoted the application release from V54 to V55 using integer-only version metadata (`versionCode = 56`, `versionName = "55"`).
- Reconciled current documentation and repository verification rules with the V55 release.
- Preserved historical V54/V53 entries as history rather than relabeling prior releases.

## V53 — Full Audit Reconciliation

- Fixed all non-exhaustive `Command` `when` expressions in the real protocol transports.
- Rotated the development protocol-signing key material consistently across all six repository packages; production signing remains a separate deployment concern.
- Added a compiled generic-IR protocol binding and kept IR as a built-in capability rather than a remotely distributed protocol.
- Removed the unimplemented DLNA/UPnP remote package from the distributable protocol repository and built-in catalog.
- Reconciled discovery capabilities with executable transport capabilities for Android TV, Samsung, LG, Sony, Philips, Roku, and IR.
- Removed the staged Voice capability from Android TV discovery so the UI cannot advertise an unwired voice path.
- Added executable binding compatibility checks before protocol activation.
- Added release-variant lint/build coverage to CI.
- Fixed the protocol repository User-Agent to use the actual application version.
- Tightened the local-TV HTTP client so both HTTP and HTTPS destinations must be private IPv4 addresses.
- Removed the unused `TvTransportPolicy.isAllowed` abstraction.

# Changelog

## V54 — Protocol/Runtime Correctness Hardening

- Corrected the Android TV Remote v2 Protobuf wire schema to the established v2 field layout, including volume, IME, voice, handshake, and app-link messages.
- Corrected the Android TV v2 pairing sequence: protocol version 2, server-advertised pairing options, client configuration, configuration acknowledgement, then PIN secret exchange.
- Hardened Android TV command-channel TLS so an unpaired device cannot enter the normal control channel through certificate TOFU; control requires the persisted paired server certificate.
- Made the Android TV remote handshake deterministic by waiting for the TV configuration and activation responses before starting the background receive loop.
- Corrected IME counters/cursor tracking and implemented protocol-compatible batch-edit text insertion.
- Corrected Android TV volume handling to use real key injection for relative volume commands.
- Added bounded Android TV voice framing with session IDs and 20 KiB PCM frames; capture now uses the protocol's 8 kHz mono PCM format and preserves resource lifecycle ordering.
- Prevented concurrent connection/discovery operations from being started repeatedly from the UI.
- Expanded connection-state transitions so active operations can be cancelled and an already-ready connection can be restarted cleanly.
- Hardened remote protocol repository paths against traversal/host escape.
- Added regression coverage for the connection-state lifecycle and Android TV wire-format invariants.


## V52 — CI Lint Fix / Permission-Safe Staged Voice Capture

- Fixed the GitHub Actions `lintDebug` blocker in `VoiceCapture.kt`: `AudioRecord` is now reached only after an explicit `RECORD_AUDIO` runtime-permission check.
- Injected `Context` into the staged voice controller/capture path so the permission check is real rather than a lint suppression.
- Kept `RECORD_AUDIO` out of the current manifest because the voice subsystem remains intentionally unwired; documented that future UI wiring must request the runtime permission before starting capture.
- Bumped release metadata from V51 to V52.

## V51 — Audit Follow-up / Build-Safe Hardening

- Added the missing `AiCommandNames` single source of truth that V50 referenced but did not ship.
- Converted `AiCommandAllowList` into a compatibility facade over the shared command-name set.
- Fixed `VoiceCapture` shutdown ordering with coroutine `cancelAndJoin()` before releasing `AudioRecord`.
- Made `CommandDeliveryClass` operational: coalescable commands are paced, while idempotent commands receive one safe retry after a failed transport attempt.
- Removed the unused `lastCommand` state from the dispatcher.
- Added optional release signing configuration driven by Gradle properties; no private signing material is stored in the repository.
- Added release-signing properties to `gradle.properties.example`.
- Strengthened repository invariants to catch the missing `AiCommandNames` declaration and validate the hardening changes before Gradle execution.

## V50 — Audit Hardening

- Hardened remote protocol signature canonicalization to cover trust/source metadata.
- Centralized HTTPS endpoint validation and removed the production dead-code security policy split.
- Hardened local TV address validation against loopback/link-local targets.
- Made PSK authentication state truthful and reuse persisted credentials.
- Fixed voice capture lifecycle ownership to avoid read/release races.
- Applied command delivery classification at dispatch without silently dropping user input.
- Added release R8/proguard configuration, lint fail-on-error policy, and deterministic protocol-repository timestamps.
- Removed Python bytecode artifacts and strengthened repository invariants.
- Kept staged future components explicitly documented rather than presenting them as shipped features.

## V50 pre-CI audit hardening
- Corrected stale V47 release metadata in the active README and build matrix.
- Replaced the protocol repository `generatedAt` publish placeholder with an actual UTC timestamp generated during manifest creation.
- Extended repository invariants to catch stale release metadata, placeholder timestamps, and the Protobuf Kotlin DSL regression before Gradle execution.

## V50 capability expansion
- Added executable Android TV Remote v2 text input and App Link launch commands.
- Added persisted app favorites and functional app-launch UI.
- Replaced keyboard placeholder with executable text dispatch.
- Replaced dead mouse placeholder with a functional gesture-to-D-pad fallback.
- Added successful command latency measurement to Diagnostics.

## V50 — Security / Release Hardening
- Fixed insecure default for the protocol repository trust anchor: production public key now defaults to empty.
- Fail closed when a remote protocol repository URL is configured without a trusted public key.
- Updated release/version documentation and CI artifact naming to V50.
- Removed stray `tools/verify_project.py.tmp`.
- Extended repository invariants to prevent regression of the trust-anchor configuration.

## V47
- Unified SSDP and Android TV Remote v2 discovery through `DiscoveryCoordinator`.
- Replaced declarative-only protocol bindings with executable compiled adapter factories.
- Centralized protocol compatibility scoring in `ProtocolSelector`.
- Migrated runtime capability projection to canonical `Capability` values.
- Added protocol-specific Android TV pairing to the live authentication path.
- Namespaced Android TV server certificates per device.
- Hardened LG SSAP registration to wait for the registration response and persist `client-key`.
- Added adapter-level health checks and kept `UNKNOWN` where a safe probe is unavailable.
- Made recovery network-change aware using the network of the last successful connection.
- Added corrupt protocol-package quarantine.
- Consolidated GitHub CI into one workflow and explicitly provisions Gradle 8.11.1.
- Corrected project documentation and release version to 47.
- Final archive must contain project contents at its root; no workspace directory is included.

## Explicit limitations
- LG pointer-input navigation remains **به‌زودی**.
- Physical TV validation and successful CI execution are not claimed until externally executed.
- A fabricated `gradle-wrapper.jar` is never included.

## V54 — Full Follow-up Audit + Hardware-Aware Discovery

- Added a central phone hardware/network preflight so Wi-Fi SSDP/mDNS/NSD discovery is skipped immediately when the phone has no Wi-Fi radio or no active Wi-Fi network.
- Added scoped Wi-Fi multicast locking for SSDP discovery.
- Kept IR discovery independent of Wi-Fi and gated it on a real `ConsumerIrManager` emitter.
- Kept Bluetooth HID undiscovered/unadvertised because no executable universal phone-as-HID transport exists; no Bluetooth discovery work is started.
- Standardized discovered device protocol IDs with the canonical executable protocol package IDs.
- Standardized generic IR on `generic-ir-tv` and prevented it from being a fallback for arbitrary network devices.
- Removed LG navigation/OK command advertisement from discovery/runtime metadata because the current LG transport does not execute those commands.
- Made protocol command metadata symmetric with capabilities: required commands must exist and commands cannot be declared without their matching capability.
- Re-signed all six remote protocol packages after the metadata reconciliation.
- Hardened the legacy mDNS engine with Wi-Fi preflight, a bounded timeout, and Android-TV-only discovery so unsupported Google Cast/AirPlay candidates are not surfaced.
- Fixed the staged AI command capability mapping for mute and playback.
- Removed the unused duplicate `CapabilityResolver` implementation.
- Bumped release metadata to V54.

# V62 — Corrected7 Java Resource Merge Fix

- Fixed Android `mergeDebugJavaResource` failure caused by the duplicate Bouncy Castle resource `META-INF/versions/9/OSGI-INF/MANIFEST.MF`.
- Added an explicit Android packaging exclusion for that duplicate Java resource.
- Kept the release identity unified at V62 / `versionName = "62"` / `versionCode = 62`.
