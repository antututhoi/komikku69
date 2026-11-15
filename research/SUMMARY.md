# 69-Komikku vs 11 apps: cross-app summary (research phase, 13–15 Nov 2026)

Apps: Mihon, TachiyomiSY, J2K, Yokai (+nightly), TachiyomiAZ, Tsundoku, Chimahon, Futon, Kaisoku, DropSauce, Kototoro. Baseline: 69-Komikku mod69 build #55.
Method: every text and setting key in each app's code diffed against 69-Komikku (inv/inv.py, raw lists in inv/), then the differences checked on the GitHub virtual phone with the same 20 MangaDex titles. Per-app tables: research/<app>/inventory.md. Pictures: research/<app>/<area>/. No app code, mod69 commit or production build was made for this.

## Coverage
| App | Code diff | Phone | Not reached |
|---|---|---|---|
| Mihon | all 95 texts / 33 keys classified | reader walk, updates filter | broken-extension screen, tracker screens (need accounts) |
| TachiyomiSY | all 105 / 52 | reader walk, new reader engine + 6 animation videos | autoscroll gestures (not in installed build) |
| J2K | 528 grouped | all tabs, sheets, stats, 9 settings pages | tracker screens |
| Yokai | 69 beyond J2K | restore, library, 7 settings pages | app icon picker list |
| TachiyomiAZ | 158 grouped | drawer, library, entry, reader, 6 settings pages | – |
| Tsundoku | 987 grouped | library sheets, duplicates, entry, browse, 9 settings pages | novel reader |
| Chimahon | 942 grouped | library, mode switch, dictionary, settings | video player, OCR overlay |
| Futon | core + extras | favourites, details, reader, colour correction, 6 settings pages | – |
| Kaisoku | core + extras | favourites, explore, details, reader, 7 settings pages | browser sources, category tiles view |
| DropSauce | core + extras | wizard, library, details, reader, 6 settings pages | eye reminder, tracker import |
| Kototoro | 2890 grouped | main tabs, appearance settings | details + reader (the virtual phone stops when it loads the MangaDex extension) |

## Best version of each screen / feature
| Area | Best version | From | 69-Komikku today | Board row |
|---|---|---|---|---|
| Library layout | Big title + built-in search, category hopper, per-category sort / update | J2K / Yokai | tabs or one page with fold | VIS-2, VIS-3, VIS-4 |
| Library quick filters | Chip row (Not started, Unread, On device…) | J2K, Futon, DropSauce | filter sheet only | CMP-LB-2 |
| Library filter by tag / source | Tag chips with counts + source tick list | Tsundoku | typed search words | CMP-LB-3 |
| Library order | Drag & drop per category | J2K, TachiyomiAZ | sorts + pin | CMP-LB-1 |
| Duplicates | Full screen with match modes and smart selection | Tsundoku | one-tap select | CMP-LB-4 |
| Cover look | Corner roundness slider; title on soft fade | TachiyomiAZ; DropSauce | fixed | CMP-LB-6, VIS-8 |
| Entry page | J2K layout (wide Start reading button) or Kotatsu bottom tabs (Chapters / Pages / Bookmarks) | J2K; Futon | 4 buttons + floating Resume | VIS-6, VIS-7, VIS-1 |
| Entry info | Estimated reading time; edit info with source hints; fill from tracker | Futon; SY; DropSauce | time spent only | CMP-L5, CMP-L4, CMP-L6 |
| Reader engine | New "high quality" renderer (13 animations, shaders, two-page when wide) | Mihon / SY | own animations + filters | CMP-RD-1, CMP-RD-2 |
| Reader bottom bar | 69-Komikku / SY bar is already richer than Mihon's; J2K floating card and DropSauce big tiles are the nicer looks | J2K; DropSauce | – | VIS-5, VIS-8 |
| Reader controls | Tap + long-press action per area, hold to scroll; autoscroll gestures | Futon / Kaisoku; SY | own tap zones | CMP-RD-6, CMP-RD-3 |
| Reader picture | Colour correction per entry with before / after | Futon, Kaisoku, DropSauce | global filter | CMP-RD-5 |
| Reader smart bits | Read at X %, smart resume, hide a page for good, eye reminder, e-ink mode | Futon; Kaisoku; DropSauce; Chimahon | – | CMP-S1, CMP-RD-7, CMP-S5, CMP-RD-8, CMP-RD-4 |
| Updates | Filter by category; swipe rows; log of checks; re-check after CAPTCHA | Mihon; DropSauce; Futon | 4 filters | CMP-U4, CMP-U5, CMP-U2, CMP-U1 |
| Recents | One tab for history + updates, grouped | J2K / Yokai | two tabs | CMP-L3 (Skipped by Gor) |
| Downloads | Save-pages folder choice; keep / delete when source removes a chapter | Futon etc.; J2K | fixed Pictures folder | CMP-D1, CMP-D5 |
| Sources | Content warnings Safe / Mixed / 18+; why an extension didn't load; blocked tags; presets; any website as a source; recent searches | Mihon; Futon; Kaisoku; Chimahon / J2K | 18+ on / off | CMP-SR-1..7 |
| Statistics | Detailed stats with pie charts and breakdowns | J2K / Yokai | numbers + bars | CMP-OT-1 |
| App look | Starting screen; long-press tab shortcuts; app icon; settings in groups / cards; glass style + Home page | J2K; Yokai; Chimahon; DropSauce; Kototoro | – | CMP-UI-3..8, VIS-8, VIS-9 |
| Other media | Online novels; Japanese study tools; built-in video player; Manga / Anime / Novels switch | Tsundoku; Chimahon | local novels, Jisho look-up, external player | CMP-OT-2, CMP-OT-3, CMP-OT-4, CMP-UI-4 |
| Network | HTTP / SOCKS proxy | Futon, Kaisoku, DropSauce | none | CMP-OT-5 |

## Where 69-Komikku is already ahead
Reader bar and extras (page bookmarks, all-pages grid, own tap zones, AI sharpen, translation, panel-by-panel), download tools (category folders, make smaller, rename, speed limits), library tools (advanced search, pin, badges per corner, find duplicates, tracker linking), suggestions, AniList list, novels / anime basics, PIN lock, display size, themes, FlareSolverr. Most Mihon / SY / Komikku-fork options were already present.

## Known problems found on the way (for the build phase, Opus)
- Library update skips entries: Smart update → "Predict next release time" also blocks manual updates (same in official Komikku). Fix idea: manual updates ignore that rule + a notification listing skipped entries.
- Polish items listed in research/69-Komikku.md.

## Board
All ideas are "To pick" rows in "Compare: …" groups on the 69 Test Board (Add it / Skipped). Nothing gets built until Gor picks.
