package eu.kanade.tachiyomi.data.download

import android.app.Application
import eu.kanade.tachiyomi.source.Source
import logcat.LogPriority
import tachiyomi.core.common.util.system.logcat
import tachiyomi.domain.chapter.model.Chapter
import tachiyomi.domain.download.service.DownloadPreferences
import tachiyomi.domain.manga.model.Manga
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

// KMK -->
/**
 * 69 mod: when an entry is migrated, its downloaded chapters are moved over to the new entry
 * (matched by chapter number) instead of being deleted.
 */
object Mod69MigrateDownloads {

    fun enabled(): Boolean = Injekt.get<DownloadPreferences>().mod69MigrateMoveDownloads().get()

    /** Moves the matching downloaded chapters. Returns how many were moved. */
    fun move(
        current: Manga,
        currentSource: Source,
        target: Manga,
        targetSource: Source,
        oldChapters: List<Chapter>,
        newChapters: List<Chapter>,
    ): Int {
        val provider = Injekt.get<DownloadProvider>()
        val context = Injekt.get<Application>()
        val oldDir = provider.findMangaDir(current.ogTitle, currentSource) ?: return 0
        val newDir = provider.getMangaDir(target.ogTitle, targetSource).getOrNull() ?: return 0
        if (oldDir.uri == newDir.uri) return 0
        var moved = 0
        for (nc in newChapters) {
            if (!nc.isRecognizedNumber) continue
            val oc = oldChapters.find { it.isRecognizedNumber && it.chapterNumber == nc.chapterNumber } ?: continue
            try {
                val src = provider.getValidChapterDirNames(oc.name, oc.scanlator, oc.url)
                    .firstNotNullOfOrNull { oldDir.findFile(it) }
                    ?: continue
                val isCbz = src.name.orEmpty().endsWith(".cbz", ignoreCase = true)
                val newName = provider.getChapterDirName(nc.name, nc.scanlator, nc.url) + if (isCbz) ".cbz" else ""
                if (newDir.findFile(newName) != null) continue
                val movedFile = Mod69DownloadFolders.moveDir(context, src, newDir) ?: continue
                if (movedFile.name != newName) movedFile.renameTo(newName)
                moved++
            } catch (e: Exception) {
                logcat(LogPriority.ERROR, e) { "69: moving ${oc.name} to the migrated entry failed" }
            }
        }
        if (oldDir.listFiles().isNullOrEmpty()) oldDir.delete()
        if (moved > 0) Injekt.get<DownloadCache>().invalidateCache()
        return moved
    }
}
// KMK <--
