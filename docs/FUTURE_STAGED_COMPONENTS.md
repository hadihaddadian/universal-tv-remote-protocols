# Staged Components — V56

These components are intentionally retained as future-extension scaffolding and are not wired into the current user-facing runtime:

- `VoiceRemoteController`, `VoiceCapture`, `AiGateway` / backend voice: future voice UI entry point.
- `IrLearningEngine`: future IR-learning hardware integration.
- `BluetoothHid`: future external Bluetooth HID integration.
- `ConnectionAiPlanner` / `ConnectionMemory`: future connection-assistance experiments.
- `MacroEngine` / `ConnectionAssistant`: future automation/assistant work.

They remain isolated from the current runtime so future work can reuse the domain boundaries without silently presenting unfinished features as available. Any future wiring must add a real UI entry point and integration tests first.


`VoiceCapture` requires the Android `RECORD_AUDIO` runtime permission before opening `AudioRecord`. The permission is intentionally not declared in the current manifest while voice remains unwired; any future UI integration must request and grant it before calling `VoiceRemoteController.start()`.


## Capability advertisement rule

A staged feature must not appear in discovery capabilities. In particular, `voice` is excluded from Android TV discovery until the microphone permission and end-to-end voice runtime are wired. This prevents the UI from advertising a control path that cannot execute.
