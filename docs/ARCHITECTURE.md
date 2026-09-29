# Architecture — V56

## Runtime flow

`DiscoveryCoordinator`
→ `ProtocolManager.prepareFor(fingerprint)`
→ `ProtocolSelector`
→ `ProtocolBindingRegistry`
→ compiled `TvTransport`
→ `RemoteCommandDispatcher`

### Discovery
`DiscoveryCoordinator` is the only UI-facing discovery entry point. SSDP and Android TV Remote v2 NSD are concurrent providers; results are normalized before presentation.

### Protocols
`ProtocolManager` owns resolution, validation, installation and refresh. `ProtocolSelector` is the sole compatibility scoring implementation.

`ProtocolBindingRegistry` maps a protocol ID + transport type to a compiled factory. Downloaded protocol packages cannot register arbitrary code.

### Authentication
Authentication is protocol-specific:
- Android TV Remote v2: actual pairing engine + per-device server certificate pinning.
- Sony: PSK.
- LG: SSAP registration/client-key response.
- Samsung: protocol-managed WebSocket authorization.
- Philips/Roku: protocol-specific local connection without a fake generic PIN flow.

### Reliability
- `ConnectionStateMachine` protects lifecycle transitions.
- `ConnectionRecoveryCoordinator` bounds retries and can rediscover after network identity changes.
- `ConnectionHealthMonitor` reports adapter-specific health or `UNKNOWN` when no reliable probe exists.
- `RemoteCommandDispatcher` serializes commands and applies pacing without silently dropping user commands.


### Capability contract
Discovery capabilities are treated as verified capabilities for the discovered transport. They are reconciled with the selected protocol profile; staged capabilities such as voice are not advertised. Protocol metadata that declares keyboard, app launch, or playback must also declare the corresponding command definition.

## V56 hardware-aware discovery
`PhoneHardwareCapabilities` is the preflight boundary for discovery. Wi-Fi discovery requires a Wi-Fi radio plus an active Wi-Fi network; IR discovery requires a real IR emitter; Bluetooth is not probed for HID discovery because the current runtime has no executable Bluetooth HID transport.

Protocol IDs used by discovery are identical to the IDs in `BuiltInProtocolCatalog` and `ProtocolBindingRegistry`.
