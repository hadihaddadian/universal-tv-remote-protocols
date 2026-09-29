# Implementation Status — V62

| Area | Status |
|---|---|
| SSDP discovery | Implemented |
| Android TV NSD discovery | Implemented |
| Unified discovery path | Implemented |
| Android TV Remote v2 pairing | Implemented in source; physical validation required |
| Samsung WebSocket | Implemented in source; model validation required |
| LG SSAP registration/client-key | Implemented in source; model validation required |
| LG pointer navigation | **به‌زودی** |
| Sony BRAVIA IRCC + PSK | Implemented in source; model validation required |
| Philips JointSpace | Implemented in source; model validation required |
| Roku ECP | Implemented in source; model validation required |
| Protocol package signatures | Implemented |
| Credential encryption | Implemented |
| Capability canonicalization | Implemented |
| Bounded recovery | Implemented |
| Adapter health probes | Partially implemented; `UNKNOWN` where no safe probe exists |
| Bluetooth HID | **به‌زودی** |
| Cloud TV control | **به‌زودی** |
| Physical hardware certification | Required |


## V62 CI compiler-memory correction

The remote-control UI was decomposed into `RemoteControlScreen.kt` so the large `RemoteControlScreen` function no longer owns the complete nested Compose control tree. Header, main mode panel, one-hand controls, standard controls, switcher, pads and action helpers are separate composables. This targets the V61 GitHub Actions failure: `OutOfMemoryError: GC overhead limit exceeded` while generating `RemoteControlScreen$lambda$108`.

## V61 CI compile correction
- Restored the missing closing scopes in `UniversalRemoteApp.kt` around the `Scaffold` content, `Box`, and `AnimatedContent` lambdas.
- This was the root cause of the cascade of unresolved composables and `private is not applicable to local function` errors reported by CI.

## V60 in-place capability expansion
- Android TV Remote v2: executable text input through IME batch edit.
- Android TV Remote v2: executable HTTP(S) App Link launch path.
- Keyboard UI now sends text instead of a placeholder.
- App launcher UI supports custom App Link plus persisted favorites.
- Mouse mode has a functional D-pad gesture fallback rather than a dead placeholder.
- Successful command latency is surfaced in Diagnostics.
- Settings (theme, one-hand, haptics) remain persisted.
- Bluetooth HID, IR learning, LG pointer-input socket, cloud control, widgets/Wear/Quick Settings and external hardware remain blocked until a real platform/backend path exists.
