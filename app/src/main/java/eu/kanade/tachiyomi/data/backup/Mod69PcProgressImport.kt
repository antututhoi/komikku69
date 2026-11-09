package eu.kanade.tachiyomi.data.backup

import android.content.Context
import android.net.Uri
import eu.kanade.tachiyomi.data.notification.Mod69Progress
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import tachiyomi.domain.chapter.interactor.GetChapterByUrlAndMangaId
import tachiyomi.domain.chapter.interactor.UpdateChapter
import tachiyomi.domain.chapter.model.ChapterUpdate
import tachiyomi.domain.manga.interactor.GetMangaByUrlAndSourceId
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

// KMK -->
/** 69 mod N36: brings back what you read on 69-Komikku-PC (its 69-komikku-pc-progress.json file). */
object Mod69PcProgressImport {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @Serializable
    data class Item(
        val source: Long,
        val mangaUrl: String,
        val chapterUrl: String,
        val read: Boolean,
        val lastPageRead: Long,
        val time: Long = 0,
    )

    @Serializable
    data class FileContent(val app: String = "", val items: List<Item> = emptyList())

    fun start(context: Context, uri: Uri) {
        scope.launch {
            val id = Mod69Progress.ID_PC_IMPORT
            val title = "Importing reading from PC"
            Mod69Progress.start(context, id, title)
            val items = runCatching {
                val text = context.contentResolver.openInputStream(uri)!!.use { it.readBytes().decodeToString() }
                Json { ignoreUnknownKeys = true }.decodeFromString(FileContent.serializer(), text).items
            }.getOrElse {
                Mod69Progress.finish(context, id, "Import failed", listOf("That is not a 69-Komikku-PC progress file"))
                return@launch
            }
            val getManga = Injekt.get<GetMangaByUrlAndSourceId>()
            val getChapter = Injekt.get<GetChapterByUrlAndMangaId>()
            val updates = mutableListOf<ChapterUpdate>()
            var missing = 0
            items.forEachIndexed { index, item ->
                Mod69Progress.update(context, id, title, item.chapterUrl, index, items.size)
                val manga = getManga.await(item.mangaUrl, item.source)
                val chapter = manga?.let { getChapter.await(item.chapterUrl, it.id) }
                if (chapter == null) {
                    missing++
                    return@forEachIndexed
                }
                updates += ChapterUpdate(
                    id = chapter.id,
                    // never mark a chapter unread because of the PC
                    read = if (item.read) true else null,
                    lastPageRead = if (!chapter.read && !item.read) item.lastPageRead else null,
                )
            }
            Injekt.get<UpdateChapter>().awaitAll(updates)
            Mod69Progress.finish(
                context,
                id,
                "Reading from PC imported",
                listOf("Updated ${updates.size} chapters" + if (missing > 0) ", $missing not found in this library" else ""),
            )
        }
    }
}
// KMK <--
