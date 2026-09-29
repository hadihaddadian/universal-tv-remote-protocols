> Historical document. It is retained for traceability and must not override README.md, ARCHITECTURE.md, IMPLEMENTATION_STATUS.md, CURRENT_STATUS.md, or SECURITY.md.

# UniversalTVRemote V30 — Unified Architecture

UI -> Application -> Connection -> Protocol -> Transport/Infrastructure

Discovery produces `TvFingerprint`; it never constructs protocol profiles.
`ProtocolManager` is the only protocol lifecycle/resolution owner.

Resolution order:
1. Built-in APK catalog
2. Verified local cache
3. Verified remote repository only if local resolution fails
4. Remote update checks run asynchronously after local resolution

Protocol identity is `ProtocolId`, not manufacturer/model. Multiple vendors can share one protocol package.

`ProtocolPackage` is declarative. `ProtocolBindingRegistry` maps known protocol IDs to in-app transports; downloaded data cannot introduce executable code.

Removed parallel protocol systems: DynamicProfiles/ProfileRepository/ProtocolRouter/AutoConnector and AutomaticConnectionEngine protocol construction.

Remote repositories require HTTPS and signed packages. Legacy local HTTP is retained only because several TV protocols require it; `LocalTvHttpInterceptor` rejects non-private IPv4 hosts.

Build matrix: AGP 8.7.3 / Kotlin 2.0.21 / Java 17 / compileSdk 35 / minSdk 26 / Gradle 8.11.1.
