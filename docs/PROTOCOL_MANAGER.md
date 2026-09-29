# Protocol Manager — V56

V56 adds the official Protocol Distribution Service while preserving the V56 rule that remote data is declarative only.

## Distribution architecture

`official sources → validation → signed package → manifest → HTTPS/CDN → app`

The repository lives under `protocol-repository/` and is CDN/static-host friendly. `manifest.json` is intentionally small and contains compatibility metadata, release state, version, SHA-256, size, and package path. The app therefore downloads the manifest first and normally downloads only the one selected package.

## Package trust

Remote packages must use ECDSA P-256 / SHA-256 signatures. The trusted public key is compiled into the APK through `protocolRepositoryPublicKey`; the private signing key must never be committed to Git. SHA-256 is checked against both the package manifest and the repository manifest.

## Release states

- `STABLE`: normal user selection.
- `BETA`: eligible but not preferred over a stable match.
- `DEPRECATED`: never selected for normal remote resolution.
- `BLOCKED`: explicitly rejected.

Only precompiled adapters in `ProtocolBindingRegistry` execute commands. The repository cannot ship executable code.

## Deployment

The private Android workflow publishes the verified `protocol-repository/` tree to the separate public repository `hadihaddadian/universal-tv-remote-protocols`. That public repository deploys the tree to GitHub Pages. Set the app build property `protocolRepositoryUrl` to `https://hadihaddadian.github.io/universal-tv-remote-protocols/` and keep `protocolRepositoryPublicKey` equal to the trusted public key.


## Executable binding rule

A declarative package is not executable merely because its signature is valid. Before activation, the runtime requires a compiled `ProtocolBindingRegistry` binding for at least one declared transport. The remote repository currently distributes six network protocols; generic IR remains built-in-only, and the unimplemented DLNA/UPnP profile is not distributed.
