# V40 implementation fixes

Applied source-level corrections from the rigorous V39 review:
- Failed -> Identifying is now a legal state-machine transition, so reconnect() can re-enter the connection flow.
- oneHandMode now changes the control layout instead of only persisting a switch.
- MacroEngine and MacroStep now use the canonical `Command` type. The obsolete macro-only `TvCommand` model was removed from `RemoteDomain.kt`.
- ProtocolTransportExecutor.send() now defensively rejects commands for a device other than the active protocol device.
- The inaccurate V39 CHANGELOG claim was corrected.
- V32_1_STATIC_CHECKS.json was moved to docs/history/.

Still external/backend-limited and honestly open:
- Valid gradle-wrapper.jar is not present and cannot be fabricated safely.
- LG webOS non-ENTER commands still require a real executable SSAP/pointer-input backend.
