#!/usr/bin/env python3
"""69 remote control for the virtual phone.
Polls branch ctl69-cmd (file cmd.txt) for new command batches, runs them with adb,
and pushes a screenshot + a readable screen list after each batch to branch ctl69-out.

cmd.txt format: blocks separated by lines '=== <id>' ; each line in a block is one command:
  tap X Y | tapt REGEX [n] | longt REGEX | hold X Y [ms] | swipe X1 Y1 X2 Y2 [ms] | up | down | left | right
  text STRING | key CODE | back | home | sleep SECONDS | rotate 0|1 | launch PKG | stop PKG
  shot NAME (extra named screenshot) | rec NAME SECONDS (screen video) | sh ADB-SHELL-COMMAND | quit
After every block: out/<id>.jpg, out/<id>.txt (visible items with tap points), out/state.txt
"""
import os, re, subprocess, sys, time, xml.etree.ElementTree as ET

REPO = os.environ["GITHUB_REPOSITORY"]; TOKEN = os.environ["TOKEN"]
URL = f"https://x-access-token:{TOKEN}@github.com/{REPO}"
OUT = "/tmp/ctlout"; CMD = "/tmp/ctlcmd"
DEADLINE = time.time() + float(os.environ.get("CTL_MINUTES", "330")) * 60

def run(*a, timeout=120, inp=None):
    try:
        r = subprocess.run(list(a), capture_output=True, timeout=timeout, input=inp)
        return r.stdout
    except Exception as e:
        return str(e).encode()
def adb(*a, timeout=60): return run("adb", *a, timeout=timeout)
def git(d, *a): return run("git", "-C", d, *a, timeout=120)

def setup():
    os.makedirs(OUT, exist_ok=True)
    git(OUT, "init", "-q"); git(OUT, "checkout", "-q", "-b", "ctl69-out")
    git(OUT, "config", "user.name", "ctl69"); git(OUT, "config", "user.email", "ctl69@users.noreply.github.com")
    os.makedirs(OUT + "/out", exist_ok=True)
    run("git", "clone", "-q", "--depth", "1", "--branch", "ctl69-cmd", URL, CMD)

def bounds(e):
    m = re.match(r"\[(\d+),(\d+)\]\[(\d+),(\d+)\]", e.get("bounds", ""))
    return tuple(map(int, m.groups())) if m else (0, 0, 0, 0)

def dump():
    for _ in range(3):
        adb("shell", "uiautomator", "dump", "/sdcard/u.xml")
        x = adb("exec-out", "cat", "/sdcard/u.xml")
        try: return ET.fromstring(x)
        except Exception: time.sleep(1)
    return None

def items(root):
    """readable list: [C]=clickable [L]=long-clickable [S]=scrollable [x]=checked; text, desc, id, centre point"""
    res = []
    if root is None: return res
    for n in root.iter("node"):
        t = (n.get("text") or "").strip(); d = (n.get("content-desc") or "").strip()
        rid = (n.get("resource-id") or "").split("/")[-1]
        flags = ("C" if n.get("clickable") == "true" else "") + ("L" if n.get("long-clickable") == "true" else "") \
            + ("S" if n.get("scrollable") == "true" else "") + ("x" if n.get("checked") == "true" else "") \
            + ("k" if n.get("checkable") == "true" else "")
        if not (t or d or flags): continue
        x1, y1, x2, y2 = bounds(n)
        if x2 <= x1 or y2 <= y1: continue
        res.append(dict(t=t, d=d, id=rid, f=flags, x=(x1 + x2) // 2, y=(y1 + y2) // 2, b=(x1, y1, x2, y2), node=n))
    return res

def handle_anr(root):
    for it in items(root):
        if it["t"] == "Wait" and any("isn't responding" in j["t"] for j in items(root)):
            adb("shell", "input", "tap", str(it["x"]), str(it["y"])); time.sleep(1); return True
    return False

def find(regex, root=None, n=0):
    root = root if root is not None else dump()
    hits = [it for it in items(root) if re.search(regex, it["t"], re.I) or re.search(regex, it["d"], re.I)]
    # prefer the clickable element itself or the nearest clickable parent area
    return hits[n] if len(hits) > n else None

def screenshot(path):
    png = adb("exec-out", "screencap", "-p", timeout=60)
    open("/tmp/s.png", "wb").write(png)
    run("ffmpeg", "-nostdin", "-loglevel", "error", "-y", "-i", "/tmp/s.png", "-vf", "scale=540:-1", "-q:v", "4", path)

def focus():
    f = adb("shell", "dumpsys", "window").decode(errors="ignore")
    m = re.search(r"mCurrentFocus=Window\{\S+ \S+ ([^}]*)\}", f)
    return m.group(1) if m else "?"

def do(cmd, bid, log):
    p = cmd.split(None, 1); op = p[0].lower() if p else ""; arg = p[1] if len(p) > 1 else ""
    a = arg.split()
    if op == "tap": adb("shell", "input", "tap", a[0], a[1]); time.sleep(1.2)
    elif op in ("tapt", "longt"):
        m = re.match(r"(.*?)(?:\s+(\d+))?$", arg); it = find(m.group(1), n=int(m.group(2) or 0))
        if not it: log.append(f"NOT FOUND: {arg}"); return
        if op == "tapt": adb("shell", "input", "tap", str(it["x"]), str(it["y"]))
        else: adb("shell", "input", "swipe", str(it["x"]), str(it["y"]), str(it["x"]), str(it["y"]), "900")
        log.append(f"{op} '{it['t'] or it['d']}' at {it['x']},{it['y']}"); time.sleep(1.3)
    elif op == "hold": adb("shell", "input", "swipe", a[0], a[1], a[0], a[1], a[2] if len(a) > 2 else "900"); time.sleep(1.3)
    elif op == "swipe": adb("shell", "input", "swipe", *a[:4], a[4] if len(a) > 4 else "300"); time.sleep(1)
    elif op == "up": adb("shell", "input", "swipe", "540", "1700", "540", "750", "300"); time.sleep(1)
    elif op == "down": adb("shell", "input", "swipe", "540", "750", "540", "1700", "300"); time.sleep(1)
    elif op == "left": adb("shell", "input", "swipe", "950", "1300", "130", "1300", "250"); time.sleep(1)
    elif op == "right": adb("shell", "input", "swipe", "130", "1300", "950", "1300", "250"); time.sleep(1)
    elif op == "text": adb("shell", "input", "text", arg.replace(" ", "%s")); time.sleep(0.8)
    elif op == "key": adb("shell", "input", "keyevent", arg); time.sleep(1)
    elif op == "back": adb("shell", "input", "keyevent", "4"); time.sleep(1.2)
    elif op == "home": adb("shell", "input", "keyevent", "3"); time.sleep(1)
    elif op == "sleep": time.sleep(float(arg or 2))
    elif op == "rotate":
        adb("shell", "settings", "put", "system", "accelerometer_rotation", "0")
        adb("shell", "settings", "put", "system", "user_rotation", arg); time.sleep(2.5)
    elif op == "launch":
        adb("shell", "monkey", "-p", arg, "-c", "android.intent.category.LAUNCHER", "1"); time.sleep(5)
    elif op == "stop": adb("shell", "am", "force-stop", arg); time.sleep(1)
    elif op == "shot": screenshot(f"{OUT}/out/{bid}_{re.sub(r'[^A-Za-z0-9_-]+', '_', arg)}.jpg")
    elif op == "rec":
        name, secs = a[0], a[1] if len(a) > 1 else "8"
        adb("shell", "screenrecord", "--time-limit", secs, "--bit-rate", "2000000", "/sdcard/r.mp4", timeout=int(secs) + 30)
        adb("pull", "/sdcard/r.mp4", "/tmp/r.mp4")
        os.makedirs(f"{OUT}/out/{bid}_{name}", exist_ok=True)
        run("ffmpeg", "-nostdin", "-loglevel", "error", "-i", "/tmp/r.mp4", "-vf", "fps=4,scale=360:-1", "-q:v", "6",
            f"{OUT}/out/{bid}_{name}/f_%03d.jpg")
    elif op == "sh": log.append(adb("shell", arg, timeout=180).decode(errors="ignore")[-3000:])
    elif op == "host": log.append(run("bash", "-c", arg, timeout=900).decode(errors="ignore")[-3000:])
    elif op == "quit": raise SystemExit
    else: log.append(f"UNKNOWN: {cmd}")

def report(bid, log):
    root = dump()
    if handle_anr(root): root = dump()
    screenshot(f"{OUT}/out/{bid}.jpg")
    lines = [f"# block {bid}  focus: {focus()}  time: {time.strftime('%H:%M:%S')}"] + [f"! {l}" for l in log]
    for it in items(root):
        lines.append(f"[{it['f']:<4}] {it['x']:>4},{it['y']:<4} t='{it['t'][:80]}' d='{it['d'][:60]}' id={it['id']}")
    open(f"{OUT}/out/{bid}.txt", "w").write("\n".join(lines) + "\n")
    open(f"{OUT}/out/state.txt", "w").write(f"last {bid}\n")
    git(OUT, "add", "-A"); git(OUT, "commit", "-qm", f"block {bid}")
    for _ in range(3):
        r = subprocess.run(["git", "-C", OUT, "push", "-q", "-f", URL, "ctl69-out"], capture_output=True)
        if r.returncode == 0: break
        time.sleep(2)

def main():
    setup()
    done = set()
    report("start", ["agent ready"])
    while time.time() < DEADLINE:
        git(CMD, "fetch", "-q", "--depth", "1", "origin", "ctl69-cmd"); git(CMD, "reset", "-q", "--hard", "FETCH_HEAD")
        try: txt = open(CMD + "/cmd.txt").read()
        except Exception: txt = ""
        for blk in re.split(r"^=== ", txt, flags=re.M)[1:]:
            head, *body = blk.split("\n"); bid = head.strip()
            if not bid or bid in done: continue
            done.add(bid); log = []
            try:
                for c in body:
                    c = c.strip()
                    if c and not c.startswith("#"): do(c, bid, log)
            except SystemExit:
                report(bid, log + ["quit"]); return
            except Exception as e:
                log.append(f"ERROR {e!r}")
            report(bid, log)
        time.sleep(3)

if __name__ == "__main__":
    main()
