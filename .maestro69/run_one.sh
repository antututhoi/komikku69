#!/usr/bin/env bash
# One app: install, first run (+ restore of the 20 titles for Mihon-type apps), then the deep walk.
set -u
name="$APP_NAME"
AAPT=$(ls -d "$ANDROID_HOME"/build-tools/*/ | sort -V | tail -1)aapt
kind=$(grep -i "^$name" .maestro69/apps.txt | head -1 | cut -d'|' -f3 | xargs)
out="$PWD/out69/$name"; mkdir -p "$out"
for t in 1 2 3 4 5 6 7 8 9 10; do
  adb shell mkdir -p /sdcard/Download/tour < /dev/null
  adb push seed.tachibk /sdcard/Download/tour/seed.tachibk < /dev/null > /dev/null 2>&1
  adb shell ls -l /sdcard/Download/tour/seed.tachibk < /dev/null 2>/dev/null | grep -q seed && break
  sleep 15
done
adb install -r -g ext_apks/mangadex.apk > out69/ext-install.txt 2>&1
apk="apks/$name.apk"
pkg=$("$AAPT" dump badging "$apk" 2>/dev/null | sed -n "s/^package: name='\([^']*\)'.*/\1/p")
echo "== $name ($pkg, $kind)"
adb install -r -g "$apk" > "$out/install.txt" 2>&1 || { cat "$out/install.txt"; exit 0; }
adb logcat -c
timeout 900 maestro test --test-output-dir "$out/m" -e APP_ID="$pkg" -e KIND="$kind" -e APP_NAME="$name" \
  "$GITHUB_WORKSPACE/.maestro69/tour_restore.yaml" < /dev/null > "$out/maestro.txt" 2>&1
adb shell am start -n "$(adb shell cmd package resolve-activity --brief "$pkg" < /dev/null | tail -1 | tr -d '\r')" < /dev/null > /dev/null 2>&1
sleep 4
python3 -I .maestro69/crawl.py "$out/crawl" "$name" "$kind" "$pkg" < /dev/null > "$out/crawl_stdout.txt" 2>&1
adb logcat -d -t 3000 > "$out/logcat.txt" 2>&1
