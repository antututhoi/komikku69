#!/usr/bin/env python3
"""Walks one app on the virtual phone: every settings page (scrolled to the end), every menu, library/entry/reader layouts.
Usage: crawl.py OUT_DIR APP_NAME KIND   (KIND = mihon | kotatsu)"""
import subprocess, sys, os, re, time, hashlib, xml.etree.ElementTree as ET

OUT, APP, KIND = sys.argv[1], sys.argv[2], sys.argv[3]
PKG = sys.argv[4] if len(sys.argv) > 4 else ""
os.makedirs(OUT, exist_ok=True)
T0 = time.time(); BUDGET = 70 * 60
shots = 0; log = open(os.path.join(OUT, "crawl.txt"), "w")
W, H = 1080, 2400
SKIP = re.compile(r"clear|delete|reset|log ?out|sign ?out|remove|wipe|uninstall|factory|biometric|app lock|secure screen|incognito|check for update|install|battery|crash|create backup|restore|erase|purge|cache|empty|stop|quit|exit", re.I)

def L(*a):
    s = " ".join(str(x) for x in a); print(s, flush=True); log.write(s + "\n"); log.flush()
def adb(*a, out=False):
    r = subprocess.run(["adb", *a], stdin=subprocess.DEVNULL, capture_output=True, timeout=60)
    return r.stdout if out else None
def over(): return time.time() - T0 > BUDGET
def tap(x, y): adb("shell", "input", "tap", str(int(x)), str(int(y))); time.sleep(1.3)
def hold(x, y): adb("shell", "input", "swipe", str(int(x)), str(int(y)), str(int(x)), str(int(y)), "900"); time.sleep(1.3)
def back():
    adb("shell", "input", "keyevent", "4"); time.sleep(1.2); ensure_app()
def ensure_app():
    if not PKG: return
    f = (adb("shell", "dumpsys", "window", out=True) or b"").decode(errors="ignore")
    m = re.search(r"mCurrentFocus=.*", f)
    if m and PKG not in m.group(0):
        L("left the app (", m.group(0)[:80], ") - relaunching")
        adb("shell", "monkey", "-p", PKG, "-c", "android.intent.category.LAUNCHER", "1"); time.sleep(4)
def up():   adb("shell", "input", "swipe", "540", "1700", "540", "700", "350"); time.sleep(0.9)
def down(): adb("shell", "input", "swipe", "540", "700", "540", "1900", "350"); time.sleep(0.9)
def shot(name):
    global shots
    if shots > 420: return
    shots += 1
    png = adb("exec-out", "screencap", "-p", out=True)
    open(os.path.join(OUT, re.sub(r"[^A-Za-z0-9_.-]+", "_", name)[:90] + ".png"), "wb").write(png)

class N:
    def __init__(s, e, kids): s.e, s.kids = e, kids
def dump():
    for _ in range(4):
        adb("shell", "uiautomator", "dump", "/sdcard/u.xml"); x = adb("exec-out", "cat", "/sdcard/u.xml", out=True)
        try: root = ET.fromstring(x)
        except Exception: time.sleep(1); continue
        t = texts(root)
        if any("isn't responding" in v or "keeps stopping" in v for v in t):
            for n in root.iter("node"):
                if (n.get("text") or "").strip() in ("Wait", "OK", "Close app"):
                    if (n.get("text") or "").strip() == "Wait":
                        x1, y1, x2, y2 = bounds(n); adb("shell", "input", "tap", str((x1 + x2) // 2), str((y1 + y2) // 2)); time.sleep(1.5); break
            continue
        return root
    return None
def bounds(e):
    m = re.match(r"\[(\d+),(\d+)\]\[(\d+),(\d+)\]", e.get("bounds", "")); return tuple(map(int, m.groups())) if m else (0, 0, 0, 0)
def texts(e):
    r = []
    for n in e.iter("node"):
        t = (n.get("text") or n.get("content-desc") or "").strip()
        if t: r.append(t)
    return r
def sig(root):
    if root is None: return ""
    return hashlib.md5("|".join(texts(root)).encode()).hexdigest()
def rows(root, top=300, bottom=2100):
    """clickable rows in the main area, each with its title text"""
    res = []
    if root is None: return res
    for n in root.iter("node"):
        if n.get("clickable") != "true": continue
        x1, y1, x2, y2 = bounds(n)
        if y2 < top or y1 > bottom or x2 - x1 < 200: continue
        t = texts(n)
        if not t: continue
        res.append(dict(title=t[0], all=t, b=(x1, y1, x2, y2), checkable=n.get("checkable") == "true" or any(k.get("checkable") == "true" for k in n.iter("node"))))
    return res
def topbar(root):
    res = []
    if root is None: return res
    for n in root.iter("node"):
        if n.get("clickable") != "true": continue
        x1, y1, x2, y2 = bounds(n)
        if y2 <= 340 and y1 >= 60 and x2 - x1 < 400:
            res.append(((x1 + x2) // 2, (y1 + y2) // 2, (n.get("content-desc") or " ".join(texts(n)) or "icon")))
    return res
def is_dialog(root):
    t = [x.lower() for x in texts(root)] if root is not None else []
    return ("cancel" in t or "ok" in t or "dismiss" in t) and len(t) < 40

def page_scroll(prefix, maxn=7):
    """screenshot the current page, then scroll down until it stops changing"""
    last = None
    for i in range(maxn):
        r = dump(); s = sig(r)
        if s == last: break
        shot(f"{prefix}_p{i}"); last = s
        up()
    for _ in range(maxn): down()

def settle_top():
    for _ in range(6): down()

def find_and_tap(title, root=None):
    """scroll the current list from the top until `title` is visible, then tap it"""
    settle_top()
    last = None
    for _ in range(10):
        r = dump()
        for row in rows(r):
            if row["title"] == title:
                x1, y1, x2, y2 = row["b"]; tap((x1 + x2) // 2, (y1 + y2) // 2); return True
        s = sig(r)
        if s == last: break
        last = s; up()
    return False

def list_entries():
    """all clickable settings rows of the current page (scrolls to the end)"""
    settle_top(); seen = []; last = None
    for _ in range(10):
        r = dump()
        for row in rows(r):
            if row["title"] not in [e["title"] for e in seen]: seen.append(row)
        s = sig(r)
        if s == last: break
        last = s; up()
    settle_top()
    return seen

def on_page(titles):
    """true when most of the page's first entries are visible at the top"""
    settle_top(); vis = set(texts(dump())); first = titles[:4]
    return sum(1 for t in first if t in vis) >= max(1, len(first) // 2)

def goto(path):
    """restart the app, open settings and follow the given entries (robust against getting lost)"""
    adb("shell", "am", "force-stop", PKG); time.sleep(1)
    adb("shell", "monkey", "-p", PKG, "-c", "android.intent.category.LAUNCHER", "1"); time.sleep(5)
    if not open_settings(): return False
    time.sleep(1.5)
    for t in path:
        if not find_and_tap(t): return False
        time.sleep(1.5)
    return True

def crawl_page(path, depth=0, maxdepth=1):
    """open every entry of the settings page at `path`; screenshot each one (scrolled to the end)"""
    if not goto(path): L("cannot reach", path); shot("lost_" + "_".join(path)); return
    if depth == 0: shot("settings_main")
    ents = list_entries(); titles = [e["title"] for e in ents]; L("  " * depth, path or "settings", "entries:", titles)
    for i, e in enumerate(ents):
        if over(): return
        if SKIP.search(e["title"]) or e["checkable"]: L("  " * depth, "skip", e["title"]); continue
        if i and not goto(path): L("cannot reach", path); return
        if not find_and_tap(e["title"]): L("  " * depth, "cannot find", e["title"]); continue
        name = "settings_" + "_".join(path + [f"{i:02d}", e["title"]])
        r = dump()
        if is_dialog(r):
            shot(name + "_dialog"); continue
        if on_page(titles): continue   # nothing opened (a toggle or a chip)
        page_scroll(name)
        if depth < maxdepth and rows(dump()):
            crawl_page(path + [e["title"]], depth + 1, maxdepth)

def on_page(titles):
    """true when most of the page's first entries are visible at the top"""
    settle_top(); vis = set(texts(dump())); first = titles[:4]
    return sum(1 for t in first if t in vis) >= max(1, len(first) // 2)

def open_settings():
    L("open settings")
    r = dump()
    def tap_text(rx):
        for n in dump().iter("node"):
            t = n.get("text") or n.get("content-desc") or ""
            if re.fullmatch(rx, t.strip(), re.I):
                x1, y1, x2, y2 = bounds(n); tap((x1 + x2) // 2, (y1 + y2) // 2); return True
        return False
    if APP == "DropSauce":
        tap(90, 200); return True
    if APP == "Kototoro":
        tap_text("home") ; time.sleep(1)
        return tap_text("settings")
    for rx in ("more", "more options|menu|more menu"):
        if tap_text(rx): break
    if tap_text("settings"): return True
    for _ in range(4):
        up()
        if tap_text("settings"): return True
    return False

def menus(prefix):
    """tap every icon in the top bar; if it opens a menu, open each menu item too"""
    r = dump(); base = sig(r); base_titles = set(texts(r))
    for i, (x, y, d) in enumerate(topbar(r)):
        if over() or re.search(r"navigate up|back|^$", d, re.I): continue
        tap(x, y); rr = dump()
        shot(f"{prefix}_menu{i}_{d}")
        items = [row for row in rows(rr, top=0, bottom=2400) if row["title"] not in base_titles][:12]
        back(); ensure_app()
        for j, it in enumerate(items):
            if over() or SKIP.search(it["title"]) or it["checkable"]: continue
            tap(x, y); rr = dump(); hit = None
            for row in rows(rr, top=0, bottom=2400):
                if row["title"] == it["title"]: hit = row
            if not hit: back(); continue
            x1, y1, x2, y2 = hit["b"]; tap((x1 + x2) // 2, (y1 + y2) // 2)
            nm = f"{prefix}_menu{i}_{d}_item{j}_{it['title']}"
            r3 = dump()
            if sig(r3) != base:
                page_scroll(nm, 3)
                back(); ensure_app()
                if sig(dump()) != base: back()

def main_tabs():
    r = dump(); res = []
    for n in r.iter("node"):
        if n.get("clickable") == "true":
            x1, y1, x2, y2 = bounds(n)
            if y1 > 2100 and x2 - x1 < 400: res.append(((x1 + x2) // 2, (y1 + y2) // 2, n.get("content-desc") or " ".join(texts(n)) or "tab"))
    return res

def tour_screens():
    names = ["Library", "Updates", "History", "Browse", "More"]
    tabs = [(108 + 216 * i, 2232, names[i]) for i in range(5)] if KIND == "mihon" else main_tabs()
    L("tabs", tabs)
    for i, (x, y, d) in enumerate(tabs):
        if over(): return
        tap(x, y); time.sleep(1.5)
        p = f"tab{i}_{d}"; page_scroll(p, 4); menus(p)
        if re.search("library|favou?rites|home", d, re.I) and KIND == "mihon":
            tap(108, 2232); time.sleep(1.5); tap(270, 900); time.sleep(6); shot(p + "_entry"); page_scroll(p + "_entry", 8); menus(p + "_entry")
            # chapter list actions: long-press a chapter row
            hold(540, 1800); shot(p + "_entry_select"); back()
            # read
            for rx in ("start|resume|read|continue|start reading",):
                for n in dump().iter("node"):
                    t = (n.get("text") or n.get("content-desc") or "").strip()
                    if re.fullmatch(rx, t, re.I):
                        x1, y1, x2, y2 = bounds(n); tap((x1 + x2) // 2, (y1 + y2) // 2); break
            time.sleep(12)
            for n in dump().iter("node"):
                if (n.get("text") or "").strip().lower() == "got it":
                    x1, y1, x2, y2 = bounds(n); tap((x1 + x2) // 2, (y1 + y2) // 2); break
            shot(p + "_reader"); tap(540, 1200); time.sleep(1.2); shot(p + "_reader_menu")
            # every icon in the reader bars
            r = dump(); base = sig(r)
            for j, (bx, by, bd) in enumerate([t for t in topbar(r)] + [t for t in main_tabs()]):
                if over(): break
                tap(bx, by); shot(f"{p}_reader_icon{j}_{bd}")
                # sheet tabs
                r2 = dump()
                for k in range(2):
                    adb("shell", "input", "swipe", "900", "1800", "200", "1800", "300"); time.sleep(1); shot(f"{p}_reader_icon{j}_{bd}_tab{k+2}")
                up(); shot(f"{p}_reader_icon{j}_{bd}_scrolled")
                back(); time.sleep(0.8)
                if sig(dump()) != base:
                    tap(540, 1200); time.sleep(1)
            back(); back(); back()
        elif i >= 1 and KIND == "kotatsu":
            pass

L("start", APP, KIND)
try:
    ensure_app()
    crawl_page([], 0)
except Exception as e:
    L("settings walk failed", repr(e))
if KIND == "mihon":
    try:
        goto([]); back(); ensure_app(); tour_screens()
    except Exception as e: L("screens failed", repr(e))
L("done", shots, "screenshots", int(time.time() - T0), "s")
