# V59 CI Compile Fix Audit

## Source of findings

This release is based directly on the GitHub Actions Kotlin compiler output supplied for V58. The failures listed below were reproduced by source inspection and corrected in the project tree.

## Corrections

1. `AndroidTvDiscovery.kt` — implemented `onServiceLost`.
2. `AndroidTvRemoteV2.kt` — restored the missing `AtvKey` public enum and common Android key codes.
3. `AtvCertificateStore.kt` — cast PEM private-key objects to `PEMKeyPair` before `JcaPEMKeyConverter.getKeyPair`.
4. `domain/RemoteDomain.kt` — added `TvCapability.NUMBER_PAD`.
5. `HttpProtocolRepository.kt` — corrected `modelPatterns` validation to use `parsed.manifest.modelPatterns`.
6. `RemoteViewModel.kt` — supplied the required `MacroCommandExecutor` implementation to `MacroEngine`.
7. `UniversalRemoteApp.kt` — imported `rememberSaveable`, removed the incompatible pointer `consume()` call, corrected `ElevatedCard` click argument ordering, and passed `vm` to `AppsScreen`.

## Verification limits

The supplied CI log is authoritative evidence that V58 failed at Kotlin compilation. The local environment does not contain a Gradle distribution or `gradle-wrapper.jar`, and outbound DNS/network access is unavailable, so a local Gradle compile cannot be executed here. The repository was instead checked with static source/invariant checks and packaged after the corrections. The next GitHub Actions run remains the authoritative full compiler verification.
