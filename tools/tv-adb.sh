#!/usr/bin/env bash
# Runs adb against the Google TV from the server (Docker, host network); keys persist in ~/.android-tv-adb.
# Pair once (TV: Developer options -> Wireless debugging -> Pair with code):  tv-adb.sh pair IP:PAIRPORT CODE
# Then:  TV=IP:PORT tv-adb.sh '<bash snippet>'   -- inside, use  a  for  adb -s $TV  ; /dist = ../dist, /shots = $SHOTS
#   e.g. TV=<tv-ip>:44849 tv-adb.sh 'a install -r /dist/cedar-tv-debug.apk; a shell input keyevent 20'
set -e
HERE="$(cd "$(dirname "$0")/.." && pwd)"
if [ "$1" = pair ]; then SNIP="echo $3 | adb pair $2"; else
SNIP="for i in 1 2 3 4 5 6; do adb connect $TV >/dev/null 2>&1; [ \"\$(adb -s $TV get-state 2>/dev/null)\" = device ] && break; sleep 2; done; a(){ adb -s $TV \"\$@\"; }; $1"; fi
docker run --rm --network host -v ~/.android-tv-adb:/root/.android -v "$HERE/dist:/dist" -v "${SHOTS:-/tmp}:/shots" mingc/android-build-box bash -c "export PATH=\$PATH:/opt/android-sdk/platform-tools; $SNIP"
