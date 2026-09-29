# V58 Correction Audit

## Scope
Follow-up to the V57 audit. The audit identified one compile-breaking test constructor and one inaccurate duplicate V57 changelog claim.

## Corrections
- `DiagnosticReportFormatterTest.kt` now uses named `TvFingerprint` arguments.
- The test now supplies `address = "192.168.1.42"` and `port = 8002` as separate fields.
- `tools/verify_project.py` now rejects positional `TvFingerprint(...)` call sites outside the data-class declaration, preventing recurrence of this constructor-order regression.
- The active release is promoted to **V58**.
- Android `versionName` is **"58"** and the human-facing README release marker is **V58**.
- Android `versionCode` is **59**, continuing the project's monotonic Android identifier convention while keeping the human-facing release number and `versionName` identical.
- The verifier now checks the README `V<integer>` release marker against `versionName`.
- The duplicate/inaccurate V57 changelog entry was consolidated into a single historical V57 entry; the nonexistent "human-facing version/versionName equality" implementation claim was removed.

## Verification performed
- `tools/verify_project.py`: **PASS — Project invariants: OK**
- Archive integrity (`unzip -t`): required before delivery.
- System Gradle: unavailable in this workspace (`gradle` command not installed).
- No genuine Gradle wrapper JAR is fabricated or added.

## Remaining external gate
A real Gradle/CI execution remains required for Kotlin compilation, unit tests, lint, and APK assembly. This audit does not claim those executions occurred locally.
