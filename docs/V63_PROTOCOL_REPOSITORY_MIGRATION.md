# V63 Protocol Repository Migration

## Purpose

The Android application repository remains private. The remotely distributed declarative protocol packages are published to a separate public repository:

`hadihaddadian/universal-tv-remote-protocols`

The private application workflow builds and cryptographically verifies `protocol-repository/`, then synchronizes that verified tree to the public repository. The public repository owns its own GitHub Pages deployment workflow.

## Required one-time GitHub setup

1. Create the public repository `hadihaddadian/universal-tv-remote-protocols`.
2. Do not add application source code or secrets to that repository.
3. In the private `hadihaddadian/19` repository, create a fine-grained personal access token restricted to **only** `hadihaddadian/universal-tv-remote-protocols` with **Contents: Read and write** permission.
4. Store that token in the private repository as the Actions secret `PROTOCOL_REPOSITORY_TOKEN`.
5. In the public repository, open **Settings → Pages** and select **GitHub Actions** as the source. This is a one-time Pages enablement step; it is deliberately not attempted from the private repository.
6. After the first successful publication, the Pages URL is:
   `https://hadihaddadian.github.io/universal-tv-remote-protocols/`

## Runtime URL

The Android build property `protocolRepositoryUrl` should be set to:

`https://hadihaddadian.github.io/universal-tv-remote-protocols/`

The existing `protocolRepositoryPublicKey` trust anchor remains mandatory. The public repository contains declarative signed packages only; it cannot ship executable code.

## Failure isolation

- Android compilation/tests/lint/APK generation are in the `verify` job.
- Cross-repository publication is in a separate `publish-protocol-repository` job and runs only after `verify` succeeds on `main`.
- The private repository no longer requests `pages: write` or `id-token: write`.
- The private repository no longer calls `actions/configure-pages`.
- The public repository performs the Pages deployment with its own `GITHUB_TOKEN`.
- The publication step verifies the generated repository immediately before synchronization.
