# Universal TV Remote — V55 Engineering Audit

## Scope

This revision incorporates the follow-up audit findings against the V54 Engineered/Hardened branch.

## Fixed

1. `TvCapability.PLAYBACK` was missing from the canonical enum while referenced by the AI policy and capability matrix. It is now declared.
2. Android TV Remote v2 pairing now sends the client `PairingOption` after `PairingRequestAck` and waits for the echoed acknowledgement before sending `PairingConfiguration`.
3. A protocol regression test verifies that the pairing option declares six-character hexadecimal input and `ROLE_TYPE_INPUT`.
4. Application versioning was advanced to `versionCode = 56`, `versionName = 55` so the corrected build is distinguishable from the prior V54 artifact.

## Verified by static inspection

- Android TV `remotemessage.proto` and `pairingmessage.proto` remain the corrected protocol schemas from the previous hardening pass.
- Android TV control-channel certificate enforcement remains active: a normal port-6466 connection requires a stored or explicitly supplied server certificate.
- Handshake responses are awaited before the receive loop starts.
- IME counters/field positions are reset on close and updated from incoming messages.
- Protocol repository URL/path validation remains fail-closed.
- No Kotlin source-level `TvCapability.PLAYBACK` reference remains unresolved because the enum now contains the member.

## Not claimable as locally executed

The supplied Gradle wrapper is a thin launcher that requires a system Gradle executable; the execution environment has no Gradle installation and no network access to provision one. Therefore `assembleDebug`, `test`, and Android lint cannot honestly be reported as executed in this environment.

The local-TV TLS first-use TOFU path is also not represented as fully eliminated: removing that trust decision requires an explicit certificate-fingerprint confirmation flow at the application layer for first-time Samsung/WSS-style connections.

## Release gate

Before release, run:

- `./gradlew clean test`
- `./gradlew lintDebug`
- `./gradlew assembleDebug`
- physical-device tests for Android TV Remote v2 pairing/control
- physical-device tests for Samsung WSS pairing/token acquisition
- regression tests for LG webOS, Sony, Philips, Roku, and IR transports


## Release numbering

V55 is the next human-facing release after V54. Android `versionCode` is 56 because the previously delivered V54.1 artifact already used versionCode 55; this preserves Play Store monotonicity while keeping release names integer-only.
