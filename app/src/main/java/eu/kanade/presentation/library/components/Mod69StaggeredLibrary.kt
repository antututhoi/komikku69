package eu.kanade.presentation.library.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import eu.kanade.tachiyomi.ui.library.LibraryItem
import tachiyomi.domain.library.model.LibraryManga
import tachiyomi.domain.manga.model.MangaCover

// KMK -->
/** 69 mod L2: covers in their own shape, packed like a Pinterest board. */
@Composable
fun Mod69StaggeredLibrary(
    items: List<LibraryItem>,
    columns: Int,
    contentPadding: PaddingValues,
    selection: Set<Long>,
    onClick: (LibraryManga) -> Unit,
    onLongClick: (LibraryManga) -> Unit,
) {
    LazyVerticalStaggeredGrid(
        columns = if (columns > 0) StaggeredGridCells.Fixed(columns) else StaggeredGridCells.Adaptive(128.dp),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 8.dp,
            end = 8.dp,
            top = 8.dp,
            bottom = contentPadding.calculateBottomPadding() + 8.dp,
        ),
        verticalItemSpacing = 6.dp,
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(6.dp),
    ) {
        items(items, key = { it.id }) { libraryItem ->
            val manga = libraryItem.libraryManga.manga
            val selected = manga.id in selection
            Column(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .then(
                        if (selected) {
                            Modifier.border(3.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(6.dp))
                        } else {
                            Modifier
                        },
                    )
                    .combinedClickable(
                        onClick = { onClick(libraryItem.libraryManga) },
                        onLongClick = { onLongClick(libraryItem.libraryManga) },
                    ),
            ) {
                Box {
                    AsyncImage(
                        model = MangaCover(
                            mangaId = manga.id,
                            sourceId = manga.source,
                            isMangaFavorite = manga.favorite,
                            ogUrl = manga.thumbnailUrl,
                            lastModified = manga.coverLastModified,
                        ),
                        contentDescription = null,
                        contentScale = ContentScale.FillWidth,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 80.dp)
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                    )
                    Row(modifier = Modifier.align(Alignment.TopStart).padding(4.dp)) {
                        Mod69Badges(libraryItem, "start")
                    }
                    Row(modifier = Modifier.align(Alignment.TopEnd).padding(4.dp)) {
                        Mod69Badges(libraryItem, "end")
                    }
                }
                Text(
                    text = manga.title,
                    style = MaterialTheme.typography.labelMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp),
                )
            }
        }
    }
}
// KMK <--
