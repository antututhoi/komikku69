# Tsundoku (source: main) vs 69-Komikku (mod69 #55): inventory

Method: 2026 strings + 441 preference keys compared with 69-Komikku. 1007 strings match (Tsundoku is a Mihon fork). Of the 1019 non-matching strings, 32 were already covered by the Mihon / SY / J2K inventories; the other 987 (inv/tsu_new.txt) were grouped by their name prefix and read. About 700 belong to novels (custom_source 140, pref_novel 99, pref_translation 79, mass_import 69, selector_* 48, novel_* 80+, epub 30, translate 19…). Phone: restored the 20 titles, updated, opened library sheets, duplicates, entry, browse, updates, history, More, every settings page (pictures research/Tsundoku/<area>/).

| # | Item (Tsundoku) | Where | In 69-Komikku? | Phone | Board |
|---|---|---|---|---|---|
| 1 | Library filter sheet tabs "Tags" (chips with counts, include/exclude/clear, search, options) and "Extensions" (tick sources) | library → filter | no (typed tag:/source: search, K7-1) | yes: library/ | CMP-LB-3 |
| 2 | Filter "Chapter count ≥ N" with threshold box; Search options: chapter names, alternative titles, by URL, regex | library → filter | different (K7-1 chapters>50; no chapter-name / regex search) | yes | part of CMP-LB-3 note |
| 3 | Sort: Downloaded chapters, Source name (+ the Mihon sorts) | library → sort | no for these two | yes | CMP-LB-3 |
| 4 | Display: Show URL in list view, Title max lines | library → display | Title lines yes (K3-2); URL no (minor) | yes | – |
| 5 | Find Duplicates screen: Exact / Contains / Same URL / List all; Flexible match; filters (type, category AND/OR, library filters, blank titles); sort Name/Latest/Ch/DL/Read/pinned/priority; group actions and "select with highest/lowest chapters, downloads, read", source priority; delete / move to category | library ⋮ and entry ⋮ | different (K7-3 simple) | yes: duplicates/ | CMP-LB-4 |
| 6 | Duplicate warning when adding + duplicate list order; migrate source list sorting | Settings → Library | no | yes (settings list) | CMP-L1 (exists) |
| 7 | Show source name on details page | Settings | no | yes | CMP-L2 (exists) |
| 8 | Entry top bar: scroll to top / last read chapter / scroll to bottom; "Scroll actions in overflow menu" | entry | no | yes: entry/ | VIS-1 (exists) |
| 9 | Skip recently updated entries (N days) + "Update selected" dialog: fetch chapters / fetch details / ignore the skip | Settings → Library; library selection | no | yes (setting) | CMP-U3 (exists, Skipped) |
| 10 | Library ⋮: Reload Library, Mass import, Import EPUB, Clear category… (clear chapters / covers / descriptions / tags / custom metadata of entries) | library ⋮ | no (clean-up tools) | menu seen | noted only (niche) |
| 11 | Quick migrate: switch N entries to another source by matching links, no search | Browse → Migrate | no | not exercised (needs 2 sources with same links) | noted only |
| 12 | Database maintenance (ANALYZE / REINDEX / VACUUM) + database statistics; clear temp files; clear rate-limit history; reset all settings to default; normalise URLs | Settings → Advanced | partly (K4-1 clean up database, clear caches). Reset-all-settings: no | settings list | noted only |
| 13 | Experimental: paginate library to reduce memory | Advanced | different (K7-8 low-memory library) | – | – |
| 14 | Minimum chapters before tracking; MangaBaka sync options; novel trackers (NovelUpdates, NovelList, RanobeDB); source-defined trackers | Settings → Tracking | yes for minimum chapters (K10-5); novel trackers no | list | part of CMP-OT-2 |
| 15 | Combined library (manga + novels in one tab), content type filter All / Manga / Novels on Updates and History, Hide manga-related UI | Appearance; chips on Updates/History | no | yes: other/ | part of CMP-OT-2 |
| 16 | Novels: novel sources + extensions (LNReader JS plugins, custom sources by CSS selectors with a step wizard), novel reader (fonts, colours, themes, paragraph nav, auto-scroll, TTS), translation engine (79 settings), quotes, word count, EPUB export / import, mass import by URL list, novel downloads with rate limits, LNReader backup import | Browse, entry, reader, settings | no (69 has local EPUB/TXT reader only, K11-2) | screens + settings seen; reader not exercised | CMP-OT-2 |
| 17 | Entry ⋮: Notes, Translate, Find Duplicates | entry | Notes yes; others see 5, 16 | yes | – |
| 18 | Manga reader settings, downloads, security pages | Settings | yes (all Mihon options; nothing new) | yes (lists) | – |
