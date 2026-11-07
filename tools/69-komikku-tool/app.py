"""69-Komikku Tool – window."""
import multiprocessing
import os
import queue
import sys
import threading
import tkinter as tk
from tkinter import filedialog, messagebox, ttk

import core


class App(tk.Tk):
    def __init__(self):
        super().__init__()
        self.title("69-Komikku Tool")
        self.geometry("720x640")
        self.minsize(600, 560)
        self.q = queue.Queue()
        self.stopping = False
        self.worker = None

        pad = {"padx": 8, "pady": 4}
        f = ttk.Frame(self)
        f.pack(fill="both", expand=True, padx=10, pady=10)
        f.columnconfigure(1, weight=1)

        self.v_in = tk.StringVar()
        self.v_backup = tk.StringVar()
        self.v_out = tk.StringVar()
        rows = [
            ("Komikku downloads folder (copied)", self.v_in, self.pick_in),
            ("Komikku backup (.tachibk) – for categories", self.v_backup, self.pick_backup),
            ("Save the result in", self.v_out, self.pick_out),
        ]
        for r, (label, var, cmd) in enumerate(rows):
            ttk.Label(f, text=label).grid(row=r * 2, column=0, columnspan=3, sticky="w", padx=8, pady=(8, 0))
            ttk.Entry(f, textvariable=var).grid(row=r * 2 + 1, column=0, columnspan=2, sticky="ew", **pad)
            ttk.Button(f, text="Choose…", command=cmd).grid(row=r * 2 + 1, column=2, **pad)

        opts = ttk.LabelFrame(f, text="What to do")
        opts.grid(row=6, column=0, columnspan=3, sticky="ew", padx=8, pady=10)
        self.v_extract = tk.BooleanVar(value=True)
        self.v_sort = tk.BooleanVar(value=True)
        self.v_resize = tk.BooleanVar(value=True)
        self.v_smaller = tk.BooleanVar(value=True)
        ttk.Checkbutton(opts, text="Unpack .cbz into folders of pictures (Komikku reads them the same)",
                        variable=self.v_extract).grid(row=0, column=0, columnspan=4, sticky="w", **pad)
        ttk.Checkbutton(opts, text="Sort into folders named after the categories (needs the backup)",
                        variable=self.v_sort).grid(row=1, column=0, columnspan=4, sticky="w", **pad)
        ttk.Checkbutton(opts, text="Make pictures smaller", variable=self.v_resize).grid(
            row=2, column=0, columnspan=4, sticky="w", **pad)

        ttk.Label(opts, text="Max width (px, 0 = keep)").grid(row=3, column=0, sticky="w", **pad)
        self.v_width = tk.StringVar(value="1080")
        ttk.Spinbox(opts, from_=0, to=8000, increment=60, textvariable=self.v_width, width=8).grid(
            row=3, column=1, sticky="w", **pad)
        ttk.Label(opts, text="Quality (1–100)").grid(row=3, column=2, sticky="w", **pad)
        self.v_quality = tk.StringVar(value="80")
        ttk.Spinbox(opts, from_=1, to=100, textvariable=self.v_quality, width=6).grid(
            row=3, column=3, sticky="w", **pad)
        ttk.Label(opts, text="Picture type").grid(row=4, column=0, sticky="w", **pad)
        self.v_fmt = tk.StringVar(value="WebP (smallest)")
        ttk.Combobox(opts, textvariable=self.v_fmt, state="readonly", width=16,
                     values=["WebP (smallest)", "JPEG", "Keep the same"]).grid(row=4, column=1, sticky="w", **pad)
        ttk.Checkbutton(opts, text="Keep the original when the new one isn't smaller",
                        variable=self.v_smaller).grid(row=5, column=0, columnspan=4, sticky="w", **pad)
        ttk.Label(opts, text="Long webtoon pages are found by themselves: only their width is limited, "
                             "never cut or squeezed.", foreground="#666", wraplength=640).grid(
            row=6, column=0, columnspan=4, sticky="w", **pad)

        bar = ttk.Frame(f)
        bar.grid(row=7, column=0, columnspan=3, sticky="ew", padx=8)
        bar.columnconfigure(0, weight=1)
        self.progress = ttk.Progressbar(bar, mode="determinate")
        self.progress.grid(row=0, column=0, sticky="ew", padx=(0, 8))
        self.v_count = tk.StringVar(value="")
        ttk.Label(bar, textvariable=self.v_count, width=14).grid(row=0, column=1)
        self.b_start = ttk.Button(bar, text="Start", command=self.start)
        self.b_start.grid(row=0, column=2, padx=4)
        self.b_stop = ttk.Button(bar, text="Stop", command=self.stop, state="disabled")
        self.b_stop.grid(row=0, column=3, padx=4)

        self.log = tk.Text(f, height=10, wrap="word")
        self.log.grid(row=8, column=0, columnspan=3, sticky="nsew", padx=8, pady=8)
        f.rowconfigure(8, weight=1)
        self.say("1) Choose the copied downloads folder, the backup and where to save.\n"
                 "2) Start. It can be stopped and started again – it continues where it stopped.\n"
                 "3) Copy the new 'downloads' folder back to the phone, turn on '69- Download into "
                 "category folders' in Komikku, then Settings > Advanced > Reindex downloads.")
        self.after(150, self.poll)

    # ---------- pickers
    def pick_in(self):
        d = filedialog.askdirectory(title="Komikku downloads folder")
        if d:
            self.v_in.set(d)
            if not self.v_out.get():
                self.v_out.set(os.path.join(os.path.dirname(d), "Komikku 69 result"))

    def pick_backup(self):
        p = filedialog.askopenfilename(title="Komikku backup",
                                       filetypes=[("Komikku backup", "*.tachibk *.proto.gz *.gz"), ("All", "*.*")])
        if p:
            self.v_backup.set(p)

    def pick_out(self):
        d = filedialog.askdirectory(title="Save the result in")
        if d:
            self.v_out.set(d)

    # ---------- run
    def say(self, text):
        self.log.insert("end", text + "\n")
        self.log.see("end")

    def start(self):
        src, out, bk = self.v_in.get().strip(), self.v_out.get().strip(), self.v_backup.get().strip()
        if not os.path.isdir(src):
            messagebox.showerror("69-Komikku Tool", "Choose the downloads folder first.")
            return
        if not out:
            messagebox.showerror("69-Komikku Tool", "Choose where to save.")
            return
        if os.path.abspath(out).startswith(os.path.abspath(src)):
            messagebox.showerror("69-Komikku Tool", "Save somewhere outside the downloads folder.")
            return
        if self.v_sort.get() and not os.path.isfile(bk):
            if not messagebox.askyesno("69-Komikku Tool", "No backup chosen: nothing can be sorted into "
                                                          "categories. Go on without sorting?"):
                return
        try:
            o = core.Options(
                resize=self.v_resize.get(),
                max_width=max(0, int(self.v_width.get() or 0)),
                quality=min(100, max(1, int(self.v_quality.get() or 80))),
                fmt={"WebP (smallest)": "webp", "JPEG": "jpeg"}.get(self.v_fmt.get(), "keep"),
                only_smaller=self.v_smaller.get(),
            )
        except ValueError:
            messagebox.showerror("69-Komikku Tool", "Width and quality must be numbers.")
            return
        self.stopping = False
        self.b_start.config(state="disabled")
        self.b_stop.config(state="normal")
        args = dict(downloads=src, output=out, backup=bk if os.path.isfile(bk) else None,
                    extract=self.v_extract.get(), sort=self.v_sort.get(), o=o)
        self.worker = threading.Thread(target=self.work, kwargs=args, daemon=True)
        self.worker.start()

    def work(self, **kw):
        try:
            core.run(**kw, log=lambda t: self.q.put(("log", t)),
                     progress=lambda d, t: self.q.put(("progress", d, t)),
                     stop=lambda: self.stopping)
        except Exception as e:
            self.q.put(("log", f"Error: {e}"))
        self.q.put(("end",))

    def stop(self):
        self.stopping = True
        self.say("Stopping after the chapters that are running…")

    def poll(self):
        try:
            while True:
                m = self.q.get_nowait()
                if m[0] == "log":
                    self.say(m[1])
                elif m[0] == "progress":
                    _, d, t = m
                    self.progress.config(maximum=max(1, t), value=d)
                    self.v_count.set(f"{d} / {t}")
                elif m[0] == "end":
                    self.b_start.config(state="normal")
                    self.b_stop.config(state="disabled")
        except queue.Empty:
            pass
        self.after(150, self.poll)


if __name__ == "__main__":
    multiprocessing.freeze_support()
    if len(sys.argv) > 1 and sys.argv[1] == "--cli":
        # 69-komikku-tool --cli <downloads> <output> [backup]
        core.run(sys.argv[2], sys.argv[3], sys.argv[4] if len(sys.argv) > 4 else None)
    else:
        App().mainloop()
