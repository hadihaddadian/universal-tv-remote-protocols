# Current Status — V63

V63 carries forward the V62 compiler/test hardening and separates the private Android application from the public protocol distribution repository. The Compose remote-control screen remains structurally decomposed to prevent Kotlin compiler memory exhaustion during IR/JVM code generation.

V60 reconciled the V53 follow-up audit findings with a hardware-aware discovery preflight, canonical protocol IDs, command/capability symmetry, and stricter discovery/runtime consistency.

Implemented:
- Ready → DeviceFound device switching regression fixed; selection no longer uses a crash-inducing `check` fallback.
- HTTPS manifest-driven remote protocol repository.
- Compatibility metadata in the manifest so the app avoids downloading every protocol package.
- SHA-256 verification and ECDSA P-256/SHA-256 signature verification remain mandatory for remote packages.
- Static CDN-friendly `protocol-repository/` tree with signed declarative packages for the currently catalogued protocols.
- Build-time repository URL/public-key configuration.
- GitHub Pages publication job in the authoritative Android CI workflow.
- Remote package execution remains prohibited; only built-in adapters execute commands.
- Regression test for Ready → DeviceFound.

Build limitation in this workspace: the project does not contain a genuine Gradle wrapper JAR and no system Gradle executable is installed. CI remains the authoritative end-to-end compiler/test runner.


## V60 Security/Build audit hardening
- Unused Bluetooth and microphone permissions are not declared until their staged features receive real UI/runtime wiring.
- Remote protocol signatures cover trust/source metadata.
- Remote protocol URL validation is centralized; repository configuration fails closed without its explicit public key.
- Local TV cleartext transport remains necessary for legacy protocols and is restricted by a single literal-IPv4 transport policy; this platform limitation is explicitly documented.
- Shared AI command-name registry is now shipped and consumed by both AI command validators; the previous missing-type build regression is eliminated.
- Voice capture shutdown awaits coroutine termination before releasing `AudioRecord`.
- Command delivery policy is now operational: coalescable commands are paced and idempotent commands receive one safe retry after a failed send.
- Release signing is supported through external Gradle properties; private signing material is never committed.

## V60 follow-up reconciliation
- Wi-Fi/SSDP/mDNS discovery is now skipped before any network discovery work when the phone has no Wi-Fi radio or no active Wi-Fi network.
- IR discovery is independent and is added only when a real `ConsumerIrManager` emitter is present.
- Bluetooth hardware is detectable through the central hardware-preflight layer, but Bluetooth HID discovery/control remains intentionally unavailable because Android does not expose a universal phone-as-HID role. No Bluetooth discovery work is started.
- All discovered network devices now use the same canonical protocol IDs as the executable protocol catalog (`samsung-ws`, `lg-ssap`, `sony-ircc`, `philips-jointspace`, `roku-ecp`, `androidtv-remote-v2`).
- Generic IR is `generic-ir-tv` and can no longer win as a fallback for arbitrary network devices.
- LG discovery no longer advertises navigation; its transport and protocol metadata are limited to power/volume/mute, matching the executable implementation.
- Protocol command metadata is now symmetric with capabilities: a declared capability requires its commands, and a command cannot be declared without its corresponding capability.
- The legacy mDNS engine now discovers only Android TV, has a bounded timeout, and never surfaces unsupported Google Cast/AirPlay devices as connectable TV candidates.
- SSDP obtains and releases a Wi-Fi multicast lock around discovery.

## V60 protocol/runtime correctness hardening
- Android TV Remote v2 `.proto` definitions are aligned with the established wire schema used by current v2 implementations; critical field numbers are covered by regression tests.
- Android TV pairing now follows the server-driven option/configuration sequence and uses protocol version 2.
- Normal Android TV control connections require the persisted server certificate created by a successful pairing; first-use TOFU is confined to the pairing channel.
- The command channel waits for the TV handshake responses before entering the asynchronous receive loop.
- Text input uses `RemoteImeBatchEdit` with synchronized IME/field counters and the latest text-field cursor range reported by the TV.
- Relative volume uses actual volume key injection rather than the non-parameterized `RemoteAdjustVolumeLevel` message.
- Voice framing is protocol-compatible and bounded; the staged UI/permission integration remains intentionally disabled until a complete user-facing voice path is wired.
- Connection-state cancellation/reconnect transitions and duplicate-operation guards were hardened.
- Remote protocol repository paths are constrained to relative paths on the configured trusted host.
