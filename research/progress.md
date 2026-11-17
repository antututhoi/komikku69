# 69-Komikku comparison: progress

Updated: 13 Nov 2026. Method: remote-controlled virtual phone on GitHub (branch `research69`, workflow "69 Research phone"; commands via branch `ctl69-cmd`, screenshots and screen lists on `ctl69-out`). Notes per app in `research/` on the research69 branch.

| # | App | Status | Notes |
|---|---|---|---|
| 1 | 69-Komikku (baseline) | done (first pass) | Settings lists, reader, library, entry, tabs. Library update cause found (Smart update → Predict next release time). 4 polish problems noted. |
| 2 | Mihon 0.21.3 | in progress | Restored, settings walk running |
| 3 | TachiyomiSY | not started | |
| 4 | J2K | not started | |
| 5 | Tsundoku | not started | |
| 6 | Chimahon | not started | |
| 7 | Kototoro | not started | |
| 8 | Kaisoku | not started | |
| 9 | Futon | not started | Test the Kotatsu backup here |
| 10 | DropSauce | not started | |
| — | Final cross-app comparison | not started | |

## Board changes so far
- Removed CMP-S4 (Hide entries already in library) and CMP-S7 (Remember page in finished chapters): both already in 69-Komikku.

## 13 Nov update
- 69-Komikku: library display modes + overlays, reader (modes, scale, tap zones, bg, toggles) done with real option presses; pictures in research/69K/.
- Mihon: reader walk done (research/MH/reader/); library display + entry page still to do.
- SY: reader walk running.
- SY: reader walk done (research/SY/reader/). Reader bar identical to 69-Komikku (Chapters, WebView, Crop, Page layout, Settings); Mihon's bar is poorer (no Chapters/WebView/Page layout). Option sets and tap-zone overlays same as 69-Komikku. Nothing new in the reader sheet itself for SY beyond what 69-Komikku has.
- Next: library display + entry page on Mihon/SY, then J2K, Tsundoku, Chimahon, Kototoro, Kaisoku, Futon, DropSauce.

## 13 Nov (later): code-first method
- New method: every string + preference key of each app is diffed against 69-Komikku's code (tool: inv/inv.py, raw lists inv/raw_<app>.txt), then only the differences are checked on the phone. Inventories: research/<app>/inventory.md.
- Mihon: DONE. 19 differing items, all classified; board rows added: CMP-U4, CMP-RD-1, CMP-RD-2, CMP-SR-2 (CMP-SR-1, CMP-UI-1, CMP-UI-2 existed).
- SY: DONE (code + phone). New: autoscroll gestures (CMP-RD-3), edit-info hints/collate tags (CMP-L4).
- Phone session 2 started with: 69-Komikku, J2K, Tsundoku, Chimahon.
- J2K: DONE (research/J2K/inventory.md, 45 pictures). Rows: CMP-OT-1, CMP-LB-1, CMP-LB-2, CMP-UI-3, CMP-D5, VIS-6; pictures added to CMP-L3.
- Tsundoku: DONE (research/Tsundoku/inventory.md, 22 pictures). Rows: CMP-LB-3, CMP-LB-4, CMP-OT-2.
- Chimahon: DONE (research/Chimahon/inventory.md, 60 pictures). Rows: CMP-UI-4, CMP-UI-5, CMP-RD-4, CMP-OT-3, CMP-OT-4, CMP-SR-3.
- Phone session 3 started with: 69-Komikku, Kototoro, Kaisoku, Futon, DropSauce (Kotatsu family; shared-core list inv/kotatsu_core.txt).
- Futon, Kaisoku, DropSauce: DONE (inventories + research/kotatsu_family_core.md; pictures 46 / 49 / 47). 16 rows added: CMP-RD-5..8, CMP-L5, CMP-L6, CMP-LB-5, CMP-SR-4..6, CMP-U5, CMP-OT-5, CMP-UI-6, VIS-7..9.
- Kototoro: PARTLY. Code read (2890 own texts grouped), 13 pictures of main tabs + wizard; the phone crashed when all 62 MangaDex sources were enabled, so details / reader / settings pages are not captured yet. Backup restore gave no favourites there (extension-based sources).
- Notes: Futon/Kaisoku restore seed_kotatsu.zip fine (Futon starts in Novel mode: tap "Manga"); DropSauce imports seed.tachibk; UI text dumps fail on animated main screens of Futon/DropSauce (use screenshots + coordinates).
- TODO: finish Kototoro on the phone; add TachiyomiAZ and Yokai (Gor, 15 Nov; yokai-nightly is the same app's test build); final cross-app summary.
- 15 Nov: Gor asked to fix 69-Continuum first (logged-in feed empty, "Cannot fetch user info"). Fork is 8.3.3.6-69; upstream is 8.3.3.9.3 with Reddit API-block fixes.
- 15 Nov: Yokai (+nightly) and TachiyomiAZ DONE; rows CMP-LB-6, CMP-UI-7, CMP-UI-8, CMP-SR-7. Kototoro closed as far as the phone allows (details / reader not capturable: emulator dies when it loads the MangaDex extension, 2 attempts). Cross-app summary: research/SUMMARY.md. RESEARCH PHASE COMPLETE; waiting for Gor's picks on the board.

## Recheck with filled screens (16 Nov, session 5)
Backups `seed2.tachibk` / `seed_kotatsu2.zip` (history + missing chapters) so Updates, History and Download queue are not empty.
Standard tour (same ~40 screens, `tour.py`), pictures in `research/<app>/<area>/*_tour_sNN_*`.
- 69-Komikku (K2): complete.
- Mihon (M2): complete. Updates lists every chapter (Komikku groups per title with a count badge and fold arrow). History: search + delete-all only (Komikku adds filter + select). Download menu on entry has no "All" (Komikku/SY have it). Global search starts on "Pinned" (Komikku on "All").
- TachiyomiSY (S3): complete except library selection shots. Same as Mihon plus SY extras already in Komikku.
- Tsundoku (T2): complete except download queue list (queue has Novels / Manga / Translations tabs). New: History shows % read and has a per-title / every-chapter switch (board CMP-H1). All / Manga / Novels chips on Updates and History. Source list has Popular / Latest / Filter / Translate chips.
- Chimahon (C2): complete. Updates tab lives under More (bottom bar: Library, History, Browse, Dictionary, More). Manga / Anime / Novel tabs on Updates, History, Categories. Statistics by period with weekday strip, streak, speed (board CMP-OT-6).
- Confirmed again: in 69-Komikku "Update library" ends without fetching missing chapters; refreshing one entry fetches them (fix first in build phase).
Still to tour with filled screens: J2K, Yokai, TachiyomiAZ, Futon, Kaisoku, DropSauce (session 6).

## Recheck, sessions 6-7 (16-17 Nov): three virtual phones side by side
New tooling: lanes b and c (`research_69_lanes.yml`, `.research69/lanes.txt`, `LANE=b ctl.sh …`), `kset.py` (fresh setup per app), `ktour.py` (Kotatsu-type tour), `j2full.py` (J2K/Yokai tour), `sqlrun.py` (root SQL on the phone; `feed.sql` puts test rows into the Kotatsu-type "new chapters" feed, which a backup cannot carry).
- J2K (J3): filled Recents (Grouped / All / History / Updates), entry page, chapter rows downloading, bottom "Downloading: …" bar on Recents, Stats with donut. Titles show in English there.
- Yokai (Y3): same as J2K plus full-screen Download queue (drag handles, per-item menu, Resume button, swipe to cancel), chapter long-press sheet (mark previous / a range as read or unread), Statistics details.
- Futon (F3): filled Feed (cover row with count + progress ring, category chips, list by day, badge on the tab), History grid by day with chips, favourites with progress rings, details + pull-up chapters, "Save manga" dialog (whole manga / one translation / first 5 / next unread 5), reader long-press sheet, reader chapters sheet, Explore tiles + source icons, Sources catalog.
- DropSauce (D3): Feed with round play buttons, Unread chip, swipe hint card, "Marked as read · Undo"; History grid + Display options sheet (list modes, group, grid size, title over cover, spacing). Its phone stopped answering when the tour reached the details page; rest to redo.
- Kaisoku (KA): same family; labelled bottom bar, "In favorites since" + Translation flag on details, History list options with sort order. Tour running.
- Board rows added: CMP-H1, CMP-OT-6, VIS-11, VIS-12.
Still to do: TachiyomiAZ filled screens, DropSauce rest (details, downloads, reader), Kaisoku rest, Kototoro retry, final per-screen tables in SUMMARY.md.
