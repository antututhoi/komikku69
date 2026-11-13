# 69-Komikku: baseline inspection (virtual phone, remote control)

Build: Stable 1.14.1 (mod69 build #55, 11/11/26). Session: 13 Nov 2026. Same 20 MangaDex titles (seed.tachibk).
Screenshots: branch `ctl69-out` (`out/<block>.jpg`, readable screen list in `out/<block>.txt`).

## Checklist
| Area | Status | Notes |
|---|---|---|
| First run / onboarding | done | Theme → storage folder → permissions (install, notifications, battery, media) → guide / restore backup |
| Restore backup | done | From onboarding, opens Data and storage, then picker |
| Library grid, tabs, category panel | done | Tapping "Library" title opens category panel (K10-2) |
| Library filter / sort / display / group sheet | done | Display: 5 modes, items per row, overlays, tabs; Group tab |
| Library overflow menu | done | Update library/category/marked, Search tips, Find duplicates, My AniList list, Suggestions for you, Random, Reindex |
| Library selection mode | done | Bottom bar: categories, read, unread, download, more (Update, Migrate, Delete, Merge, MangaDex follows); top ⋮ Pin, Mark/Unmark update, Link tracker |
| Entry page | done | Cover, authors, status, 4 action buttons, description, tags, Suggestions row, Recommendations, chapter search box, chapters |
| Entry menus | done | Download menu (next 1/5/10/25, unread, bookmarked, all one copy); Filter/Sort/Display sheet; very long ⋮ menu (19 items) |
| Reader menu | done | Top: title, chapter, bookmark. Bottom: prev/next, slider, chapters, WebView, crop, page layout, settings |
| Reader settings sheet | done | Tabs Reading mode / General / Custom filter, all scrolled |
| Reader long-press menu | done | Set as cover, Copy, Share, Save, Bookmark page, Page bookmarks, Translate, Japanese words, AI sharpen, All pages |
| All pages grid, chapter list sheet | done | |
| Long strip + landscape | done | |
| Updates, History, Browse (Sources, Feed, Extensions, Migrate) | done (screens) | |
| Source browse (MangaDex) | done (first screen) | Komikku "Latest + Browse" rows view |
| More screen, Statistics, Download queue | done | |
| All top-level settings pages, scrolled to the end | done (lists) | 11 pages; option choices of each dropdown not all opened |
| Settings dropdown choices | partial | Opened only where relevant; code used for the rest (labelled) |
| Page-turn animations (K10-1) | not yet | Needs a paged chapter + video |
| Tracker login screens | not possible | Needs accounts |

## Library update problem (solved cause)
- Same in the official Komikku 1.14.1: update job ends in about 1 s and finds nothing.
- Cause: Settings → Library → Smart update → "Predict next release time" skips every entry it thinks is not due yet, also for manual updates.
- With that option off, the same update found 194 new chapters.
- Proposed fix (after research, with OK): manual updates ignore the release-time skip; the finished notification says how many entries were skipped and why.

## Problems seen in 69-Komikku (for fixes, not ideas)
1. Reader long-press menu: second row labels wrap badly ("Book mark page", "Trans late page", "sharp er with AI").
2. Entry ⋮ menu has 19 items, 6 of them "69-" items: hard to scan; could group the 69- download tools into one submenu.
3. Entry "Resume" button did nothing after the last read chapter (next chapter not on this source?), no message shown.
4. MangaDex extension shows an "18+" label in Extensions.

## Already in 69-Komikku (so NOT new): found during the walk
- Browse → "Hide entries already in library" (removed board row CMP-S4).
- Reader → "Preserve reading position on read entries" (removed board row CMP-S7).
- Advanced: Refresh library covers, Reset per-series reader settings, Update titles to match source, Disallow non-ASCII filenames, Include chapter URL hash, source blacklist, Data saver, DoH, hardware bitmap threshold, legacy long-strip decoder, custom display profile, Revoke trusted extensions.
- Security: Hide notification content, Secure screen, Incognito, CBZ password.
- Reader: E-Ink flash on page change, Auto Webtoon Mode, Center margin, Archive reader mode, custom brightness / colour filter / grayscale / invert.
