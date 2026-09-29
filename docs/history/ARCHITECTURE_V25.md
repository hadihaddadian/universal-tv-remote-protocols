> Historical document. It is retained for traceability and must not override README.md, ARCHITECTURE.md, IMPLEMENTATION_STATUS.md, CURRENT_STATUS.md, or SECURITY.md.

# UniversalTVRemote V25

Layering:
domain -> application -> data/transport/security/ai -> ui

UI is state-driven and does not perform network I/O.
Application owns connection orchestration and command validation.
Protocol selection is capability-driven.

Connection state machine:
IDLE -> DISCOVERING -> FINGERPRINTING -> SELECTING_PROTOCOL ->
CONNECTING -> PAIRING? -> VERIFYING -> READY
Failure -> FAILED -> retry

Integrated feature boundaries:
- Auto connection and connection assistant
- Device discovery/fingerprinting
- Capability matrix
- Multi-TV profiles
- D-pad, touchpad, mouse and keyboard modes
- Clipboard-ready input
- Voice AI intent parsing with strict command policy
- Macros
- IR learning
- Signed protocol package validation
- Guest-policy-ready command gating

Security:
AI provider keys are never stored in the Android client.
AI output is validated against TV capabilities before dispatch.
Protocol packages require SHA-256 and signature verification.
Internet/AI endpoints must use HTTPS.

## Protocol Manager V25.1

`connection.protocol.ProtocolManager` is now the single lifecycle owner for protocol profiles.
It sits between device fingerprinting and the connection adapter/orchestrator.

- installed protocol cache is checked first;
- built-in catalog is the offline fallback;
- optional HTTPS repository supplies signed updates/new declarative profiles;
- package integrity, signature, compatibility and downgrade checks are centralized;
- installation is atomic;
- downloaded packages are declarative only and cannot execute code;
- executable vendor implementations remain compiled app bindings;
- `ProtocolBindingRegistry` is the extension point for future vendor modules.
