#!/usr/bin/env bash
# Prepares the virtual phone (backup file, MangaDex extension, the session's apps) then starts remote control.
set -u
adb shell settings put global hide_error_dialogs 1 < /dev/null
for t in 1 2 3 4 5 6 7 8 9 10; do
  adb shell mkdir -p /sdcard/Download/tour < /dev/null
  adb push seed.tachibk /sdcard/Download/tour/seed.tachibk < /dev/null > /dev/null 2>&1
  adb push seed_kotatsu.zip /sdcard/Download/tour/seed_kotatsu.zip < /dev/null > /dev/null 2>&1
  adb shell ls /sdcard/Download/tour/seed.tachibk < /dev/null 2>/dev/null | grep -q seed && break
  sleep 15
done
adb install -r -g ext_apks/mangadex.apk < /dev/null
for f in apks/*.apk; do echo "install $f"; adb install -r -g "$f" < /dev/null; done
adb shell pm list packages -3 < /dev/null
python3 -u .research69/agent.py
