# Protocol repository tooling

`build_repository.py` creates a deterministic `manifest.json` from signed `.json` protocol packages.

`sign_package.py` signs a declarative package with an ECDSA P-256 private key and emits the corresponding public key.

Do **not** commit the private key. Store it in GitHub Actions Secrets or another dedicated secret manager.
