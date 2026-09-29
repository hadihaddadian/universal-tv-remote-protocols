# V41 Protocol Runtime Hardening

This release works from technical item 3 onward in the current engineering roadmap.

Implemented in source:
- Effective capability intersection between protocol declarations and observed device capabilities.
- Discovery normalization to merge duplicate observations by stable identity.
- Protocol/device-scoped credential storage on top of the existing encrypted secret store.
- Bounded connection recovery coordinator with rediscovery after network identity changes.
- Central command-policy classification without silent success for dropped user commands.
- IR capability remains explicitly hardware-gated.
- LG SSAP now sends direct service requests for Volume Up, Volume Down, Mute, Power and Enter. D-pad navigation still requires the paired pointer-input socket and remains **به‌زودی**.

Existing foundations retained:
- Android TV Remote engine, pairing engine, TLS and protobuf stack remain the reference backend foundation.
- Voice capture and AI intent boundary remain constrained to canonical remote commands.

Not claimed as complete:
- Android TV end-to-end pairing against real hardware.
- LG pointer-input socket acquisition and navigation commands (**به‌زودی**).
- Model-specific capability probing for every vendor.
- Hardware validation on a real TV matrix.

