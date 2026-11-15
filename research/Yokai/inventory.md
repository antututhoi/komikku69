# Yokai (null2264/yokai, nightly build 1.10.3) vs 69-Komikku (mod69 #55): inventory

Yokai is the continuation of J2K: same screens (big title + search, category hopper, Recents tab, quick filter chips, detailed stats). yokai-nightly is the same app's test build; the nightly was used on the phone.
Method: 1103 strings / 110 keys compared; 522 strings don't match 69-Komikku, 453 of them are identical to J2K's (see research/J2K/inventory.md). The 69 remaining (inv/yokai_new.txt) + the settings walk (settings/settings_text.log, compared line by line with J2K's) give:

| # | Item (Yokai only) | Where | In 69-Komikku? | Phone | Board |
|---|---|---|---|---|---|
| 1 | Long tap Recents behaviour (download queue / last read chapter), Long tap Browse behaviour (extensions-migration menu / global search) | Settings → General → Navigation | no | yes (setting) | CMP-UI-7 |
| 2 | Change App Icon (Beta) | Settings → General | no | setting seen; icon list from code | CMP-UI-8 |
| 3 | Enable chapter swipe action, Enable source swipe action | Library / Browse settings | yes (swipe actions can be disabled) | yes | CMP-SR-7 |
| 4 | Local source: Scan external storages for entries (Beta) | Advanced | no | yes (setting) | CMP-SR-7 |
| 5 | Auto-append ID to download names (Beta) | Downloads | yes (chapter hash) | yes | – |
| 6 | Extension repos, revoke trusted extensions, signing key fingerprint check | Browse / Advanced | yes | yes | – |
| 7 | Themes Doki, Hot Pink, Classic Blue, Yokai | Appearance | different theme list | onboarding | – |
| 8 | Experimental compose library, debug mode, crash test, prune workers, hardware bitmap threshold, display profile | Advanced | yes or developer-only | yes | – |
| 9 | Everything else | all | same as J2K rows 1–23 | restore + library seen | J2K rows |
