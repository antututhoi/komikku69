package eu.kanade.tachiyomi.data.download

import android.content.Context
import com.hippo.unifile.UniFile
import eu.kanade.tachiyomi.data.notification.Mod69Progress
import tachiyomi.domain.chapter.interactor.GetChaptersByMangaId
import tachiyomi.domain.manga.interactor.GetAllManga
import tachiyomi.domain.source.service.SourceManager
import tachiyomi.domain.storage.service.StorageManager
import tachiyomi.source.local.isLocal
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

// KMK -->
/**
 * 69 mod: renames downloaded entry and chapter folders / .cbz files to the current naming settings
 * (short Windows-safe names, scanlator name in chapter names). Komikku finds every chapter afterwards.
 * Each folder is listed once, so this stays quick with big libraries.
 */
object Mod69RenameDownloads {

    data class Result(
        val entries: Int,
        val entryRenamed: Int,
        val chapterRenamed: Int,
        val alreadyOk: Int,
        val nameTaken: Int,
        val failed: Int,
    )

    suspend fun run(context: Context): Result {
        val title = "Renaming downloads"
        val provider = Injekt.get<DownloadProvider>()
        val sources = Injekt.get<SourceManager>()
        val getChapters = Injekt.get<GetChaptersByMangaId>()
        val downloads = Injekt.get<StorageManager>().getDownloadsDirectory()
            ?: return Result(0, 0, 0, 0, 0, 0)
        val started = System.currentTimeMillis()
        Mod69Progress.start(context, Mod69Progress.ID_RENAME, title)

        // top folders (sources and category folders), each listed only when needed
        val tops = downloads.listFiles().orEmpty().filter { it.isDirectory && !it.name.isNullOrBlank() }
            .associateBy { it.name!!.lowercase() }
        val children = HashMap<String, MutableMap<String, UniFile>>()
        fun childrenOf(top: UniFile): MutableMap<String, UniFile> = children.getOrPut(top.name!!.lowercase()) {
            top.listFiles().orEmpty().filter { !it.name.isNullOrBlank() }
                .associateByTo(HashMap()) { it.name!!.lowercase() }
        }

        val mangas = Injekt.get<GetAllManga>().await()
        var entries = 0
        var entryRenamed = 0
        var chapterRenamed = 0
        var alreadyOk = 0
        var nameTaken = 0
        var failed = 0
        for ((index, manga) in mangas.withIndex()) {
            if (Mod69Progress.isStopped(Mod69Progress.ID_RENAME)) break
            val source = sources.getOrStub(manga.source)
            if (source.isLocal()) continue
            val names = provider.getMangaDirNames(manga.ogTitle)
            val topNames = listOfNotNull(
                Mod69DownloadFolders.folderOf(source.id, manga.ogTitle),
                provider.getSourceDirName(source),
            )
            var top: UniFile? = null
            var dir: UniFile? = null
            for (t in topNames) {
                val topDir = tops[t.lowercase()] ?: continue
                val found = names.firstNotNullOfOrNull { childrenOf(topDir)[it.lowercase()] } ?: continue
                top = topDir
                dir = found
                break
            }
            if (dir == null || top == null || !dir.isDirectory) continue
            entries++
            Mod69Progress.update(context, Mod69Progress.ID_RENAME, title, manga.title, index + 1, mangas.size)

            // the entry folder
            val wanted = provider.getMangaDirName(manga.ogTitle)
            if (dir.name != wanted) {
                val siblings = childrenOf(top)
                if (siblings[wanted.lowercase()] != null && !wanted.equals(dir.name, ignoreCase = true)) {
                    nameTaken++
                } else if (runCatching { dir.renameTo(wanted) }.getOrDefault(false)) {
                    siblings.remove(dir.name.orEmpty().lowercase())
                    dir = top.findFile(wanted) ?: dir
                    siblings[wanted.lowercase()] = dir
                    entryRenamed++
                } else {
                    failed++
                }
            }

            // its chapters
            val files = dir.listFiles().orEmpty().filter { !it.name.isNullOrBlank() }
                .associateByTo(HashMap()) { it.name!! }
            if (files.isEmpty()) continue
            for (chapter in getChapters.await(manga.id)) {
                val existing = provider.getValidChapterDirNames(chapter.name, chapter.scanlator, chapter.url)
                    .firstNotNullOfOrNull { files[it] } ?: continue
                val base = provider.getChapterDirName(chapter.name, chapter.scanlator, chapter.url)
                val want = if (existing.name!!.endsWith(".cbz")) "$base.cbz" else base
                when {
                    existing.name == want -> alreadyOk++
                    files.containsKey(want) -> nameTaken++
                    runCatching { existing.renameTo(want) }.getOrDefault(false) -> {
                        files.remove(existing.name!!)
                        files[want] = existing
                        chapterRenamed++
                    }
                    else -> failed++
                }
            }
        }

        Injekt.get<DownloadCache>().invalidateCache()
        val result = Result(entries, entryRenamed, chapterRenamed, alreadyOk, nameTaken, failed)
        Mod69Progress.finish(
            context,
            Mod69Progress.ID_RENAME,
            "Renaming downloads finished",
            listOf(
                "Entries with downloads: ${result.entries}",
                "Entry folders renamed: ${result.entryRenamed}",
                "Chapters renamed: ${result.chapterRenamed}",
                "Chapters already right: ${result.alreadyOk}",
                "Skipped (name already used): ${result.nameTaken}",
                "Failed: ${result.failed}",
                "Time: ${Mod69Progress.duration(System.currentTimeMillis() - started)}",
            ),
        )
        return result
    }
}
// KMK <--
