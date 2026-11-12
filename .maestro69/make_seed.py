#!/usr/bin/env python3
"""Makes seed.tachibk: 20 popular ongoing MangaDex titles (2 categories) for Mihon-type apps,
and downloads the MangaDex extension. Runs on GitHub (needs internet)."""
import gzip, json, os, time, urllib.request, urllib.parse

def get(url):
    req = urllib.request.Request(url, headers={"User-Agent": "69-tour/1.0"})
    with urllib.request.urlopen(req, timeout=60) as r:
        return r.read()

# --- protobuf writer (just what the backup needs) ---
def varint(n):
    n &= (1 << 64) - 1
    out = bytearray()
    while True:
        b = n & 0x7F; n >>= 7
        out.append(b | (0x80 if n else 0))
        if not n: return bytes(out)
def key(f, wt): return varint((f << 3) | wt)
def f_int(f, v): return key(f, 0) + varint(v)
def f_bytes(f, b): return key(f, 2) + varint(len(b)) + b
def f_str(f, s): return f_bytes(f, s.encode())

# --- MangaDex extension + repo from the keiyoushi index (protobuf index.pb) ---
REPO = "https://raw.githubusercontent.com/keiyoushi/extensions/repo"
def rv(b, i):
    n = sh = 0
    while True:
        c = b[i]; i += 1; n |= (c & 0x7F) << sh; sh += 7
        if c < 0x80: return n, i
def parse(b):
    i = 0; out = []
    while i < len(b):
        k, i = rv(b, i); f, wt = k >> 3, k & 7
        if wt == 0: v, i = rv(b, i)
        elif wt == 2:
            l, i = rv(b, i); v = b[i:i + l]; i += l
        elif wt == 1: v = b[i:i + 8]; i += 8
        elif wt == 5: v = b[i:i + 4]; i += 4
        else: raise ValueError(wt)
        out.append((f, wt, v))
    return out
index = gzip.decompress(get(REPO + "/index.pb"))
def entries(b):
    for f, wt, v in parse(b):
        if wt != 2 or b"eu.kanade.tachiyomi.extension." not in v: continue
        try: kids = parse(v)
        except Exception: continue
        if any(ff == 2 and ww == 2 and vv.startswith(b"eu.kanade.tachiyomi.extension.") for ff, ww, vv in kids):
            yield v
        else:
            yield from entries(v)
entry = next(v for v in entries(index) if b"extension.all.mangadex\x1a" in v)
fields = parse(entry)
apk_url = next(parse(v)[0][2].decode() for f, wt, v in fields if f == 3)
source_id = None
for f, wt, v in fields:
    if f == 8:
        sub = {ff: vv for ff, ww, vv in parse(v)}
        if sub.get(3) == b"en":
            source_id = sub[1]
            if source_id >= 1 << 63: source_id -= 1 << 64
os.makedirs("ext_apks", exist_ok=True)
open("ext_apks/mangadex.apk", "wb").write(get(apk_url))
repo_meta = json.loads(get(REPO + "/repo.json"))["meta"]
print("MangaDex ext", apk_url, "source id", source_id)

# --- 20 popular ongoing titles ---
q = [("limit", "20"), ("status[]", "ongoing"), ("order[followedCount]", "desc"),
     ("availableTranslatedLanguage[]", "en"), ("contentRating[]", "safe"), ("includes[]", "cover_art"),
     ("includes[]", "author"), ("includes[]", "artist"), ("hasAvailableChapters", "true")]
data = json.loads(get("https://api.mangadex.org/manga?" + urllib.parse.urlencode(q)))["data"]
now = int(time.time() * 1000)
mangas = []
for i, m in enumerate(data):
    a = m["attributes"]
    title = a["title"].get("en") or next(iter(a["title"].values()))
    rel = {r["type"]: r for r in m["relationships"]}
    cover = next((r["attributes"]["fileName"] for r in m["relationships"] if r["type"] == "cover_art" and r.get("attributes")), None)
    author = next((r["attributes"]["name"] for r in m["relationships"] if r["type"] == "author" and r.get("attributes")), "")
    artist = next((r["attributes"]["name"] for r in m["relationships"] if r["type"] == "artist" and r.get("attributes")), "")
    desc = (a.get("description") or {}).get("en", "")[:800]
    tags = [t["attributes"]["name"]["en"] for t in a.get("tags", [])][:8]
    b = f_int(1, source_id) + f_str(2, "/manga/" + m["id"]) + f_str(3, title)
    if artist: b += f_str(4, artist)
    if author: b += f_str(5, author)
    if desc: b += f_str(6, desc)
    for t in tags: b += f_str(7, t)
    b += f_int(8, 1)  # ongoing
    if cover: b += f_str(9, f"https://uploads.mangadex.org/covers/{m['id']}/{cover}")
    b += f_int(13, now - i * 86400000)
    b += f_int(17, 1 if i < 12 else 2)  # category order
    b += f_int(100, 1)
    mangas.append(b)
    print(" ", title)

cats = [f_str(1, "Reading") + f_int(2, 1) + f_int(3, 1), f_str(1, "Plan to read") + f_int(2, 2) + f_int(3, 2)]
backup = b"".join(f_bytes(1, m) for m in mangas) + b"".join(f_bytes(2, c) for c in cats)
backup += f_bytes(101, f_str(1, "MangaDex") + f_int(2, source_id))
store = (f_str(1, REPO + "/index.min.json") + f_str(2, repo_meta.get("name", "Keiyoushi"))
         + f_str(4, repo_meta.get("website", "https://keiyoushi.github.io"))
         + f_str(5, repo_meta["signingKeyFingerprint"]))
backup += f_bytes(106, store)
open("seed.tachibk", "wb").write(gzip.compress(backup))
print("seed.tachibk", len(mangas), "titles")
