# Production Roadmap — V56

1. Verify V56 with Gradle 8.11.1 in GitHub Actions.
2. Create the public `hadihaddadian/universal-tv-remote-protocols` repository, enable GitHub Pages using GitHub Actions, configure `PROTOCOL_REPOSITORY_TOKEN` in the private app repository, and set `protocolRepositoryUrl` to `https://hadihaddadian.github.io/universal-tv-remote-protocols/`.
3. Move the development signing key to GitHub Actions Secrets / an external secret manager and rotate the compiled public key to the production key.
4. Populate only validated protocol releases; promote packages from BETA to STABLE only after hardware testing.
5. Add rollback/revocation automation and repository monitoring.
6. Expand real protocol adapters and hardware coverage.
