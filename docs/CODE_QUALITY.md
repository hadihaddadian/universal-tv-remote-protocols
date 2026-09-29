# Code Quality — V56

Design rules:
- One canonical type per domain concept at each boundary.
- One protocol compatibility scorer.
- One executable protocol binding registry.
- UI does not implement transport logic.
- Remote protocol packages are data-only.
- State-machine transitions are checked rather than silently ignored.
- Commands are never reported successful when they were dropped.
- Corrupt persisted protocol data is quarantined.
- Background protocol refresh scopes are closed with the ViewModel lifecycle.
