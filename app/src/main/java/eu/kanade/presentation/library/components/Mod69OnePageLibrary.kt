package eu.kanade.presentation.library.components

import android.content.res.Configuration
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import eu.kanade.core.preference.PreferenceMutableState
import eu.kanade.tachiyomi.ui.library.LibraryItem
import tachiyomi.domain.category.model.Category
import tachiyomi.domain.library.model.LibraryDisplayMode
import tachiyomi.domain.library.model.LibraryManga
import tachiyomi.domain.manga.model.MangaCover
import tachiyomi.i18n.kmk.KMR
import tachiyomi.presentation.core.i18n.stringResource

// KMK -->
/** 69 mod L1: every category in one scrolling page, with a header per category. */
object Mod69OnePage {
    /** Index of each category's header in the grid, filled while drawing (for "jump to category"). */
    val headerIndex = mutableMapOf<Long, Int>()
}

@Composable
fun Mod69OnePageLibrary(
    categories: List<Category>,
    gridState: LazyGridState,
    contentPadding: PaddingValues,
    selection: Set<Long>,
    getDisplayMode: (Int) -> PreferenceMutableState<LibraryDisplayMode>,
    getColumnsForOrientation: (Boolean) -> PreferenceMutableState<Int>,
    getItemsForCategory: (Category) -> List<LibraryItem>,
    onClickManga: (Category, LibraryManga) -> Unit,
    onLongClickManga: (Category, LibraryManga) -> Unit,
) {
    val displayMode by getDisplayMode(0)
    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    val columns by remember(isLandscape) { getColumnsForOrientation(isLandscape) }
    var collapsed by rememberSaveable { mutableStateOf(setOf<Long>()) }
    val isList = displayMode == LibraryDisplayMode.List

    LazyVerticalGrid(
        columns = when {
            isList -> GridCells.Fixed(1)
            columns == 0 -> GridCells.Adaptive(128.dp)
            else -> GridCells.Fixed(columns)
        },
        state = gridState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 8.dp,
            end = 8.dp,
            top = 4.dp,
            bottom = contentPadding.calculateBottomPadding() + 8.dp,
        ),
        horizontalArrangement = Arrangement.spacedBy(if (isList) 0.dp else 4.dp),
        verticalArrangement = Arrangement.spacedBy(if (isList) 0.dp else 4.dp),
    ) {
        var index = 0
        item(span = { GridItemSpan(maxLineSpan) }, key = "mod69-onepage-tools") {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = { collapsed = emptySet() }) {
                    Text(stringResource(KMR.strings.mod69_expand_all))
                }
                TextButton(onClick = { collapsed = categories.map { it.id }.toSet() }) {
                    Text(stringResource(KMR.strings.mod69_collapse_all))
                }
            }
        }
        index++
        categories.forEach { category ->
            val items = getItemsForCategory(category)
            val isCollapsed = category.id in collapsed
            Mod69OnePage.headerIndex[category.id] = index
            item(span = { GridItemSpan(maxLineSpan) }, key = "mod69-header-${category.id}") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            collapsed = if (isCollapsed) collapsed - category.id else collapsed + category.id
                        }
                        .padding(horizontal = 8.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "${category.name.ifBlank { "Default" }} (${items.size})",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f),
                    )
                    Icon(
                        imageVector = if (isCollapsed) Icons.Outlined.ExpandMore else Icons.Outlined.ExpandLess,
                        contentDescription = null,
                    )
                }
            }
            index++
            if (!isCollapsed) {
                items(items, key = { "mod69-${category.id}-${it.id}" }) { libraryItem ->
                    val manga = libraryItem.libraryManga.manga
                    val cover = MangaCover(
                        mangaId = manga.id,
                        sourceId = manga.source,
                        isMangaFavorite = manga.favorite,
                        ogUrl = manga.thumbnailUrl,
                        lastModified = manga.coverLastModified,
                    )
                    if (isList) {
                        MangaListItem(
                            isSelected = manga.id in selection,
                            title = manga.title,
                            coverData = cover,
                            badge = { Mod69Badges(libraryItem, "start") },
                            onClick = { onClickManga(category, libraryItem.libraryManga) },
                            onLongClick = { onLongClickManga(category, libraryItem.libraryManga) },
                        )
                    } else {
                        MangaCompactGridItem(
                            isSelected = manga.id in selection,
                            title = manga.title,
                            coverData = cover,
                            coverBadgeStart = { Mod69Badges(libraryItem, "start") },
                            coverBadgeEnd = { Mod69Badges(libraryItem, "end") },
                            onClick = { onClickManga(category, libraryItem.libraryManga) },
                            onLongClick = { onLongClickManga(category, libraryItem.libraryManga) },
                        )
                    }
                }
                index += items.size
            }
        }
    }
}
// KMK <--
