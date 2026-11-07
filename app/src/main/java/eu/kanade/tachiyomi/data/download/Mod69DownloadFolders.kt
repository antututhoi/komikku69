package eu.kanade.tachiyomi.data.download

import android.app.Application
import android.content.Context
import android.provider.DocumentsContract
import com.hippo.unifile.UniFile
import eu.kanade.tachiyomi.source.Source
import eu.kanade.tachiyomi.util.storage.DiskUtil
import kotlinx.coroutines.runBlocking
import logcat.LogPriority
import tachiyomi.core.common.util.system.logcat
import tachiyomi.domain.category.interactor.GetCategories
import tachiyomi.domain.download.service.DownloadPreferences
import tachiyomi.domain.manga.interactor.GetFavorites
import tachiyomi.domain.manga.model.Manga
import tachiyomi.domain.storage.service.StorageManager
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.io.File

// KMK -->
/**
 * 69 mod: downloads in a folder named after the entry's category
 * (downloads/<category>/<entry>/<chapter>) instead of the source's name.
 *
 * Which folder each entry uses is remembered here (source + title -> folder name), so the
 * download index can find entries in category folders. Entries in 2+ categories use the first
 * category; it can be changed per entry in its 3-dot menu ("69- Download folder").
 */
object Mod69DownloadFolders {

    private val prefs by lazy {
        Injekt.get<Application>().getSharedPreferences("mod69_download_folders", Context.MODE_PRIVATE)
    }
    private val downloadPreferences by lazy { Injekt.get<DownloadPreferences>() }

    /** "Source folder" choice: keep the normal layout for this entry. */
    const val SOURCE = "\u0000source"

    fun enabled(): Boolean = downloadPreferences.mod69CategoryFolders().get()

    private fun key(sourceId: Long, title: String) = "$sourceId|$title"

    /** The category folder this entry uses, or null for the source folder. */
    fun folderOf(sourceId: Long, title: String): String? =
        prefs.getString(key(sourceId, title), null)?.takeIf { it != SOURCE }

    private fun remember(sourceId: Long, title: String, folder: String?) {
        prefs.edit().putString(key(sourceId, title), folder ?: SOURCE).apply()
    }

    private fun isDecided(sourceId: Long, title: String) = prefs.contains(key(sourceId, title))

    fun folderName(categoryName: String): String = DiskUtil.buildValidFilename(categoryName)

    /** The entry's categories in their order (no "Default"). */
    private suspend fun categoriesOf(manga: Manga): List<String> =
        Injekt.get<GetCategories>().await(manga.id)
            .filter { !it.isSystemCategory && it.name.isNotBlank() }
            .sortedBy { it.order }
            .map { it.name }

    private suspend fun libraryEntry(sourceId: Long, title: String): Manga? =
        Injekt.get<GetFavorites>().await().firstOrNull { it.source == sourceId && it.ogTitle == title }

    /**
     * For a new download: the category folder to use (decides once per entry), or null = source folder.
     * Entries that already have downloads in the source folder stay there (move them with the setting).
     */
    fun folderForNewDownload(source: Source, title: String, hasSourceFolderDownloads: Boolean): String? {
        if (isDecided(source.id, title)) return folderOf(source.id, title)
        if (!enabled() || hasSourceFolderDownloads) return null
        val folder = runCatching {
            runBlocking {
                val manga = libraryEntry(source.id, title) ?: return@runBlocking null
                categoriesOf(manga).firstOrNull()?.let { folderName(it) }
            }
        }.getOrNull() ?: return null
        remember(source.id, title, folder)
        return folder
    }

    /** For the download index: (folder lowercased, entry folder lowercased) -> source ids. */
    fun index(provider: DownloadProvider): Map<Pair<String, String>, List<Long>> {
        val out = HashMap<Pair<String, String>, MutableList<Long>>()
        prefs.all.forEach { (k, v) ->
            val folder = v as? String ?: return@forEach
            if (folder == SOURCE) return@forEach
            val sourceId = k.substringBefore('|').toLongOrNull() ?: return@forEach
            val title = k.substringAfter('|')
            out.getOrPut(folder.lowercase() to provider.getMangaDirName(title).lowercase()) { mutableListOf() } += sourceId
        }
        return out
    }

    // ---------------- moving ----------------

    /** Moves [dir] into [newParent] (fast move when the storage allows it, else copy + delete). */
    private fun move(context: Context, dir: UniFile, newParent: UniFile): UniFile? {
        val name = dir.name ?: return null
        newParent.findFile(name)?.let { existing ->
            // already there: move what is inside, one by one
            dir.listFiles().orEmpty().forEach { child ->
                if (existing.findFile(child.name.orEmpty()) == null) move(context, child, existing)
            }
            if (dir.listFiles().isNullOrEmpty()) dir.delete()
            return existing
        }
        val parent = dir.parentFile
        val uri = dir.uri
        // plain files ("file://" storage)
        if (uri.scheme == "file") {
            val src = File(uri.path ?: return null)
            val dst = File(File(newParent.uri.path ?: return null), name)
            if (src.renameTo(dst)) return UniFile.fromFile(dst)
        } else if (parent != null) {
            runCatching {
                DocumentsContract.moveDocument(context.contentResolver, uri, parent.uri, newParent.uri)
            }.getOrNull()?.let { return UniFile.fromUri(context, it) }
        }
        // copy, then delete
        return copy(dir, newParent)?.also { dir.delete() }
    }

    private fun copy(src: UniFile, newParent: UniFile): UniFile? {
        val name = src.name ?: return null
        return if (src.isDirectory) {
            val d = newParent.createDirectory(name) ?: return null
            src.listFiles().orEmpty().forEach { copy(it, d) ?: return null }
            d
        } else {
            val f = newParent.createFile(name) ?: return null
            src.openInputStream().use { input -> f.openOutputStream().use { input.copyTo(it) } }
            f
        }
    }

    /** Moves one entry's downloads to [folder] (null = source folder) and remembers the choice. */
    fun moveEntry(context: Context, manga: Manga, source: Source, folder: String?): Boolean {
        val provider = Injekt.get<DownloadProvider>()
        val downloads = Injekt.get<StorageManager>().getDownloadsDirectory() ?: return false
        val current = provider.findMangaDir(manga.ogTitle, source)
        remember(manga.source, manga.ogTitle, folder)
        if (current == null) return true
        val targetParent = if (folder == null) {
            downloads.createDirectory(provider.getSourceDirName(source))
        } else {
            downloads.createDirectory(folder)
        } ?: return false
        if (current.parentFile?.uri == targetParent.uri) return true
        return move(context, current, targetParent) != null
    }

    /** Moves every library entry's downloads into its (first) category folder. Returns how many moved. */
    suspend fun moveAll(context: Context): Int {
        val provider = Injekt.get<DownloadProvider>()
        val sources = Injekt.get<eu.kanade.tachiyomi.source.SourceManager>()
        var moved = 0
        for (manga in Injekt.get<GetFavorites>().await()) {
            val source = sources.get(manga.source) ?: continue
            if (provider.findMangaDir(manga.ogTitle, source) == null) continue
            val folder = folderOf(manga.source, manga.ogTitle)
                ?: categoriesOf(manga).firstOrNull()?.let { folderName(it) }
                ?: continue
            try {
                if (moveEntry(context, manga, source, folder)) moved++
            } catch (e: Exception) {
                logcat(LogPriority.ERROR, e) { "69: moving ${manga.title} failed" }
            }
        }
        Injekt.get<DownloadCache>().invalidateCache()
        return moved
    }

    /** Folder choice for one entry: its categories + "Source folder". Returns (label, folder or null). */
    suspend fun choices(manga: Manga): List<Pair<String, String?>> =
        categoriesOf(manga).map { it to folderName(it) } + ("Source folder (normal)" to null)
}
// KMK <--
