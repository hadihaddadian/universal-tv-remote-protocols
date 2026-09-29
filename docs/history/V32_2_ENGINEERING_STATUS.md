> Historical document. It is retained for traceability and must not override README.md, ARCHITECTURE.md, IMPLEMENTATION_STATUS.md, CURRENT_STATUS.md, or SECURITY.md.

# V32.2 Engineering Status

## Implemented in this revision
- Bounded retry/backoff foundation for connection attempts.
- Serialized command dispatcher with short duplicate-command coalescing.
- Explicit connection state-machine implementation and unit tests.
- Persistent settings repository foundation using SharedPreferences (no new dependency).
- GitHub Actions CI workflow for real `test` + `assembleDebug` validation.
- Existing protocol version/rollback and offline-first infrastructure retained and documented as active architecture.

## Intentionally not claimed complete
The following require real device/protocol adapter work or end-to-end testing and remain **به‌زودی**:
- Full migration from `TvTransportRegistry` to `ProtocolBindingRegistry`.
- Real Android TV end-to-end device validation.
- Capability probing that is verified against every vendor/model.
- Pointer delta/click/scroll transport adapters.
- Text and clipboard transport adapters.
- Real microphone capture plus speech/AI runtime integration.
- Persistent Favorites and executable app launching.
- UI-wide persisted theme/one-hand/haptic wiring (repository foundation is present).
- Automatic reconnect network observation beyond explicit retry.
- Per-protocol timeout classification and retryability rules.
- Full protocol rollback orchestration after post-update health checks.
- Credential storage migration audit for every vendor path.
- Mock TV servers for Android TV, Samsung, LG and Roku.
- Full integration test chain against protocol simulators.
- Gradle Wrapper JAR: still absent; CI uses installed Gradle instead.

## Important safety rule
No UI or documentation in this revision should present a future adapter as operational. Features without an executable backend path remain labeled **به‌زودی**.
