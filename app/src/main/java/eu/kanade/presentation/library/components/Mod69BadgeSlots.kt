package eu.kanade.presentation.library.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
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

// KMK -->
/** 69 mod: how far the entry is read (0..1), drawn as a bar under the cover. Null = no bar. */
val LocalMod69CoverProgress = compositionLocalOf<Float?> { null }

/** Gives the covers inside [content] the reading progress bar of [libraryItem] when "Progress bar" is picked. */
@Composable
fun Mod69CoverProgress(libraryItem: LibraryItem, content: @Composable () -> Unit) {
    val prefs = remember { Injekt.get<LibraryPreferences>() }
    val style by prefs.mod69BadgeStyle().collectAsState()
    val total = libraryItem.libraryManga.totalChapters
    val progress = if (style == "bar" && total > 0) {
        (libraryItem.libraryManga.readCount.toFloat() / total).coerceIn(0f, 1f)
    } else {
        null
    }
    CompositionLocalProvider(LocalMod69CoverProgress provides progress, content = content)
}

/** The bar itself, at the bottom of a cover box. Uses the app theme colour. */
@Composable
fun BoxScope.Mod69CoverProgressBar(progress: Float?) {
    if (progress == null) return
    Box(
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .fillMaxWidth()
            .height(4.dp)
            .clip(RoundedCornerShape(bottomStart = 4.dp, bottomEnd = 4.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(progress)
                .fillMaxHeight()
                .background(MaterialTheme.colorScheme.primary),
        )
    }
}
// KMK <--
