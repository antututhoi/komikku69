package eu.kanade.presentation.manga.components

import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import eu.kanade.tachiyomi.ui.manga.ChapterList
import tachiyomi.i18n.kmk.KMR
import tachiyomi.presentation.core.i18n.stringResource

// KMK -->
/** 69 mod D5: type a number or part of a name to find a chapter in a long list. */
object Mod69ChapterSearch {
    var query by mutableStateOf("")

    fun filter(chapters: List<ChapterList>): List<ChapterList> {
        val q = query.trim().lowercase()
        if (q.isEmpty()) return chapters
        return chapters.filter {
            it is ChapterList.Item && (
                it.chapter.name.lowercase().contains(q) ||
                    it.chapter.chapterNumber.toString().removeSuffix(".0") == q ||
                    it.chapter.scanlator.orEmpty().lowercase().contains(q)
                )
        }
    }
}

@Composable
fun Mod69ChapterSearchField() {
    DisposableEffect(Unit) {
        onDispose { Mod69ChapterSearch.query = "" }
    }
    OutlinedTextField(
        value = Mod69ChapterSearch.query,
        onValueChange = { Mod69ChapterSearch.query = it },
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        singleLine = true,
        label = { Text(stringResource(KMR.strings.mod69_search_chapters)) },
    )
}

/** 69 mod D4: one row of numbered chapter tiles. */
@Composable
fun Mod69ChapterGridRow(
    items: List<ChapterList.Item>,
    columns: Int,
    label: (ChapterList.Item) -> String,
    onClick: (ChapterList.Item) -> Unit,
    onLongClick: (ChapterList.Item) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 3.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        items.forEach { item ->
            val container = when {
                item.selected -> MaterialTheme.colorScheme.primary
                item.chapter.bookmark -> MaterialTheme.colorScheme.tertiaryContainer
                item.chapter.read -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                else -> MaterialTheme.colorScheme.secondaryContainer
            }
            val content = when {
                item.selected -> MaterialTheme.colorScheme.onPrimary
                item.chapter.bookmark -> MaterialTheme.colorScheme.onTertiaryContainer
                item.chapter.read -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                else -> MaterialTheme.colorScheme.onSecondaryContainer
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .aspectRatio(1.4f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(container)
                    .combinedClickable(onClick = { onClick(item) }, onLongClick = { onLongClick(item) }),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = label(item),
                    color = content,
                    style = MaterialTheme.typography.labelLarge,
                    maxLines = 1,
                )
                if (item.downloadState == eu.kanade.tachiyomi.data.download.model.Download.State.DOWNLOADED) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(4.dp)
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(content),
                    )
                }
            }
        }
        // keep the last row's tiles the same size as the others
        repeat(columns - items.size) { Spacer(modifier = Modifier.weight(1f)) }
    }
}
// KMK <--
