package eu.kanade.presentation.manga.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
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
// KMK <--
