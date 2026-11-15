# Kototoro vs 69-Komikku (mod69 #55): inventory

Kototoro is the biggest app of the set: Kotatsu core + novels + video + its own iOS-style interface. 2890 texts exist only in Kototoro (inv/Kototoro_only.txt); grouped by name: reader 394 (≈300 are AI translation / super-resolution), pref 184, novel 182, video 180, home/discover/space ≈120, appearance/glass/panorama/badge ≈160, migration 99, source/extension ≈120, tracking sites (kitsu, simkl, bangumi…) ≈150, others.
Phone: wizard, Home, History, Favorites, Explore, Feed, space switcher, extension manager, Settings main, Appearance and its sub-pages (Lists, Details, Home, Navigation). NOT captured: details page and reader with a real title. The virtual phone stops working every time Kototoro loads the MangaDex extension (twice, two sessions), and its backup restore brings no favourites without a source. Those parts are from the code only.

| # | Item | Where | In 69-Komikku? | Phone | Board |
|---|---|---|---|---|---|
| 1 | Interface style switch: iOS (modern glass) / Material 3 / expressive; glass finish tuner; immersive layers (tint + blur under bars) | Appearance | partly (frosted glass bars K10-6) | yes | VIS-9 |
| 2 | Home tab: greeting, hero carousel (fixed / automatic / curated), continue reading, quick access tiles, suggestions | Home | no | yes | VIS-9 |
| 3 | Floating navigation: capsule highlight, labels below / right, full width, heights | Navigation appearance | yes (K9-1 floating bar, bar height) | yes | – |
| 4 | Page background: standard or blurred artwork / custom image | Appearance | no | setting seen | VIS-9 note |
| 5 | Lists: card progress style (bottom bar / corner badge), badges per corner (favourite, saved, score, tracker icons, chapters left), slide animation intensity, quick filters | Appearance → Lists | yes (K7-5, progress bar) except extra badge kinds (minor) | yes | – |
| 6 | Details: panorama cover with motion, modern dock, details style per kind, pages tab, translate button | Appearance → Details | different | settings only | VIS-6 / VIS-7 |
| 7 | Spaces: separate Manga / Novel / Video contexts | bottom bar | no | yes | CMP-UI-4 |
| 8 | Explore: big banner, schedule / seasonal / trending from tracking sites (Discover) | Explore | partly (K10-4, K10-9) | yes | VIS-9 |
| 9 | Reader: AI translation pipeline, AI super-resolution (Real-CUGAN, Anime4K), floating buttons you choose, info bar layouts, autoscroll floating button | reader | yes in simpler form (K11-1, K6-4, K2-1) | code | – |
| 10 | Crop a panel + note; broken-source warnings with "migrate these"; hide adult content per tab; e-ink full refresh; duplicates finder with tolerance | various | partly (K8-3, CMP-LB-4, CMP-RD-4) | code | – |
| 11 | Novels, video playback, TVBox / JAR sources, WebDAV sync, many trackers | spaces | no / not applicable (CMP-OT-2, CMP-OT-4) | code | – |
| 12 | Shared Kotatsu core | all | see research/kotatsu_family_core.md | – | – |
