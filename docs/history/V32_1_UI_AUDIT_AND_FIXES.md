> Historical document. It is retained for traceability and must not override README.md, ARCHITECTURE.md, IMPLEMENTATION_STATUS.md, CURRENT_STATUS.md, or SECURITY.md.

# V32.1 UI Audit and Fixes

## Corrections applied
- Added the missing `androidx.compose.ui.input.pointer.consume` import required by `PointerInputChange.consume()`.
- Connection status in the Remote header now depends on `ConnectionPhase.Ready`; selecting a device no longer falsely displays it as connected.
- Remote mode selection is capability-aware. Pointer and keyboard modes are hidden when the selected device has not reported the corresponding capability.
- The Mouse mode no longer pretends that directional swipe commands are a real pointer transport. It is explicitly marked **به‌زودی** until pointer deltas/clicks are wired to an executable adapter.
- Apps are no longer displayed as actionable for devices that have not confirmed `app_launch`.
- Appearance, one-hand layout, and global Haptic settings were previously local UI state with no application effect. They are now explicitly marked **به‌زودی** rather than presented as working settings.

## Still intentionally not claimed as complete
- Real voice capture and command dispatch: **به‌زودی**
- Keyboard text/clipboard transport dispatch: **به‌زودی**
- App launching: **به‌زودی**
- Pointer movement/click adapter dispatch: **به‌زودی**
- Persistent favorites: **به‌زودی**
- Latency measurement: **به‌زودی**
- Safe-log export: **به‌زودی**
- Global persisted theme / one-hand / haptic preferences: **به‌زودی**

## Architectural limitation retained for safety
The UI still uses `TvTransportRegistry` through `RemoteViewModel`. This release does not falsely claim that the V32 UI has completed the migration to `ProtocolManager` / `ProtocolBindingRegistry`. That migration requires backend adapter wiring and should be performed as a dedicated integration refactor, not by deleting the legacy transport path from the UI layer.

## Build validation
The archive was statically checked after modification. A full Gradle build could not be executed in this environment because the project does not contain `gradle-wrapper.jar` and no system Gradle installation is available.
