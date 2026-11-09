package eu.kanade.presentation.manga.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import tachiyomi.core.common.util.lang.withIOContext
import tachiyomi.domain.chapter.model.Chapter
import tachiyomi.domain.history.interactor.GetHistory
import tachiyomi.domain.history.model.History
import tachiyomi.i18n.MR
import tachiyomi.i18n.kmk.KMR
import tachiyomi.presentation.core.i18n.stringResource
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.text.DateFormat
import java.time.Instant
import java.time.ZoneId

// KMK -->
/** 69 mod D8: reading record of one entry (time, days, first / last read, timeline to jump back). */
object Mod69ReadingRecord {
    var openFor by mutableStateOf<Long?>(null)
}

private fun readable(ms: Long): String {
    val minutes = ms / 60_000
    return when {
        minutes >= 60 -> "${minutes / 60} h ${minutes % 60} min"
        minutes > 0 -> "$minutes min"
        ms > 0 -> "${ms / 1000} s"
        else -> "0"
    }
}

@Composable
fun Mod69ReadingRecordDialog(
    mangaId: Long,
    chapters: List<Chapter>,
    onOpenChapter: (Chapter) -> Unit,
    onDismissRequest: () -> Unit,
) {
    val history by produceState<List<History>?>(initialValue = null, mangaId) {
        value = withIOContext {
            runCatching { Injekt.get<GetHistory>().await(mangaId) }.getOrDefault(emptyList())
        }
    }
    val byId = chapters.associateBy { it.id }
    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text(stringResource(KMR.strings.mod69_reading_record)) },
        text = {
            val list = history
            when {
                list == null -> Text("…")
                list.isEmpty() -> Text(stringResource(KMR.strings.mod69_reading_record_empty))
                else -> {
                    val dated = list.filter { it.readAt != null }.sortedByDescending { it.readAt }
                    val total = list.sumOf { it.readDuration.coerceAtLeast(0) }
                    val days = dated.map {
                        Instant.ofEpochMilli(it.readAt!!.time).atZone(ZoneId.systemDefault()).toLocalDate()
                    }.distinct().size
                    val format = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT)
                    Column {
                        Text(stringResource(KMR.strings.mod69_record_time, readable(total)))
                        Text(stringResource(KMR.strings.mod69_record_chapters, list.size))
                        Text(stringResource(KMR.strings.mod69_record_days, days))
                        dated.lastOrNull()?.readAt?.let {
                            Text(stringResource(KMR.strings.mod69_record_first, format.format(it)))
                        }
                        dated.firstOrNull()?.readAt?.let {
                            Text(stringResource(KMR.strings.mod69_record_last, format.format(it)))
                        }
                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                        Text(
                            stringResource(KMR.strings.mod69_record_timeline),
                            style = MaterialTheme.typography.titleSmall,
                        )
                        LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 280.dp)) {
                            items(dated.take(50), key = { it.id }) { item ->
                                val chapter = byId[item.chapterId]
                                TextButton(
                                    onClick = {
                                        if (chapter != null) {
                                            onDismissRequest()
                                            onOpenChapter(chapter)
                                        }
                                    },
                                    enabled = chapter != null,
                                ) {
                                    Text(
                                        text = "${format.format(item.readAt!!)} · ${chapter?.name ?: "?"}" +
                                            if (item.readDuration > 0) " · ${readable(item.readDuration)}" else "",
                                        style = MaterialTheme.typography.bodySmall,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismissRequest) { Text(stringResource(MR.strings.action_ok)) }
        },
    )
}
// KMK <--
