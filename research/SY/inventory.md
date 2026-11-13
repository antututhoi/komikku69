# TachiyomiSY (source: master) vs 69-Komikku (mod69 #55): inventory

Method: all 1571 strings and 312 preference keys of SY compared with 69-Komikku. 69-Komikku is built on SY, so 1466 strings / 260 keys match. ALL non-matching items (105 strings, 52 keys; raw list inv/raw_TachiyomiSY.txt) are below.

| # | Item (SY) | Where | In 69-Komikku? | Checked on phone | Board |
|---|---|---|---|---|---|
| 1 | Everything in the Mihon inventory rows 1–7, 12–17 (WebGPU engine, updates category filter, time format, Tokyo Night, refresh tracker profile, skip version…) | same places | same answers as Mihon | yes: WebGPU engine checked in SY (research/SY/reader_hq/) | CMP-RD-1, CMP-RD-2, CMP-U4, CMP-UI-1/2 |
| 2 | Autoscroll gestures (long strip): tap = pause/resume, swipe up/down = speed, two-finger drag = scroll by hand, swipe sideways = stop; + "Gesture toasts" + help popups | reader → pull-down tools panel, next to Autoscroll | no (69 has Autoscroll, Retry all, Boost page only) | panel opened (research/SY/reader_hq/SY_reader_exh_panel*.jpg); the gesture switches are not in the installed SY build, so behaviour read from code | CMP-RD-3 |
| 3 | Edit entry info dialog: "Collate tags" button (merged entries: gathers tags of all merged sources), "Source: …" hint under each field showing the source's own value, labelled Description / Tags boxes | entry → ⋮ → Edit info | different (69 dialog has no hints, no collate) | code | CMP-L4 |
| 4 | Merge settings: Duplicate chapters Keep all / Priority / Most / Highest, Info source, Drag to set priority, Remove entries from the library | merged entry → edit merge | yes (same options under other text names; board K7-9) | – | – |
| 5 | Batch recommendations sheet: "For N selected entries", "Search in: each entry's own source / your trackers" | library selection → recommendations | yes (same sheet; reworded texts) | – | – |
| 6 | E-Hentai / nhentai extras texts (Disowned, favourites / views / total downloads counts) | 18+ source info pages | yes/different wording (69 carries the same exh screens) | – | – |
| 7 | Sync (own server / Google Drive) keys `sync_*`, `google_drive_*` | Data and storage → Sync | yes (69 has Sync with other key names) | – | – |
| 8 | Backup section keys (`appSettings`, `categories`, `chapters`, `history`, `tracking`, `savedSearches`…) | backup create dialog | yes (same options, other key names) | – | – |
| 9 | `eh_log_level`, `pinned_catalogues`, `targetAppId`, `extensionRepoSettings` | internal | yes / not applicable | – | – |

Reader walk (all options pressed): research/SY/reader/. Reader bar identical to 69-Komikku.
Status: all non-matching items classified. Item 2 could not be exercised on the installed build (code only).
