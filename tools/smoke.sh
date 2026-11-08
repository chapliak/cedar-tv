#!/usr/bin/env bash
# Automatic smoke test on the TV (adb). Every step starts from a freshly launched app, presses fixed keys, then ASSERTS the
# expected screen by reading the on-screen text (uiautomator dump), so a wrong key path is reported as a failure, not hidden.
# Then a random D-pad monkey and a crash/ANR scan.
#   TV=<tv-ip>:PORT [EVENTS=800] [SEED=7] bash tools/smoke.sh      (run only one at a time; the wireless-debug port can change)
# Output: tools/smoke-out/<time>/ (screenshots, result.txt, crash.log). Last line: "SMOKE RESULT: PASS|FAIL (n failures)".
# CAUTION: the monkey presses real buttons and may change app settings (subtitle language, screensaver time...).
cd "$(dirname "$0")/.."
SEED=${SEED:-7}; EVENTS=${EVENTS:-800}
OUT=tools/smoke-out/$(date +%m%d-%H%M%S); mkdir -p "$OUT"
SHOTS="$(pwd)/$OUT" bash tools/tv-adb.sh '
SEED='"$SEED"'; EVENTS='"$EVENTS"'
FAILS=0
fail(){ echo "  FAIL: $*"; FAILS=$((FAILS+1)); }
k(){ a shell input keyevent $1; sleep ${2:-0.7}; }
shot(){ a shell screencap -p /sdcard/$1.png; a pull /sdcard/$1.png /shots/$1.png >/dev/null; }
dump(){ a shell uiautomator dump /sdcard/ui.xml >/dev/null 2>&1; a shell cat /sdcard/ui.xml; }
# has "regex": is a text/desc matching the regex on screen (retries 3x)
has(){ for i in 1 2 3; do dump | grep -qE "(text|content-desc)=\"[^\"]*($1)[^\"]*\"" && return 0; sleep 1.2; done; return 1; }
expect(){ has "$1" && echo "  ok: $2" || { fail "$2 (no text /$1/)"; }; }
foreground(){ a shell dumpsys activity activities | grep -m1 topResumedActivity | grep -q app.cedartv; }
fresh(){ a shell am force-stop app.cedartv; a shell am start -n app.cedartv/.MainActivity >/dev/null; sleep 7; foreground || fail "app not in foreground after launch"; }
# step NAME  KEYS...   (keys: numbers = keyevent, w<sec> = wait)
press(){ for x in "$@"; do case $x in w*) sleep ${x#w};; *) k $x;; esac; done; }
tohome(){ for i in 1 2 3 4 5; do has "Latest Videos|Continue watching" && return 0; k 4 1.2; foreground || { fail "left the app while going back to Home"; fresh; return 1; }; done; has "Latest Videos|Continue watching"; }
a logcat -c
echo "== screens from Home"
fresh; shot 00-home; expect "Watch TV" "Home shows Watch TV"; expect "Library" "Home shows Library"
fresh; press 22 66 w2; shot 01-library;  has "Latest Videos" && fail "Library did not open" || echo "  ok: Library opened"; tohome && echo "  ok: Back to Home" || fail "Back from Library"
fresh; press 22 22 66 w2; shot 02-music; has "Latest Videos" && fail "Music did not open" || echo "  ok: Music opened"; tohome || fail "Back from Music"
fresh; press 22 22 22 66 w2; shot 03-karaoke; has "Latest Videos" && fail "Karaoke did not open" || echo "  ok: Karaoke opened"; tohome || fail "Back from Karaoke"
fresh; press 19 66 w2; shot 04-search; expect "Type to search" "Search opened"; tohome || fail "Back from Search"
fresh; press 19 22 66 w1.5; shot 05-cc-dropdown; expect "Translation only|Both languages" "CC dropdown opened"; press 4; has "Translation only" && fail "CC dropdown did not close"
fresh; press 19 22 22 66 w2.5; shot 06-language; expect "Search language" "Language screen opened"; press 4 w1.5; shot 07-after-language; expect "Latest Videos|Continue watching" "Back to Home from Language"
fresh; press 19 22 22 22 66 w2; shot 08-settings; expect "Screensaver" "Settings opened"; press 20 20 66 w1; shot 09-settings-item; foreground && echo "  ok: Settings item pressed" || fail "app died in Settings"; tohome || fail "Back from Settings"
echo "== video"
fresh; press 66 w10; shot 10-video; foreground || fail "app died starting a video"
# controls are hidden now: Right scrubs and focuses the time bar, Down goes to the CC button, OK opens the panel
press 22 20 66 w1.5; shot 12-subtitles-panel; expect "Subtitles" "Subtitles panel opened (scrub, Down, OK)"
press 66 w1.5; shot 13-subtitles-popup; expect "More languages" "Subtitle language popup opened"
press 4 4 w1; tohome || fail "Back from the video"
fresh; press 66 w10 22 20 22 66 w1.5; shot 14-video-settings; expect "Video settings" "Video settings panel opened"
press 20 66 w1.5; shot 15-audio-popup; expect "More languages" "Audio language popup opened"
press 4 4 w1; tohome || fail "Back from video settings"
echo "== random D-pad monkey ($EVENTS events, seed $SEED)"
fresh
a shell monkey -p app.cedartv -c android.intent.category.LEANBACK_LAUNCHER --pct-syskeys 0 --pct-appswitch 0 --pct-motion 0 --pct-touch 0 --pct-trackball 0 --pct-nav 45 --pct-majornav 50 --pct-anyevent 5 --throttle 250 -s $SEED -v $EVENTS > /tmp/monkey.txt 2>&1
grep -E "Events injected|Monkey finished|crashed|ANR|aborted" /tmp/monkey.txt | head -4
grep -qE "CRASH|ANR in|aborted" /tmp/monkey.txt && fail "monkey reported a crash/ANR/abort"
shot 99-after-monkey
a logcat -d -b crash > /shots/crash.log; a logcat -d -s AndroidRuntime:E ActivityManager:E > /shots/errors.log
grep -qE "FATAL|ANR in" /shots/crash.log /shots/errors.log && fail "crash/ANR in logcat (crash.log)"
[ $FAILS = 0 ] && echo "SMOKE RESULT: PASS" || echo "SMOKE RESULT: FAIL ($FAILS failures)"' 2>&1 | grep -vE "pulled|^\* daemon" | tee "$OUT/result.txt"
