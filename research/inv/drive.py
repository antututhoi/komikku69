#!/usr/bin/env python3
"""Scenario driver: each step = one block on the virtual phone. Shots are named <TAG>_<name>."""
import subprocess, sys, re, time, itertools
CNT = itertools.count(int(time.time()) % 100000)
def blk(*cmds, tag="x"):
    bid = f"{tag}{next(CNT)}"
    out = subprocess.run(["/home/claude/ctl.sh", bid, *cmds], capture_output=True, text=True).stdout
    return out
def texts(out): return re.findall(r"t='([^']*)'", out)
def has(out, s): return any(s == t for t in texts(out))

def fresh(pkg, tab="sleep 1"):
    return [f"stop {pkg}", f"launch {pkg}", "sleep 2", tab, "sleep 2"]

def lib_display(T, pkg, modes, overlays):
    """Library Display sheet: every display mode and every overlay box, each with a library screenshot."""
    for m in modes:
        blk(*fresh(pkg), "tap 881 213", "sleep 1", "tapt ^Display$", "sleep 1", f"tapt ^{m}$", "sleep 1",
            "tap 540 130", "sleep 2", f"shot {T}_libmode_{m}", tag=T)
    blk(*fresh(pkg), "tap 881 213", "sleep 1", "tapt ^Display$", "sleep 1", f"tapt ^{modes[0]}$", "tap 540 130", tag=T)
    for o in overlays:
        blk(*fresh(pkg), "tap 881 213", "sleep 1", "tapt ^Display$", "sleep 1", "up", f"tapt ^{o}$", "sleep 1",
            "tap 540 130", "sleep 2", f"shot {T}_libover_{o}_toggled", tag=T)
        blk(*fresh(pkg), "tap 881 213", "sleep 1", "tapt ^Display$", "sleep 1", "up", f"tapt ^{o}$", "tap 540 130", tag=T)

def run_lib_69k():
    blk(*fresh("app.komikku.mod69"), "shot K_lib_default", tag="K")
    lib_display("K", "app.komikku.mod69",
        ["Compact grid", "Comfortable grid", "List", "Cover-only grid", "Panorama comfortable grid"],
        ["Downloaded chapters", "Unread chapters", "Local source", "Language", "Use language icon", "Source icon",
         "Continue reading button", "Show category tabs", "Show number of items"])

if __name__ == "__main__":
    pass

def run_lib_69k():
    lib_display("K", "app.komikku.mod69",
        ["Compact grid", "Comfortable grid", "List", "Cover-only grid", "Panorama comfortable grid"],
        ["Downloaded chapters", "Unread chapters", "Local source", "Language", "Use language icon", "Source icon", "Continue reading button",
         "Show category tabs", "Show number of items"])

def reader_open(pkg, T, title_tab=(376, 358), cover=(540, 695), chapter="Vol.19 Ch.128"):
    blk(*fresh(pkg), f"tap {title_tab[0]} {title_tab[1]}", "sleep 2", f"tap {cover[0]} {cover[1]}", "sleep 5",
        "up", "sleep 1", f"tapt {chapter}", "sleep 8", "tapt ^Got it$", "sleep 1", tag=T)

def reader_option(T, tab, option, shotname, extra_up=0):
    """open reader menu → settings sheet → tab → option; close; screenshot the page"""
    ups = ["up"] * extra_up
    out = blk("tap 540 1200", "sleep 1", "tap 929 2275", "sleep 2", f"tapt ^{tab}$", "sleep 1", *ups,
              f"tapt ^{option}$", "sleep 1", "tap 540 250", "sleep 2", f"shot {T}_{shotname}", tag=T)
    return out

def run_reader_69k():
    T = "K"; pkg = "app.komikku.mod69"
    reader_open(pkg, T)
    for m in ["Paged (left to right)", "Paged (right to left)", "Paged (vertical)", "Long strip", "Long strip with gaps"]:
        reader_option(T, "Reading mode", m, f"rd_mode_{m}")
    reader_option(T, "Reading mode", "Paged \\(right to left\\)", "rd_mode_back_to_RTL")
    for s in ["Stretch", "Fit width", "Fit height", "Original size", "Smart fit", "Fit screen"]:
        reader_option(T, "Reading mode", s, f"rd_scale_{s}", extra_up=1)
    for z in ["L shaped", "Kindle-ish", "Edge", "Right and Left", "Default"]:
        reader_option(T, "Reading mode", z, f"rd_tapzone_{z}")
    for b in ["Gray", "White", "Auto", "Black"]:
        reader_option(T, "General", b, f"rd_bg_{b}")
    for t in ["Show page number", "Fullscreen", "Show page number", "Fullscreen"]:
        reader_option(T, "General", t, f"rd_toggle_{t}")
    for f in ["Grayscale", "Inverted", "Grayscale", "Inverted"]:
        reader_option(T, "Custom filter", f, f"rd_filter_{f}")

# ---- reader v2 (fixed: Settings found by label, bar differs per mode) ----
def rd2(T, tab, option, shot, ups=0, hide=False, pre=None):
    cmds = []
    if hide: cmds += ["tap 540 1200", "sleep 1"]
    cmds += ["tapt ^Settings$", "sleep 2"]
    if tab != "Reading mode": cmds += [f"tapt ^{tab}$", "sleep 1"]
    cmds += ["up"] * ups
    cmds += [f"tapt ^{option}$", "sleep 1", "tap 540 250", "sleep 2"]
    if hide: cmds += ["tap 540 1200", "sleep 2"]
    cmds += [f"shot {T}_{shot}"]
    return blk(*cmds, tag=T)

def run_reader_69k2(T="K", pkg="app.komikku.mod69", only=None):
    reader_open(pkg, T)
    blk("tap 540 1200", "sleep 1", tag=T)
    if only in (None, "mode"):
        for m in ["Paged \\(left to right\\)", "Paged \\(vertical\\)", "Long strip", "Long strip with gaps", "Paged \\(right to left\\)"]:
            rd2(T, "Reading mode", m, "rd_mode_" + re.sub(r"\W+", "_", m).strip("_"))
    if only in (None, "scale"):
        for s in ["Stretch", "Fit width", "Fit height", "Original size", "Smart fit", "Fit screen"]:
            rd2(T, "Reading mode", s, "rd_scale_" + s.replace(" ", "_"), ups=1)
    if only in (None, "tap"):
        for z in ["L shaped", "Kindle-ish", "Edge", "Right and Left", "Disabled", "Default"]:
            rd2(T, "Reading mode", z, "rd_tapzone_" + z.replace(" ", "_"), ups=1)
        for z in ["Horizontal", "Vertical", "Both", "None"]:
            rd2(T, "Reading mode", z, "rd_invert_" + z, ups=1)
    blk("tap 540 1200", "sleep 1", tag=T)
    if only in (None, "bg"):
        for b in ["Gray", "White", "Black", "Auto"]:
            rd2(T, "General", b, "rd_bg_" + b, hide=True)
    if only in (None, "tog"):
        for t in ["Show page number", "Fullscreen", "Keep screen on", "Flash on page change", "Always show chapter transition"]:
            for k in (1, 2):
                rd2(T, "General", t, f"rd_toggle_{t.replace(' ', '_')}_{'first_tap' if k == 1 else 'second_tap'}", hide=True)

def run_reader_other(T, pkg):
    reader_open(pkg, T)
    blk("sleep 3", tag=T)
    o = blk("tap 540 1200", "sleep 1", tag=T)
    if "Settings" not in o: blk("tap 540 1200", "sleep 1", tag=T)
    for m in ["Paged \\(left to right\\)", "Paged \\(vertical\\)", "Long strip", "Long strip with gaps", "Paged \\(right to left\\)"]:
        rd2(T, "Reading mode", m, "rd_mode_" + re.sub(r"\W+", "_", m).strip("_"))
    for s in ["Stretch", "Fit width", "Fit height", "Original size", "Smart fit", "Fit screen"]:
        rd2(T, "Reading mode", s, "rd_scale_" + s.replace(" ", "_"), ups=1)
    for z in ["L shaped", "Kindle-ish", "Edge", "Right and Left", "Disabled", "Default"]:
        rd2(T, "Reading mode", z, "rd_tapzone_" + z.replace(" ", "_"), ups=1)
    blk("tap 540 1200", "sleep 1", tag=T)
    for b in ["Gray", "White", "Black", "Auto"]:
        rd2(T, "General", b, "rd_bg_" + b, hide=True)
    for t in ["Show page number", "Fullscreen", "Keep screen on", "Flash on page change", "Always show chapter transition"]:
        for k in (1, 2):
            rd2(T, "General", t, f"rd_toggle_{t.replace(' ', '_')}_{'first_tap' if k == 1 else 'second_tap'}", hide=True)
