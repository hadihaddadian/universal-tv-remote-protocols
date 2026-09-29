> Historical document. It is retained for traceability and must not override README.md, ARCHITECTURE.md, IMPLEMENTATION_STATUS.md, CURRENT_STATUS.md, or SECURITY.md.

# V32.3 Follow-up fixes

## Applied
- Removed the manifest networkSecurityConfig override that previously superseded usesCleartextTraffic and blocked plain HTTP TV protocols before runtime policy checks. Cleartext remains restricted by application transport policy to approved private/local addresses.
- Removed unused LG `keyPayload` construction; LG non-ENTER key control remains **به‌زودی** until a paired pointer-input socket implementation is wired.
- Updated README version from stale v0.9 to v0.32.2.

## Still open
- LG pointer-input socket implementation: به‌زودی
- Full local Gradle wrapper recovery/build confirmation: requires a genuine wrapper artifact or successful CI run
- RemoteCommand/Command unification and full legacy TvTransportRegistry migration: pending dedicated integration refactor
- Documentation consolidation: partially addressed; historical versioned documents retained for traceability and must not be treated as current architecture.
