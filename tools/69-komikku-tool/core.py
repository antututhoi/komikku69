"""
69-Komikku Tool: turns a copied Komikku "downloads" folder into folders of (smaller) images,
sorted into folders named after the entries' categories (read from a Komikku backup).

Komikku reads a chapter folder of images just like a .cbz (folder name = .cbz name without ".cbz").
"""
import gzip
import io
import os
import re
import shutil
import zipfile
from concurrent.futures import ProcessPoolExecutor, as_completed
from dataclasses import dataclass, field, replace

from PIL import Image, ImageFile

ImageFile.LOAD_TRUNCATED_IMAGES = True
Image.MAX_IMAGE_PIXELS = None

IMAGE_EXT = {".jpg", ".jpeg", ".png", ".webp", ".gif", ".bmp", ".avif", ".jxl"}
WEBP_MAX = 16383  # WebP can't be taller/wider than this
DONE_MARK = ".69done"
DEFAULT_FOLDER = "Default"  # library entries in Komikku's Default category (same name in the app)
NO_INFO_FOLDER = "uncategories"  # not in the library / backup (same name in the app)


# ---------------------------------------------------------------- names (same rules as Komikku)

def valid_filename(name: str) -> str:
    """Komikku's DiskUtil.buildValidFilename (FAT-safe)."""
    name = name.strip(". ")
    if not name:
        return "(invalid)"
    out = []
    for c in name:
        if ord(c) < 0x20 or ord(c) == 0x7F or c in '"*/:<>?\\|':
            out.append("_")
        else:
            out.append(c)
    s = "".join(out)
    b = s.encode("utf-8")
    if len(b) > 240:
        s = b[:240].decode("utf-8", "ignore")
    return s


def loose(name: str) -> str:
    return re.sub(r"[^0-9a-z]+", "", name.lower())


# ---------------------------------------------------------------- Komikku backup (.tachibk)

def _varint(buf, i):
    shift = result = 0
    while True:
        b = buf[i]
        i += 1
        result |= (b & 0x7F) << shift
        if b < 0x80:
            return result, i
        shift += 7


def _fields(buf):
    i, n = 0, len(buf)
    while i < n:
        key, i = _varint(buf, i)
        num, wt = key >> 3, key & 7
        if wt == 0:
            v, i = _varint(buf, i)
        elif wt == 1:
            v = buf[i:i + 8]
            i += 8
        elif wt == 2:
            ln, i = _varint(buf, i)
            v = buf[i:i + ln]
            i += ln
        elif wt == 5:
            v = buf[i:i + 4]
            i += 4
        else:
            raise ValueError("bad backup file")
        yield num, wt, v


def read_backup(path: str):
    """Returns {title: [category names in order]} for library entries."""
    raw = open(path, "rb").read()
    try:
        raw = gzip.decompress(raw)
    except OSError:
        pass
    cats = {}  # order -> name
    mangas = []
    for num, wt, v in _fields(raw):
        if num == 2 and wt == 2:
            name, order = "", 0
            for n2, w2, v2 in _fields(v):
                if n2 == 1 and w2 == 2:
                    name = v2.decode("utf-8", "ignore")
                elif n2 == 2 and w2 == 0:
                    order = v2
            cats[order] = (order, name)
        elif num == 1 and wt == 2:
            title, orders, favorite = "", [], True
            for n2, w2, v2 in _fields(v):
                if n2 == 3 and w2 == 2:
                    title = v2.decode("utf-8", "ignore")
                elif n2 == 17:
                    if w2 == 0:
                        orders.append(v2)
                    elif w2 == 2:  # packed
                        j = 0
                        while j < len(v2):
                            o, j = _varint(v2, j)
                            orders.append(o)
                elif n2 == 100 and w2 == 0:
                    favorite = bool(v2)
            if favorite and title:
                mangas.append((title, orders))
    result = {}
    for title, orders in mangas:
        names = [cats[o] for o in orders if o in cats]
        names.sort()
        result[title] = [n for _, n in names if n.strip()]
    return result


class CategoryFinder:
    def __init__(self, backup: dict):
        self.exact = {}
        self.fuzzy = {}
        for title, names in backup.items():
            self.exact.setdefault(valid_filename(title).lower(), names)
            self.fuzzy.setdefault(loose(title), names)

    def folder_for(self, manga_dir_name: str):
        """Category folder; DEFAULT_FOLDER for library entries without a category,
        NO_INFO_FOLDER for entries that aren't in the library/backup."""
        names = self.exact.get(manga_dir_name.lower())
        if names is None:
            names = self.fuzzy.get(loose(manga_dir_name))
        if names is None:
            return NO_INFO_FOLDER
        if not names:
            return DEFAULT_FOLDER
        return valid_filename(names[0])


# ---------------------------------------------------------------- images

@dataclass
class Options:
    resize: bool = True
    max_width: int = 1080
    max_height: int = 0  # 0 = no limit
    fmt: str = "webp"  # webp / jpeg / keep
    quality: int = 80
    webtoon_ratio: float = 2.5
    only_smaller: bool = True
    shrink_webtoon: bool = False  # long (and wide) pictures keep their full size unless this is on
    # [(folder, "width" | "height" | "all")]: that limit is not used for pictures inside the folder
    excludes: list = field(default_factory=list)


EXCLUDE_CHOICES = {"width": "Don't limit width", "height": "Don't limit height", "all": "Don't resize at all"}


def _norm(p: str) -> str:
    return os.path.normcase(os.path.abspath(p)).rstrip("\\/")


def options_for(path: str, o: Options) -> Options:
    """Options for one chapter, with the exclude list applied (several folders can match)."""
    if not o.excludes:
        return o
    p = _norm(path)
    mw, mh = o.max_width, o.max_height
    for folder, mode in o.excludes:
        f = _norm(folder)
        if p == f or p.startswith(f + os.sep):
            if mode in ("width", "all"):
                mw = 0
            if mode in ("height", "all"):
                mh = 0
    return replace(o, max_width=mw, max_height=mh, excludes=[])


def convert_image(data: bytes, name: str, o: Options):
    """Returns (new_name, new_bytes). Keeps the original when it would not get smaller."""
    base, ext = os.path.splitext(name)
    ext = ext.lower()
    if not o.resize or ext == ".gif":
        return name, data
    try:
        im = Image.open(io.BytesIO(data))
        im.load()
    except Exception:
        return name, data
    w, h = im.size
    tall = h > w * o.webtoon_ratio or w > h * o.webtoon_ratio  # long webtoon strip / wide panorama
    scale = 1.0
    # fits inside max width x max height, same shape, never made bigger;
    # long webtoon strips keep their size (only type + quality change) unless asked; never cut
    if o.shrink_webtoon or not tall:
        if o.max_width and w > o.max_width:
            scale = min(scale, o.max_width / w)
        if o.max_height and h > o.max_height:
            scale = min(scale, o.max_height / h)
    if scale < 1.0:
        im = im.resize((max(1, round(w * scale)), max(1, round(h * scale))), Image.LANCZOS)
    nw, nh = im.size
    fmt = o.fmt
    if fmt == "keep":
        fmt = {"jpg": "jpeg", "jpeg": "jpeg", "png": "png", "webp": "webp"}.get(ext.strip("."), "jpeg")
    if fmt == "webp" and max(nw, nh) > WEBP_MAX:
        fmt = "jpeg"  # too tall for WebP (long strip): JPEG instead, never cut
    if fmt == "jpeg" and im.mode not in ("RGB", "L"):
        im = im.convert("RGB")
    if fmt == "webp" and im.mode not in ("RGB", "RGBA", "L"):
        im = im.convert("RGBA" if "A" in im.mode else "RGB")
    out = io.BytesIO()
    if fmt == "webp":
        im.save(out, "WEBP", quality=o.quality, method=4)
        new_ext = ".webp"
    elif fmt == "png":
        im.save(out, "PNG", optimize=True)
        new_ext = ".png"
    else:
        im.save(out, "JPEG", quality=o.quality, optimize=True, progressive=True)
        new_ext = ".jpg"
    b = out.getvalue()
    if o.only_smaller and len(b) >= len(data) and scale == 1.0:
        return name, data
    return base + new_ext, b


# ---------------------------------------------------------------- chapters

def process_chapter(src: str, dst_dir: str, extract: bool, o: Options):
    """src = .cbz file or chapter folder. Writes the chapter to dst_dir. Returns (bytes before, bytes after)."""
    before = after = 0
    is_cbz = os.path.isfile(src) and src.lower().endswith((".cbz", ".zip"))
    if is_cbz and not extract:
        # stays a .cbz, pages re-packed
        os.makedirs(os.path.dirname(dst_dir), exist_ok=True)
        tmp = dst_dir + ".cbz.69tmp"
        with zipfile.ZipFile(src) as zin, zipfile.ZipFile(tmp, "w", zipfile.ZIP_STORED) as zout:
            for info in zin.infolist():
                if info.is_dir():
                    continue
                data = zin.read(info)
                before += len(data)
                name = os.path.basename(info.filename)
                if os.path.splitext(name)[1].lower() in IMAGE_EXT:
                    name, data = convert_image(data, name, o)
                after += len(data)
                zout.writestr(name, data)
        os.replace(tmp, dst_dir + ".cbz")
        return before, after
    os.makedirs(dst_dir, exist_ok=True)
    if is_cbz:
        with zipfile.ZipFile(src) as zin:
            entries = [(i.filename, zin.read(i)) for i in zin.infolist() if not i.is_dir()]
    else:
        entries = []
        for n in sorted(os.listdir(src)):
            p = os.path.join(src, n)
            if os.path.isfile(p) and n != DONE_MARK:
                entries.append((n, open(p, "rb").read()))
    for full, data in entries:
        before += len(data)
        name = os.path.basename(full)  # pages straight in the chapter folder (no sub folders)
        if not name or name.startswith("."):
            continue
        if os.path.splitext(name)[1].lower() in IMAGE_EXT:
            name, data = convert_image(data, name, o)
        after += len(data)
        with open(os.path.join(dst_dir, name), "wb") as f:
            f.write(data)
    open(os.path.join(dst_dir, DONE_MARK), "w").close()
    return before, after


def _job(args):
    src, dst, extract, o = args
    try:
        b, a = process_chapter(src, dst, extract, o)
        return src, b, a, None
    except Exception as e:  # keep going with the other chapters
        return src, 0, 0, str(e)


def find_chapters(downloads: str):
    """Yields (top folder, entry folder, chapter path). Accepts Komikku's root or its downloads folder."""
    if os.path.isdir(os.path.join(downloads, "downloads")):
        downloads = os.path.join(downloads, "downloads")
    for top in sorted(os.listdir(downloads)):
        tp = os.path.join(downloads, top)
        if not os.path.isdir(tp):
            continue
        for manga in sorted(os.listdir(tp)):
            mp = os.path.join(tp, manga)
            if not os.path.isdir(mp):
                continue
            for ch in sorted(os.listdir(mp)):
                cp = os.path.join(mp, ch)
                if ch.startswith("_tmp") or ch.endswith("_tmp"):
                    continue
                if os.path.isdir(cp) or ch.lower().endswith((".cbz", ".zip")):
                    yield top, manga, cp


def _done(dst):
    return os.path.exists(os.path.join(dst, DONE_MARK)) or os.path.exists(dst + ".cbz")


def _move_done(old, dst):
    """Moves a chapter finished by an earlier run (old layout) to dst. True when moved."""
    try:
        if os.path.exists(os.path.join(old, DONE_MARK)):
            os.makedirs(os.path.dirname(dst), exist_ok=True)
            os.replace(old, dst)
        elif os.path.exists(old + ".cbz"):
            os.makedirs(os.path.dirname(dst), exist_ok=True)
            os.replace(old + ".cbz", dst + ".cbz")
        else:
            return False
    except OSError:
        return False
    for d in (os.path.dirname(old), os.path.dirname(os.path.dirname(old))):
        try:
            os.rmdir(d)  # only removed when empty
        except OSError:
            pass
    return True


def run(downloads, output, backup=None, extract=True, sort=True, o: Options = Options(),
        workers=None, log=print, progress=lambda done, total: None, stop=lambda: False):
    finder = CategoryFinder(read_backup(backup)) if (sort and backup) else None
    out_root = os.path.join(output, "downloads")
    jobs = []
    moved = 0
    counts = {DEFAULT_FOLDER: set(), NO_INFO_FOLDER: set()}
    for top, manga, cp in find_chapters(downloads):
        folder = top
        if finder:
            folder = finder.folder_for(manga)
            if folder in counts:
                counts[folder].add(manga)
        ch_name = os.path.basename(cp)
        if ch_name.lower().endswith((".cbz", ".zip")):
            ch_name = ch_name[:-4]
        dst = os.path.join(out_root, folder, manga, ch_name)
        if _done(dst):
            continue  # done before (the tool can be run again to continue)
        if folder != top and _move_done(os.path.join(out_root, top, manga, ch_name), dst):
            moved += 1  # done by an earlier run into the source folder: moved, not redone
            continue
        jobs.append((cp, dst, extract, options_for(cp, o)))
    if finder:
        log(f"{len(counts[DEFAULT_FOLDER])} entries are in Komikku's Default category -> '{DEFAULT_FOLDER}' folder")
        log(f"{len(counts[NO_INFO_FOLDER])} entries aren't in the library/backup -> '{NO_INFO_FOLDER}' folder")
    if moved:
        log(f"{moved} chapters done by an earlier run were moved into their new folder")
    total = len(jobs)
    log(f"{total} chapters to do")
    before = after = 0
    done = 0
    progress(0, total)
    with ProcessPoolExecutor(max_workers=workers or os.cpu_count()) as ex:
        futures = [ex.submit(_job, j) for j in jobs]
        for fut in as_completed(futures):
            if stop():
                for f in futures:
                    f.cancel()
                log("Stopped. Run again to continue where it stopped.")
                break
            src, b, a, err = fut.result()
            done += 1
            if err:
                log(f"Failed: {src}: {err}")
            before += b
            after += a
            progress(done, total)
    gb = 1024 ** 3
    log(f"Done: {done}/{total} chapters. {before / gb:.2f} GB -> {after / gb:.2f} GB")
    return before, after
