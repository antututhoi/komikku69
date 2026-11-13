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
