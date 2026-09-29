#!/bin/sh
set -e
if command -v gradle >/dev/null 2>&1; then exec gradle "$@"; fi
echo "Gradle executable not found. Use GitHub Actions or install Gradle 8.11.1 locally." >&2
exit 1
