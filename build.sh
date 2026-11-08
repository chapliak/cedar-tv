#!/usr/bin/env bash
# Build the debug APK in Docker (no Android SDK on the server). Output: dist/cedar-tv-debug.apk
# The debug signing key is kept in ~/.android-cedar-build so updates install over each other (adb install -r).
# Usage: bash build.sh [assembleDebug (default) | assembleRelease -> dist/cedar-tv-release.apk, optimised, use this on the TV]
set -euo pipefail
cd "$(dirname "$0")"
TASK="${1:-assembleDebug}"
VARIANT=debug; [ "$TASK" = assembleRelease ] && VARIANT=release
docker run --rm -v "$PWD":/project -v cedartv-gradle:/root/.gradle -v "$HOME/.android-cedar-build":/root/.android -w /project mingc/android-build-box:latest \
  bash -c "./gradlew --no-daemon -q $TASK; rc=\$?; chown -R $(id -u):$(id -g) /project/app/build /project/build /project/.gradle 2>/dev/null; exit \$rc"
mkdir -p dist
OUT=dist/cedar-tv-$VARIANT.apk
cp app/build/outputs/apk/$VARIANT/app-$VARIANT.apk "$OUT"
ls -lh "$OUT"
