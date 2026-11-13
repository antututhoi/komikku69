# Mihon (source: main branch) vs 69-Komikku (mod69 #55): inventory

Method: every string resource (971) and preference key (187) in Mihon's code was compared with 69-Komikku's (2119 strings, 463 keys). Everything Mihon has that matched by name or text is "yes" (not listed: ~876 strings, 154 keys — Mihon is the base of 69-Komikku). The rows below are ALL the items that did not match (95 strings, 33 keys), grouped. Raw list: inv/raw_mihon.txt.

| # | Item (Mihon) | Where | In 69-Komikku? | Checked on phone | Board |
|---|---|---|---|---|---|
| 1 | High quality renderer (WebGPU reader engine): `pref_high_quality_renderer_key`, webgpu_* (13 keys) | Advanced → Reader → Use high quality renderer; then reader sheet gets a "WebGPU" section | no | yes (in SY, which carries it): research/SY/reader_hq/ | CMP-RD-1 |
| 1a | Transition animations: Basic, Page flip, Page flip left/right, Stack left/right/up/down, Sphere, Cube inside/outside, Fade, Fade to white, None (+ separate set for dual pages) | reader sheet → WebGPU | different (69 has Slide, Fade, Stack, Cube, Zoom out, Page flip = K10-1; no Sphere, Cube inside, Fade to white, 4 stack directions, real page curl) | yes, videos: research/SY/reader_hq/video_* | CMP-RD-2 |
| 1b | Dual page view: Never / Always / When wide | reader sheet | different (69 has Page layout: single/double/automatic) | yes (sheet) | part of CMP-RD-1 |
| 1c | Display cutout mode: Ignore / Avoid / Shift (+ dual) | reader sheet → WebGPU | different (69: "Show content in cutout area" + 69- notch only when sideways) | yes (sheet) | part of CMP-RD-1 |
| 1d | Image filters / Tile filters: Add shader (pick a shader file), shader parameters, Upscaler, Downscaler | reader sheet → WebGPU | no (69 has 69- Picture filter Sharpen/Smooth, K10-8) | yes (opens file picker) | part of CMP-RD-1 |
| 1e | Crop instead of zoom, Disable double tap gestures, Show all tiles together, Disable tiles, continuous Gap / Min width | reader sheet → WebGPU | no | yes (sheet list) | part of CMP-RD-1 |
| 2 | Updates tab → Filter → "Categories" tab (include / exclude categories) + "Filter excluded scanlators" | Updates → filter icon | no (69 filter has Downloaded/Unread/Started/Bookmarked only) | yes: research/MH/updates/ | CMP-U4 |
| 3 | Upcoming screen → Filter by categories | Updates → calendar icon → filter | no | yes (filter button seen) | CMP-U4 |
| 4 | Extension content warnings Safe/Mixed/18+ + "also apply to installed" | Settings → Browse | no | yes (earlier) | CMP-SR-1 (exists) |
| 5 | Extension "Not loaded" state with reason (unsigned, unsupported library version, malformed, threw error, filtered) + Copy stack trace | Browse → Extensions | no | no (needs a broken extension) | CMP-SR-2 |
| 6 | Time format (follow language / system / 12 h / 24 h) | Settings → Appearance | no | yes (earlier) | CMP-UI-1 (exists) |
| 7 | Tokyo Night theme | Appearance | no | code | CMP-UI-2 (exists) |
| 8 | Load ahead only on Wi-Fi | Settings → Reader | yes (69- Load pages ahead only on Wi-Fi, K2-3) | – | – |
| 9 | Vertical chapter navigator in (paged / long strip), on left side, height | Settings → Reader | yes / removed (side slider + left-handed exist; height was removed at Gor's request, K2-2) | – | – |
| 10 | Enable chapter name hash suffix | Advanced | yes ("Include chapter URL hash") | – | – |
| 11 | Redact sensitive headers (logs) | Advanced | yes (K4-8 hides private info) | – | – |
| 12 | Refresh tracker profile (button + result messages) | Settings → Tracking | no | no (needs a tracker account) | noted only (small; needs login to test) |
| 13 | MangaBaka tracker status "Considering", score types (kitsu/mangabaka) | Tracking | no (trackers MangaBaka/Hikka/Comick noted in K8-9) | no (accounts) | K8-9 note |
| 14 | App update: "Skip this version", "Downloading… (%)" | About → update | not applicable (69 builds come from GitHub Actions, in-app updater unused) | – | – |
| 15 | "Finishing the update" screen while migrating data | app start after update | no (cosmetic) | no | – |
| 16 | Support us / donation screens (4+9 strings) | More | not applicable | – | – |
| 17 | "WebView stopped working and the page was closed" message | WebView | no (tiny safety message) | no | – |
| 18 | Category filter help texts for Updates/Upcoming | – | part of #2/#3 | – | – |
| 19 | `pinned_catalogues`, `last_version_code`, `targetAppId`, `donation_campaign_shown` | internal | 69 uses other key names for the same things / not applicable | – | – |

Status: every non-matching item is classified above. Phone-checked: 1, 1a–1e, 2, 3, 4, 6. Not checkable without accounts/broken extension: 5, 12, 13.
