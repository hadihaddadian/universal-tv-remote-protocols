# V60 Feature & Version Audit

## Version invariant

V60 uses one integer everywhere:
- Human-facing release: V60
- Android `versionName`: `60`
- Android `versionCode`: `60`

The repository verifier now rejects any mismatch between `versionCode` and `versionName`.

## Feature completion

- Android TV voice transport is exposed through the Voice screen with runtime `RECORD_AUDIO` permission handling.
- Voice UI is capability-gated and reports transport state rather than advertising an unimplemented future feature.
- One-hand mode now affects the actual Compose layout.
- Favorite TVs are prioritized in device lists with deterministic name ordering.

## Verification

`tools/verify_project.py` must pass before packaging. Full Kotlin/Gradle compilation remains dependent on the GitHub Actions environment because this working environment does not contain a usable Gradle distribution.
