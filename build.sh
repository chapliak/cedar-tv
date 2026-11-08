#!/usr/bin/env bash
# Build the debug APK in Docker (no Android SDK on the server). Output: dist/cedar-tv-debug.apk
# The debug signing key is kept in ~/.android-cedar-build so updates install over each other (adb install -r).
# Usage: bash build.sh [assembleDebug (default) | assembleRelease -> dist/cedar-tv-release.apk, optimised, use this on the TV
#        | bundleRelease -> dist/cedar-tv-release.aab for Google Play, signed with the upload key in ~/.cedar-upload]
set -euo pipefail
cd "$(dirname "$0")"
TASK="${1:-assembleDebug}"
VARIANT=debug; [ "$TASK" = assembleRelease ] && VARIANT=release
PLAY=0; [ "$TASK" = bundleRelease ] && { VARIANT=release; PLAY=1; }
UPLOAD="$HOME/.cedar-upload"; mkdir -p "$UPLOAD"
docker run --rm -e CEDAR_PLAY=$PLAY -v "$UPLOAD":/upload:ro -v "$PWD":/project -v cedartv-gradle:/root/.gradle -v "$HOME/.android-cedar-build":/root/.android -w /project mingc/android-build-box:latest \
  bash -c "./gradlew --no-daemon -q $TASK; rc=\$?; chown -R $(id -u):$(id -g) /project/app/build /project/build /project/.gradle 2>/dev/null; exit \$rc"
mkdir -p dist
if [ "$TASK" = bundleRelease ]; then
  OUT=dist/cedar-tv-release.aab
  cp app/build/outputs/bundle/release/app-release.aab "$OUT"
  [ -f "$UPLOAD/keystore.properties" ] || echo "WARNING: no upload key (~/.cedar-upload): this .aab is signed with the TEST key. Run tools/make-upload-key.sh first; do NOT upload this one to Play."
else
  OUT=dist/cedar-tv-$VARIANT.apk
  cp app/build/outputs/apk/$VARIANT/app-$VARIANT.apk "$OUT"
fi
ls -lh "$OUT"
