#!/usr/bin/env bash
# Exploration run for the 4 Kotatsu-type apps: install, first-run screens, search, dump screen structure.
set -u
AAPT=$(ls -d "$ANDROID_HOME"/build-tools/*/ | sort -V | tail -1)aapt
while IFS='|' read -r name repo kind <&3; do
  case "$name" in \#*|'') continue;; esac
  name=$(echo "$name" | xargs)
  apk="apks/$name.apk"; [ -f "$apk" ] || continue
  out="$PWD/out69/$name"; mkdir -p "$out"
  pkg=$("$AAPT" dump badging "$apk" 2>/dev/null | sed -n "s/^package: name='\([^']*\)'.*/\1/p")
  echo "== $name ($pkg)"
  adb install -r -g "$apk" > "$out/install.txt" 2>&1 || { cat "$out/install.txt"; continue; }
  adb logcat -c
  for f in k1 k2 k3; do
    timeout 600 maestro test --test-output-dir "$out/m_$f" -e APP_ID="$pkg" -e APP_NAME="$name" \
      "$GITHUB_WORKSPACE/.maestro69/$f.yaml" < /dev/null > "$out/maestro_$f.txt" 2>&1
    adb shell uiautomator dump /sdcard/ui.xml < /dev/null > /dev/null 2>&1
    adb pull /sdcard/ui.xml "$out/ui_$f.xml" > /dev/null 2>&1
  done
  adb logcat -d -t 2000 > "$out/logcat.txt" 2>&1
  adb uninstall "$pkg" < /dev/null > /dev/null 2>&1
done 3< .maestro69/apps.txt
