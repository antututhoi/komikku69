package eu.kanade.presentation.library.components

import androidx.compose.foundation.layout.RowScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import eu.kanade.tachiyomi.ui.library.LibraryItem
import tachiyomi.domain.library.service.LibraryPreferences
import tachiyomi.presentation.core.util.collectAsState
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

// KMK -->
/**
 * 69 mod L9: the badges of a library cover, each in the corner the user picked
 * ("start" = top left, "end" = top right, "hide").
 */
@Composable
internal fun RowScope.Mod69Badges(libraryItem: LibraryItem, side: String) {
    val prefs = remember { Injekt.get<LibraryPreferences>() }
    val downloads by prefs.mod69BadgeDownloads().collectAsState()
    val unread by prefs.mod69BadgeUnread().collectAsState()
    val language by prefs.mod69BadgeLanguage().collectAsState()
    val source by prefs.mod69BadgeSource().collectAsState()

    if (downloads == side) DownloadsBadge(count = libraryItem.downloadCount)
    if (unread == side) {
        UnreadBadge(
            count = libraryItem.unreadCount,
            readCount = libraryItem.libraryManga.readCount,
            totalChapters = libraryItem.libraryManga.totalChapters,
        )
    }
    if (language == side) {
        LanguageBadge(
            isLocal = libraryItem.isLocal,
            sourceLanguage = libraryItem.sourceLanguage,
            useLangIcon = libraryItem.useLangIcon,
        )
    }
    if (source == side) SourceIconBadge(source = libraryItem.source)
}
// KMK <--
