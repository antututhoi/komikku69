#!/usr/bin/env bash
# usage: [LANE=b] ctl.sh ID "cmd1" "cmd2" ...   -> sends a block, waits for the phone, prints the screen list
id="$1"; shift
L="${LANE:-}"; S=""; [ -n "$L" ] && S="-$L"
cd /home/claude/ctlcmd$L
{ echo "=== $id"; for c in "$@"; do echo "$c"; done; } >> cmd.txt
git add -A >/dev/null; git -c user.name=claude -c user.email=noreply@anthropic.com commit -qm "$id" >/dev/null
git push -q -f origin HEAD:ctl69-cmd$S 2>/dev/null
O=/home/claude/ctlout$L
[ -d $O/.git ] || git clone -q --depth 1 --branch ctl69-out$S https://github.com/antututhoi/komikku69 $O 2>/dev/null
for i in $(seq 1 120); do
  sleep 4
  git -C $O fetch -q --depth 1 origin ctl69-out$S 2>/dev/null && git -C $O reset -q --hard FETCH_HEAD 2>/dev/null
  [ -f $O/out/$id.txt ] && { cat $O/out/$id.txt; exit 0; }
done
echo "TIMEOUT waiting for $id"
