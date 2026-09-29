# V32.5 ProtocolManager Migration

## Completed
- Removed `TvTransportRegistry`.
- `RemoteViewModel` now prepares every non-IR device through `ProtocolManager`.
- `ProtocolTransportExecutor` is the sole bridge from a resolved `ProtocolProfile` to executable in-app transports.
- `ProtocolBindingRegistry` is checked before an executable transport is selected.
- Command dispatch uses the active resolved protocol instead of `TvDevice.protocol` to select a vendor transport.
- Disconnect clears the active protocol session.

## Intentional limits
Android TV Remote v2 and generic declarative profiles are not given a fake executable transport. If no executable adapter exists, connection fails explicitly rather than falling back to the deleted legacy registry.
