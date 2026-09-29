# Universal TV Remote — V56 Engineering Audit

## Release identity

- Human-facing release: **V56**
- Android `versionName`: **56**
- Android `versionCode`: **57** (monotonic after V55/versionCode 56)
- Rule: release number and `versionName` are identical integers; no decimal release names are permitted.

## Implemented in V56

1. Consolidated current documentation to V56 while retaining historical documents unchanged.
2. Added bounded, secret-redacting diagnostic report generation suitable for user sharing.
3. Added regression coverage proving diagnostic reports do not expose a full local IP address or token value.
4. Strengthened release verification so the declared human-facing version and Android `versionName` must match exactly.
5. Preserved the existing Android TV Remote v2 pairing, handshake, certificate enforcement, IME state tracking, protocol repository path hardening, capability symmetry and recovery architecture from V55.

## Explicitly not claimed

- No physical TV interoperability is claimed without hardware tests.
- Gradle assemble/lint/unit-test execution is only claimed when CI or a compatible local Gradle environment actually runs it.
- Voice and model-specific pointer backends remain staged unless an executable transport is wired and tested.
