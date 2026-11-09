package eu.kanade.presentation.manga.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import tachiyomi.i18n.MR
import tachiyomi.i18n.kmk.KMR
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.domain.manga.model.MangaCover as DomainMangaCover

// KMK -->
/** 69 mod D1: a cover found on another source. */
data class Mod69FoundCover(val sourceId: Long, val sourceName: String, val title: String, val url: String)

@Composable
fun Mod69CoverPickDialog(
    covers: List<Mod69FoundCover>,
    searching: Boolean,
    searchedSources: Int,
    totalSources: Int,
    onPick: (Mod69FoundCover) -> Unit,
    onDismissRequest: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text(stringResource(KMR.strings.mod69_find_covers)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (searching) {
                    Text(
                        stringResource(KMR.strings.mod69_find_covers_progress, searchedSources, totalSources, covers.size),
                        style = MaterialTheme.typography.bodySmall,
                    )
                    LinearProgressIndicator(
                        progress = { if (totalSources > 0) searchedSources / totalSources.toFloat() else 0f },
                        modifier = Modifier.fillMaxWidth(),
                    )
                } else if (covers.isEmpty()) {
                    Text(stringResource(KMR.strings.mod69_find_covers_none))
                }
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    modifier = Modifier.fillMaxWidth().heightIn(max = 460.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    items(covers, key = { it.sourceId.toString() + it.url }) { cover ->
                        Column {
                            MangaCover.Book(
                                data = DomainMangaCover(
                                    mangaId = -1L,
                                    sourceId = cover.sourceId,
                                    isMangaFavorite = false,
                                    ogUrl = cover.url,
                                    lastModified = 0L,
                                ),
                                onClick = { onPick(cover) },
                            )
                            Text(
                                text = cover.sourceName,
                                style = MaterialTheme.typography.labelSmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismissRequest) { Text(stringResource(MR.strings.action_cancel)) }
        },
    )
}
// KMK <--
