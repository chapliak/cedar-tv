#!/usr/bin/env bash
# Scroll benchmark on the TV: Home screen, DOWN x6 then RIGHT x12 with short pauses; prints jank stats.
# Usage: TV=IP:PORT bash tools/bench.sh   (adb input itself loads the CPU, compare runs only with each other)
cd "$(dirname "$0")/.."
SHOTS=/tmp bash tools/tv-adb.sh 'a shell am force-stop app.cedartv; a shell am start -n app.cedartv/.MainActivity >/dev/null; sleep 8;
a shell dumpsys gfxinfo app.cedartv reset >/dev/null
for i in 1 2 3 4 5 6; do a shell input keyevent 20; sleep 0.45; done
for i in 1 2 3 4 5 6 7 8 9 10 11 12; do a shell input keyevent 22; sleep 0.3; done
for i in 1 2 3 4 5 6; do a shell input keyevent 19; sleep 0.45; done
a shell dumpsys gfxinfo app.cedartv | grep -E "Total frames|Janky|50th|90th|95th|99th|Number Missed|Number High"'
