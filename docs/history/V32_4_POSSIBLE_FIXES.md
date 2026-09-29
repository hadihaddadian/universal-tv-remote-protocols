# v0.32.4 Possible Fixes Applied

This pass addressed issues that could be corrected from the available source without fabricating binaries or device-test results.

1. Fixed the Android cleartext configuration mismatch by removing an invalid CIDR-like domain configuration that could not reliably represent private IPv4 ranges.
2. Removed dead `RemoteCommand` duplication after repository-wide reference checking showed no active usages.
3. Normalized the current version to 0.32.4 / 37 in the active Gradle configuration and README.
4. Added CURRENT_STATUS.md as the canonical implementation-status document.
5. Marked older version-suffixed architecture/audit documents as historical to reduce contradictory guidance while retaining traceability.

Not fabricated: Gradle wrapper JAR, APK build success, vendor hardware tests, or incomplete vendor protocol functionality.
