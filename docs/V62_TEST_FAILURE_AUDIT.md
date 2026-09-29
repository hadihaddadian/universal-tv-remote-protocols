# V62 Test Failure Audit — Corrected5

## CI evidence

The V62 CI run reached `:app:testDebugUnitTest` successfully. Compilation of the debug/release Kotlin and Java sources completed. The failure was entirely in the JVM unit-test phase: 56 tests ran and 19 failed.

## Root causes addressed

1. **Built-in protocol catalog failed during JVM unit tests.**
   `BuiltInProtocolCatalog.profile()` generates its payload through `ProtocolPackageCodec`, which uses Android's `org.json` implementation. Local JVM tests can otherwise resolve Android framework stubs rather than a concrete JSON implementation. A JVM test dependency on `org.json:json` is now supplied.

2. **Protocol command/capability contract was incomplete.**
   The catalog emits `Guide`, `Stop`, `Previous`, `Next`, `Rewind`, `FastForward`, `Input`, and `Number`, but the validation/test mapping did not define all of them. The mapping is now centralized in `ProtocolCommandCapabilities` and shared by the validator and consistency test.

3. **Android TV pairing behavior was not represented when no ProtocolManager was supplied.**
   `ConnectionOrchestratorTest` supplies an Android TV capability fingerprint directly. The orchestrator now treats `android_tv_remote_v2` as requiring the pairing stage before connection, preserving the intended connection contract.

## Verification

- Project invariant verifier: PASS.
- Release identity remains V62 / `versionName = "62"` / `versionCode = 62`.
- Protobuf field remains `request_case_value = 3`.
- ZIP is produced only after static checks.
- Local Gradle execution remains unavailable in this environment; GitHub Actions remains the authoritative compile/test runner.
