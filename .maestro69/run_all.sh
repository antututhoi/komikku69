#!/usr/bin/env bash
# Installs each app on the virtual phone, runs the tour, saves screenshots, video and log.
set -u
AAPT=$(ls -d "$ANDROID_HOME"/build-tools/*/ | sort -V | tail -1)aapt
# wait until the phone's storage is ready, then copy the backup in and check it's really there
for t in 1 2 3 4 5 6 7 8 9 10; do
  adb shell mkdir -p /sdcard/Download/tour < /dev/null
  adb push seed.tachibk /sdcard/Download/tour/seed.tachibk < /dev/null > /dev/null 2>&1
  if adb shell ls -l /sdcard/Download/tour/seed.tachibk < /dev/null 2>/dev/null | grep -q seed; then echo "backup copied (try $t)"; break; fi
  echo "backup copy failed (try $t), waiting"; sleep 15
done
adb shell ls -l /sdcard/Download/tour < /dev/null
adb install -r -g ext_apks/mangadex.apk > out69/ext-install.txt 2>&1
while IFS='|' read -r name repo kind <&3; do
  case "$name" in \#*|'') continue;; esac
  name=$(echo "$name" | xargs); kind=$(echo "$kind" | xargs)
  apk="apks/$name.apk"; [ -f "$apk" ] || continue
  out="$PWD/out69/$name"; mkdir -p "$out"
  pkg=$("$AAPT" dump badging "$apk" 2>/dev/null | sed -n "s/^package: name='\([^']*\)'.*/\1/p")
  echo "== $name ($pkg, $kind)"
  if ! adb install -r -g "$apk" > "$out/install.txt" 2>&1; then cat "$out/install.txt"; continue; fi
  adb logcat -c
  ( for k in 1 2 3 4 5; do adb shell screenrecord --bit-rate 1500000 --time-limit 175 /sdcard/tour_$k.mp4 || break; done ) &
  rec=$!
  timeout 900 maestro test --test-output-dir "$out/m" -e APP_ID="$pkg" -e KIND="$kind" \
    "$GITHUB_WORKSPACE/.maestro69/tour.yaml" < /dev/null > "$out/maestro.txt" 2>&1
  kill $rec 2>/dev/null; adb shell pkill -INT screenrecord; sleep 3
  for k in 1 2 3 4 5; do adb pull /sdcard/tour_$k.mp4 "$out/tour_$k.mp4" > /dev/null 2>&1; done
  adb shell rm -f /sdcard/tour_*.mp4
  adb logcat -d -t 4000 > "$out/logcat.txt" 2>&1
  adb uninstall "$pkg" < /dev/null > /dev/null 2>&1
  adb shell ls /sdcard/Download/tour/seed.tachibk < /dev/null > /dev/null 2>&1 || adb push seed.tachibk /sdcard/Download/tour/seed.tachibk < /dev/null > /dev/null 2>&1
done 3< .maestro69/apps.txt
