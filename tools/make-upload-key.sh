#!/usr/bin/env bash
# Creates the REAL Google Play upload key once: ~/.cedar-upload/upload.jks + keystore.properties (chmod 600).
# Run it yourself in a terminal (it asks for the password with hidden input; never type it into a chat).
# AFTER running: copy BOTH files to 1Password + an offline backup. If you lose the key, Play must reset it (takes days).
set -euo pipefail
DIR="$HOME/.cedar-upload"; mkdir -p "$DIR"; chmod 700 "$DIR"
[ -e "$DIR/upload.jks" ] && { echo "Already exists: $DIR/upload.jks (refusing to overwrite)"; exit 1; }
read -r -s -p "New keystore password (12+ chars, no \$ or quotes): " P1; echo
read -r -s -p "Repeat: " P2; echo
[ "$P1" = "$P2" ] && [ ${#P1} -ge 12 ] || { echo "Passwords differ or are too short"; exit 1; }
docker run --rm -v "$DIR":/out mingc/android-build-box:latest keytool -genkeypair -keystore /out/upload.jks \
  -alias cedar-upload -keyalg RSA -keysize 4096 -validity 10000 -storepass "$P1" -keypass "$P1" \
  -dname "CN=Maksym Chapliak, C=CA"
printf 'storePassword=%s\nkeyPassword=%s\nkeyAlias=cedar-upload\n' "$P1" "$P1" > "$DIR/keystore.properties"
chmod 600 "$DIR/upload.jks" "$DIR/keystore.properties"
echo "Done. Back up $DIR/upload.jks and keystore.properties NOW (1Password + offline copy)."
