# Mihon 0.21.3: inspection summary
Checked on the virtual phone (13 Nov 2026): onboarding, restore, all 10 settings pages scrolled to the end, library and its Filter/Sort/Display sheet and menu, entry page, reader menu and settings sheet.

- Mihon is the base of 69-Komikku. Library, entry page and reader look and work the same; 69-Komikku has far more options everywhere (Group tab, extra display modes, many 69- extras).
- Only in Mihon (newer upstream changes not in 69-Komikku yet):
  - Extension content warnings (Safe / Mixed / 18+) → board CMP-SR-1
  - Time format setting → board CMP-UI-1
  - Tokyo Night theme → board CMP-UI-2
  - MangaBaka, Hikka, Comick trackers (already noted on the board, row K8-9)
  - Verbose logging / redact sensitive headers, chapter name hash suffix (69-Komikku has "Include chapter URL hash" and log redaction K4-8: not new)
  - "Use vertical chapter navigator in" = same as 69-Komikku's vertical seekbar options (not new)
- Mihon's library update worked with the same backup only because of the release-time setting difference (see 69-Komikku notes).
- Not checked: tracker logins (need accounts).

## Reader walk (virtual phone, 13 Nov) — pictures in research/MH/reader/
- Same Settings sheet tabs as 69-Komikku: Reading mode / General / Custom filter. Modes, scale types, tap zones, background colours, page number, fullscreen etc. all tested by pressing and screenshotting each option (+ both toggle states).
- Mihon bottom bar: 4 icons (Reading mode, Rotation, Crop borders, Settings). 69-Komikku bar: Chapters, WebView, Crop borders, Page layout, Settings (richer). Mihon top bar: bookmark + "More options" menu.
- Mihon has no "69- My own" tap zone, no "Disabled" zone check yet needed; tap-zone overlays (L shaped, Kindle-ish, Edge, Right and Left) look identical to 69-Komikku.
- Mihon reader sheet has the same toggles (Show page number, Fullscreen, Keep screen on, Flash on page change, Always show chapter transition). Background colours: Black/Gray/White/Auto — same.
