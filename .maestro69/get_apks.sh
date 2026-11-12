#!/usr/bin/env bash
# Downloads the newest APK of every app in apps.txt (prefers x86_64, then universal, then arm64).
set -u
mkdir -p apks out69
grep -v '^#' .maestro69/apps.txt | while IFS='|' read -r name repo kind; do
  name=$(echo "$name" | xargs); repo=$(echo "$repo" | xargs)
  [ -z "$name" ] && continue
  if [ "$repo" = "self" ]; then repo="$GITHUB_REPOSITORY"; rel="tags/mod69-latest"; else rel=""; fi
  if [ -n "$rel" ]; then
    json=$(gh api "repos/$repo/releases/$rel" 2>/dev/null)
  else
    json=$(gh api "repos/$repo/releases?per_page=10" --jq '[.[] | select(any(.assets[]; .name|endswith(".apk")))][0]' 2>/dev/null)
  fi
  url=$(echo "$json" | jq -r '
    [.assets[] | select(.name|endswith(".apk"))] as $a |
    ( [$a[] | select(.name|test("x86_64";"i"))] + [$a[] | select(.name|test("universal";"i"))]
      + [$a[] | select(.name|test("arm64";"i"))] + $a ) | .[0].browser_download_url // empty')
  tag=$(echo "$json" | jq -r '.tag_name // "?"')
  if [ -z "$url" ]; then echo "$name: no APK found" | tee -a out69/apps-found.txt; continue; fi
  curl -sSL -o "apks/$name.apk" "$url" && echo "$name: $tag $(basename "$url")" | tee -a out69/apps-found.txt
done

