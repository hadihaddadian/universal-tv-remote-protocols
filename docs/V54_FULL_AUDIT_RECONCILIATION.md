# V54 Full Audit Reconciliation

## Scope
This release incorporates the V53 follow-up audit and a second project-wide consistency pass. The V53 report explicitly left TLS first-use TOFU and regex-based logging as unchanged items, while also noting a stale archive root directory name.

## Findings fixed in V54

1. **Hardware-aware discovery preflight**
   - Wi-Fi discovery now requires both a Wi-Fi radio and an active Wi-Fi network.
   - SSDP and Android TV NSD are not started when that preflight fails.
   - IR discovery remains independent and is added only when a real IR emitter is present.
   - Bluetooth has no executable HID discovery path, so no Bluetooth discovery work is started; the central hardware layer can still detect whether a Bluetooth radio exists for future implementations.

2. **SSDP multicast behavior**
   - `CHANGE_WIFI_MULTICAST_STATE` is declared.
   - A scoped `WifiManager.MulticastLock` is acquired only around SSDP and always released.

3. **Protocol identity normalization**
   - Discovery now emits the same canonical IDs used by `BuiltInProtocolCatalog` and `ProtocolBindingRegistry`.
   - Existing V53 credential keys using short IDs remain readable through compatibility aliases.
   - Generic IR uses `generic-ir-tv` consistently.

4. **Generic IR selection safety**
   - Generic IR is no longer a fallback for arbitrary unknown network devices.
   - It is selected only for an explicit IR fingerprint.

5. **Capability/command symmetry**
   - Catalog generation declares commands only for declared capabilities.
   - Package validation rejects both missing commands for declared capabilities and commands declared without their matching capability.
   - LG is limited to power/volume/mute because its current executable transport does not implement navigation/text/app launch.

6. **Discovery candidate quality**
   - The legacy mDNS engine is bounded by a timeout and only surfaces Android TV because Google Cast/AirPlay have no executable protocol binding in the current release.
   - Android TV generic service names no longer cause distinct TVs to be merged solely because their advertised names match.

7. **AI capability mapping**
   - `Mute` maps to `MUTE` and `PlayPause` maps to `PLAYBACK` in the staged AI policy.

8. **Logging boundary**
   - Known credential fields and sensitive URL query parameters are redacted.
   - Throwable messages are sanitized before logging rather than passing the original throwable object, avoiding a second unsanitized message channel.

9. **Dead duplicate abstraction**
   - The unused `CapabilityResolver` was removed.
   - The unused `ProtocolRuntime.requireBinding` method was removed.

10. **Release/package consistency**
   - Release metadata is V54.
   - The packaged project root is `v54audit/`, matching the release.
   - All six remote protocol packages were re-signed after metadata changes and verified against the development public key.

## Verification performed

- Project invariants: OK.
- Protocol repository verification: OK (6 releases).
- Extended static reconciliation: OK.
- No `.pyc`, `__pycache__`, or `.tmp` artifacts shipped.
- ZIP integrity checked after packaging.

## Remaining security design note

The Samsung/local WSS path still uses first-use TOFU for self-signed TV certificates. Eliminating that completely requires an explicit certificate-fingerprint verification UX; silently switching to the Android system trust store would reject many self-signed local-TV certificates and break first-use connectivity. V54 therefore does not pretend that TOFU has been eliminated.

Gradle compilation/lint/test/assemble remains authoritative in GitHub Actions because this workspace does not contain a genuine Gradle wrapper JAR and has no system Gradle executable.

## Second V54 correctness pass

The follow-up source audit identified a more important issue than the earlier architectural checks: the Android TV Remote v2 generated-message schema in the archive did not match the established v2 wire format. The runtime was therefore internally consistent with its own schema but not wire-compatible for several operations.

The correction included:

1. **Wire-schema correction** — volume level/mute fields, IME batch-edit structure, voice session/payload fields, remote handshake messages, and message field numbers were reconciled with the established v2 protocol schema.
2. **Pairing sequence correction** — the client now receives the TV's pairing options, sends `PairingConfiguration`, waits for `PairingConfigurationAck`, and only then submits the PIN-derived pairing secret.
3. **Control-channel trust boundary** — normal port 6466 connections now require the server certificate persisted by a successful pairing.
4. **Handshake ordering** — `RemoteConfigure` and `RemoteSetActive` responses are consumed before the background receiver starts, avoiding a race between handshake and command traffic.
5. **Text input** — the engine tracks TV-reported IME/field counters and cursor positions and emits protocol-compatible `RemoteEditInfo` records.
6. **Voice framing** — session IDs, PCM field names, and bounded frame sizes now match the protocol model; capture uses 8 kHz mono PCM.
7. **Lifecycle hardening** — repeated connect/discover operations are guarded, and active connection states can be cancelled cleanly.
8. **Repository path hardening** — signed remote package paths cannot escape the configured repository host or use traversal segments.

These changes are source-level corrections. Physical TV certification still requires real hardware, and the workspace still cannot execute Gradle locally because it intentionally contains no Gradle wrapper JAR and no installed Gradle distribution.

## Second V54 follow-up fixes

This branch incorporates the follow-up audit corrections:

- `TvCapability.PLAYBACK` is now declared in the canonical `TvCapability` enum, removing the known Kotlin compile break in the AI/capability matrix.
- Android TV Remote v2 pairing now sends the client's `PairingOption` immediately after `PairingRequestAck`, then waits for the TV acknowledgement before sending `PairingConfiguration`.
- A regression test locks the pairing option to six-character hexadecimal input and `ROLE_TYPE_INPUT`.

The local-TV TLS TOFU path remains intentionally isolated from the Android TV pairing certificate store. A complete elimination of first-use trust requires an application-level certificate-fingerprint confirmation UX for protocols whose first connection is itself the pairing step; that UX is not present in this branch and therefore has not been represented as solved here.
