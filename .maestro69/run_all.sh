#!/usr/bin/env bash
# Installs each downloaded app on the virtual phone, runs the tour, saves screenshots, a video and the log.
set -u
AAPT=$(ls -d "$ANDROID_HOME"/build-tools/*/ | sort -V | tail -1)aapt
adb shell settings put global window_animation_scale 0
for apk in apks/*.apk; do
  name=$(basename "$apk" .apk)
  out="$PWD/out69/$name"; mkdir -p "$out"
  pkg=$("$AAPT" dump badging "$apk" 2>/dev/null | sed -n "s/^package: name='\([^']*\)'.*/\1/p")
  echo "== $name ($pkg)"
  if ! adb install -r -g "$apk" > "$out/install.txt" 2>&1; then echo "install failed"; cat "$out/install.txt"; continue; fi
  adb logcat -c
  adb shell screenrecord --bit-rate 2000000 --time-limit 170 /sdcard/tour.mp4 &
  rec=$!
  ( cd "$out" && timeout 600 maestro test -e APP_ID="$pkg" "$GITHUB_WORKSPACE/.maestro69/tour.yaml" > "$out/maestro.txt" 2>&1 )
  adb shell pkill -INT screenrecord; wait $rec 2>/dev/null; sleep 2
  adb pull /sdcard/tour.mp4 "$out/tour.mp4" > /dev/null 2>&1; adb shell rm -f /sdcard/tour.mp4
  adb logcat -d -t 3000 > "$out/logcat.txt" 2>&1
  adb uninstall "$pkg" > /dev/null 2>&1
done
