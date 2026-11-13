# Chimahon 2.4.9 (source: master; a Komikku fork) vs 69-Komikku (mod69 #55): inventory

Method: 2910 strings + 804 preference keys compared with 69-Komikku. 1943 strings match (same Komikku base). 942 non-matching strings not already covered by earlier apps (inv/chi_new.txt), grouped by prefix: player 129+59+21+15… (≈330, built-in video player / cast), dictionary + popup 80, Anki 73 (incl. 35 audio-export error texts), OCR 45, anime library / episodes ≈150, novels ≈40, the rest below. Phone: restore, library, mode switch, browse, Dictionary tab, More, settings pages (pictures research/Chimahon/<area>/).

| # | Item (Chimahon) | Where | In 69-Komikku? | Phone | Board |
|---|---|---|---|---|---|
| 1 | Library title drop-down Manga / Anime / Novels; "Consolidated library" setting; separate default categories and smart-update rules per kind | library title; Appearance | no (Anime / Novels under More) | yes: library/ | CMP-UI-4 |
| 2 | Bottom bar: Library, History, Browse, Dictionary, More; Start screen; Navigation style; Customize tab bar layout; Always show nav labels | Appearance → Navbar | Start screen: no (CMP-UI-3). Others: yes (Komikku has nav style / labels) | yes (settings) | CMP-UI-3 |
| 3 | Settings list in sections General / Media / Learning / Sync | Settings | no (flat list) | yes | CMP-UI-5 |
| 4 | Floating "+" button on the library; "Show floating add button" | library | no (minor; opens add / local import) | seen | noted only |
| 5 | Dictionary tab: search, camera OCR; Yomitan dictionary import / export / auto-update, profiles, word audio sources | Dictionary tab; Settings → Dictionaries and audio | no (K11-3 uses Jisho online) | yes (screens) | CMP-OT-3 |
| 6 | Popup dictionary: size, fonts, theme, frequency / pitch display, recursive lookup style, swipe to dismiss, E-Ink mode, custom CSS | Settings → Popup dictionary | no | yes (settings) | CMP-OT-3 |
| 7 | Anki: deck / note type, field mapping (sentence, furigana, pitch, screenshot, audio), duplicates handling, tags | Settings → Anki | different (K11-3 sends a basic card) | yes (settings) | CMP-OT-3 |
| 8 | OCR: engines (on-device Lens, Paddle, Mokuro), model downloads, OCR boxes look, pre-OCR chapters, auto-OCR on download, two-finger gestures, crop and re-OCR, panel detection | Settings → OCR; reader | different (69 has page translation + Japanese words OCR) | settings yes; reader overlay not exercised (needs model download) | CMP-OT-3 |
| 9 | Screen lookup: floating capture button that OCRs other apps; quick-settings tile | More | no | switch seen | CMP-OT-3 |
| 10 | Reader: E-Ink mode, Flash on scroll/movement, Startup delay, Disable zoom in, Wide images zoom mode, Smaller tap zones, Smart scale on wide screen, Tap scroll by page | Settings → Reader | E-ink items, startup delay, disable zoom in: no. Smaller tap zones, smart scale, tap scroll by page: yes | yes (settings) | CMP-RD-4 |
| 11 | Browse: Search history (remember recent searches), Feed tab options, Suggestions options, source categories, Replace latest button | Settings → Browse | Search history: no. Others: yes (Komikku) | yes | CMP-SR-3 |
| 12 | Library: fetch metadata / chapters when adding, tag sorting list, dynamic category updates, updating progress banner | Settings → Library | yes (Komikku) | yes | – |
| 13 | Appearance: Custom theme + palette, cover-based theme style, panorama cover, align cover to top, recommendations / merge in overflow, previews row count | Appearance | yes (Komikku) | yes | – |
| 14 | Anime: anime library, episodes, seasons, built-in mpv player (≈330 strings: gestures, subtitles, audio, AniSkip, PiP, Cast, torrent server, hosters, external downloader) | Anime mode; Settings → Player | different (K11-4: Aniyomi extensions + external player) | settings list only | CMP-OT-4 |
| 15 | Novels: novel library, novel servers, progress & stats in backup | Novels mode | different (K11-2 local files) | mode seen | CMP-OT-2 |
| 16 | App icon chooser (text only; no screen uses it in this version) | – | no | not present | – |
| 17 | FlareSolverr settings + test | Advanced | yes (K10-3) | – | – |

Not exercised: video player, OCR overlay in the reader, dictionary pop-up (need downloads / accounts).
