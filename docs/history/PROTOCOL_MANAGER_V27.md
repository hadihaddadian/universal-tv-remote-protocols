> Historical document. It is retained for traceability and must not override README.md, ARCHITECTURE.md, IMPLEMENTATION_STATUS.md, CURRENT_STATUS.md, or SECURITY.md.

# Protocol Manager V27

## Shared protocol core

Protocol identity is `protocolId + version + sha256`, not manufacturer.
A `ProtocolProfile` can declare `supportedManufacturers`, allowing multiple TV brands/models to reuse one protocol package.

Example:

`Sony + TCL + Hisense -> androidtv-remote-v2@1`

The package is stored once under the protocol ID. Device-specific differences remain in profile/mapping metadata.

## Resolution order

1. Fingerprint the TV.
2. Search installed packages by protocol ID/profile compatibility.
3. Reuse the installed package if it satisfies the required version.
4. Query the built-in catalog.
5. Query remote repositories only if the protocol is missing or an update is needed.
6. Verify package integrity and trust.
7. Resolve dependencies.
8. Atomically install only newer content.
9. Return the protocol profile to the Connection Engine.

## No duplicate downloads

The manager never uses manufacturer as the storage key. If two brands resolve to the same `protocolId`, the installed package is reused.

The package SHA-256 is also retained for content identity, so identical content from different repositories can be recognized as the same payload.

## Compatibility

`ProtocolPackageCodec` persists `supportedManufacturers`, while older packages without this field decode safely to an empty set.

## Extensibility

New brands should normally be added as profiles, not as new protocol packages. New protocol packages are introduced only when the wire protocol/transport/command semantics are genuinely different.

Dependencies are resolved through the Protocol Manager boundary so shared protocol cores can later depend on reusable authentication or transport definitions without changing Connection Orchestrator code.
