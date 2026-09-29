# Universal TV Remote — Official Protocol Repository

This public repository is the static, CDN-friendly distribution endpoint for the app's declarative protocol packages.

The private Android application repository generates and verifies this tree before publishing it here.

## Distribution URL

`https://hadihaddadian.github.io/universal-tv-remote-protocols/`

## Security model

- HTTPS only.
- Packages contain declarative protocol data only; no DEX/APK/JAR executable payloads.
- Every remote package is SHA-256 checked and ECDSA P-256/SHA-256 signed.
- `manifest.json` is the compatibility/release index. The app downloads only the selected package.
- Release states: `STABLE`, `BETA`, `DEPRECATED`, `BLOCKED`.
- The app refuses blocked/incompatible releases and rejects downgrade/same-version replacement.
- Keep the signing private key outside Git and outside the deployed repository.

GitHub Pages deployment is defined in `.github/workflows/pages.yml`.
