# Security Model — V56

## Protocol updates
- Remote repositories must use HTTPS.
- Packages are bounded by size.
- SHA-256 is checked against the manifest.
- Signed packages use ECDSA P-256/SHA-256 verification against a trusted application key.
- Minimum app version is enforced.
- Downgrades and same-version replacement with different content are rejected.
- Package manifest/profile identity must match.
- Installation is atomic.
- Corrupt local packages are quarantined instead of silently ignored.
- Protocol packages are declarative and cannot contain executable DEX/APK/native/script code.

## Local TV networking
Legacy TV protocols may require cleartext HTTP. The app-level cleartext capability exists only because of these local protocols; `LocalTvHttpClient` applies a runtime destination policy and rejects non-local cleartext endpoints. Internet-facing protocol repositories and backend sessions remain HTTPS-only.

## Credentials
Device credentials and Android TV server certificates are stored through an Android Keystore-backed AES-GCM store. Android TV server certificates are namespaced per device, so pairing one TV cannot overwrite the trust anchor of another.

## AI boundary
The Android client does not embed a provider API key. AI output must remain inside the TV-command allow-list/validation boundary. AI is not a general-purpose action executor.


## Local Samsung TLS TOFU

The first TLS connection to a local Samsung endpoint uses trust-on-first-use because many TVs expose self-signed certificates. The app immediately pins the observed certificate per device after the TLS handshake. This first-use window is an explicit residual risk on an untrusted LAN; users should pair on a trusted local network. The app never replaces an existing pin after a successful pin has been stored.


### Local transport boundary
The local-TV OkHttp client now restricts both cleartext and TLS destinations to literal RFC1918 IPv4 addresses. This avoids turning the local client into a generic HTTPS client and preserves the DNS-rebinding/SSRF boundary.

## V56 discovery preflight
- Local network discovery is gated by actual Wi-Fi hardware and an active Wi-Fi transport before sockets or NSD discovery are started.
- SSDP multicast uses a scoped Wi-Fi multicast lock and releases it in `finally`.
- IR is exposed only when `ConsumerIrManager.hasIrEmitter()` is true.
