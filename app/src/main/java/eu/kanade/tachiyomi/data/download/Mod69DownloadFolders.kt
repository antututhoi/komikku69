package eu.kanade.tachiyomi.data.download

import android.app.Application
import android.content.Context
import android.provider.DocumentsContract
import com.hippo.unifile.UniFile
import eu.kanade.tachiyomi.source.Source
import eu.kanade.tachiyomi.util.storage.DiskUtil
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch
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

    /** Folder for library entries in the Default category (same name as the 69-Komikku PC tool). */
    const val DEFAULT_FOLDER = "Default"

    /** Folder for downloads of entries that aren't in the library (same name as the PC tool). */
    const val NO_INFO_FOLDER = "uncategories"

    fun enabled(): Boolean = downloadPreferences.mod69CategoryFolders().get()

    private fun key(sourceId: Long, title: String) = "$sourceId|$title"

    /** The category folder this entry uses, or null for the source folder. */
    fun folderOf(sourceId: Long, title: String): String? =
        prefs.getString(key(sourceId, title), null)?.takeIf { it != SOURCE }

    private fun remember(sourceId: Long, title: String, folder: String?) {
        prefs.edit().putString(key(sourceId, title), folder ?: SOURCE).apply()
    }

    fun isDecided(sourceId: Long, title: String) = prefs.contains(key(sourceId, title))

    /** Remembers the folder chosen for an entry (null = source folder). */
    fun choose(sourceId: Long, title: String, folder: String?) = remember(sourceId, title, folder)

    /**
     * The category folders to ask about before downloading, or empty when there's nothing to ask:
     * the setting is off, the folder is already chosen, or the entry has fewer than 2 categories.
     */
    suspend fun folderChoicesToAsk(manga: Manga): List<Pair<String, String>> {
        if (!enabled() || isDecided(manga.source, manga.ogTitle)) return emptyList()
        val cats = categoriesOf(manga)
        if (cats.size < 2) return emptyList()
        return cats.map { it to folderName(it) }
    }

    /** Moves [dir] into [newParent] (public for the download provider). */
    fun moveDir(context: Context, dir: UniFile, newParent: UniFile): UniFile? = move(context, dir, newParent)

    fun folderName(categoryName: String): String = DiskUtil.buildValidFilename(categoryName)

    /** The excluded names typed in the settings, lowercased. */
    private fun excludedNames(): List<String> =
        downloadPreferences.mod69ExcludedFolderCategories().get()
            .split(',')
            .map { it.trim().lowercase() }
            .filter { it.isNotEmpty() }

    /** True when the category (or folder) name is one the user doesn't want as a download folder. */
    fun isExcluded(name: String): Boolean {
        val n = name.trim().lowercase()
        val folder = folderName(name).lowercase()
        return excludedNames().any { n.startsWith(it) || folder.startsWith(folderName(it).lowercase()) }
    }

    /** The entry's categories in their order (no "Default", no excluded ones). */
    private suspend fun categoriesOf(manga: Manga): List<String> =
        Injekt.get<GetCategories>().await(manga.id)
            .filter { !it.isSystemCategory && it.name.isNotBlank() && !isExcluded(it.name) }
            .sortedBy { it.order }
            .map { it.name }

    private suspend fun libraryEntry(sourceId: Long, title: String): Manga? =
        Injekt.get<GetFavorites>().await().firstOrNull { it.source == sourceId && it.ogTitle == title }

    /**
     * For a new download: the category folder to use (decides once per entry), or null = source folder.
     * Earlier downloads in the source folder are moved there by the caller.
     */
    fun folderForNewDownload(source: Source, title: String): String? {
        if (isDecided(source.id, title)) {
            val kept = folderOf(source.id, title)
            if (kept == null || !isExcluded(kept)) return kept
        }
        if (!enabled()) return null
        val folder = runCatching {
            runBlocking {
                val manga = libraryEntry(source.id, title) ?: return@runBlocking NO_INFO_FOLDER
                categoriesOf(manga).firstOrNull()?.let { folderName(it) } ?: DEFAULT_FOLDER
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
            provider.getMangaDirNames(title).forEach { name ->
                out.getOrPut(folder.lowercase() to name.lowercase()) { mutableListOf() } += sourceId
            }
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
        val sources = Injekt.get<tachiyomi.domain.source.service.SourceManager>()
        val id = eu.kanade.tachiyomi.data.notification.Mod69Progress.ID_MOVE
        val progress = eu.kanade.tachiyomi.data.notification.Mod69Progress
        val title = "Moving downloads into category folders"
        val started = System.currentTimeMillis()
        progress.start(context, id, title)
        val favorites = Injekt.get<GetFavorites>().await()
        var moved = 0
        var already = 0
        var noDownloads = 0
        var failed = 0
        for ((index, manga) in favorites.withIndex()) {
            if (progress.isStopped(id)) break
            progress.update(context, id, title, manga.title, index + 1, favorites.size)
            val source = sources.get(manga.source) ?: continue
            val dir = provider.findMangaDir(manga.ogTitle, source)
            if (dir == null) {
                noDownloads++
                continue
            }
            val folder = folderOf(manga.source, manga.ogTitle)?.takeUnless { isExcluded(it) }
                ?: categoriesOf(manga).firstOrNull()?.let { folderName(it) }
                ?: DEFAULT_FOLDER
            if (dir.parentFile?.name.equals(folder, ignoreCase = true)) {
                remember(manga.source, manga.ogTitle, folder)
                already++
                continue
            }
            try {
                if (moveEntry(context, manga, source, folder)) moved++ else failed++
            } catch (e: Exception) {
                failed++
                logcat(LogPriority.ERROR, e) { "69: moving ${manga.title} failed" }
            }
        }
        Injekt.get<DownloadCache>().invalidateCache()
        progress.finish(
            context,
            id,
            "Moving downloads finished",
            listOf(
                "Moved: $moved",
                "Already in the right folder: $already",
                "Library entries without downloads: $noDownloads",
                "Failed: $failed",
                "Time: ${progress.duration(System.currentTimeMillis() - started)}",
            ),
        )
        return moved
    }

    /** Folder choice for one entry: its categories + "Source folder". Returns (label, folder or null). */
    suspend fun choices(manga: Manga): List<Pair<String, String?>> =
        categoriesOf(manga).map { it to folderName(it) }.ifEmpty { listOf(DEFAULT_FOLDER to DEFAULT_FOLDER) } +
            ("Source folder (normal)" to null)

    // ---------------- following the app's categories ----------------

    /**
     * Renames a category folder when the category is renamed, and moves an entry's folder when the
     * entry is taken out of the category it is kept in (to its first category, or the source folder).
     */
    @OptIn(kotlinx.coroutines.FlowPreview::class)
    fun watch(scope: kotlinx.coroutines.CoroutineScope) {
        val context = Injekt.get<Application>()
        scope.launch(kotlinx.coroutines.Dispatchers.IO) {
            Injekt.get<GetCategories>().subscribe()
                .debounce(2_000)
                .collect { cats -> runCatching { onCategories(context, cats) } }
        }
        scope.launch(kotlinx.coroutines.Dispatchers.IO) {
            Injekt.get<tachiyomi.domain.manga.interactor.GetLibraryManga>().subscribe()
                .debounce(3_000)
                .collect { list -> runCatching { onLibrary(context, list) } }
        }
    }

    private fun onCategories(context: Context, cats: List<tachiyomi.domain.category.model.Category>) {
        val known = context.getSharedPreferences("mod69_category_names", Context.MODE_PRIVATE)
        val renames = mutableListOf<Pair<String, String>>()
        val edit = known.edit()
        cats.filter { !it.isSystemCategory }.forEach { c ->
            val now = folderName(c.name)
            val before = known.getString(c.id.toString(), null)
            if (before != null && before != now) renames += before to now
            edit.putString(c.id.toString(), now)
        }
        edit.apply()
        if (renames.isEmpty() || !enabled()) return
        val downloads = Injekt.get<StorageManager>().getDownloadsDirectory() ?: return
        var changed = false
        renames.forEach { (old, new) ->
            val dir = downloads.findFile(old)
            if (dir != null && dir.isDirectory) {
                if (downloads.findFile(new) == null) {
                    dir.renameTo(new)
                } else {
                    // a folder with the new name already exists: move the entries into it
                    val target = downloads.findFile(new)!!
                    dir.listFiles().orEmpty().forEach { move(context, it, target) }
                    if (dir.listFiles().isNullOrEmpty()) dir.delete()
                }
            }
            // entries kept in the old folder are now in the new one
            val e = prefs.edit()
            prefs.all.forEach { (k, v) -> if (v == old) e.putString(k, new) }
            e.apply()
            changed = true
        }
        if (changed) Injekt.get<DownloadCache>().invalidateCache()
    }

    private suspend fun onLibrary(context: Context, list: List<tachiyomi.domain.library.model.LibraryManga>) {
        if (!enabled()) return
        val catNames = Injekt.get<GetCategories>().await()
            .filter { !it.isSystemCategory && !isExcluded(it.name) }
            .associate { it.id to (it.order to folderName(it.name)) }
        val sources = Injekt.get<tachiyomi.domain.source.service.SourceManager>()
        var changed = false
        for (item in list) {
            val manga = item.manga
            if (!isDecided(manga.source, manga.ogTitle)) continue
            val current = folderOf(manga.source, manga.ogTitle) ?: continue // kept in the source folder by choice
            val folders = item.categories.mapNotNull { catNames[it] }.sortedBy { it.first }.map { it.second }
            if (current in folders && !isExcluded(current)) continue
            // category changed: follow it to its (first) category, or the Default folder
            val target = folders.firstOrNull() ?: DEFAULT_FOLDER
            if (target == current) continue
            val source = sources.get(manga.source) ?: continue
            if (moveEntry(context, manga, source, target)) changed = true
        }
        if (changed) Injekt.get<DownloadCache>().invalidateCache()
    }

    /**
     * Downloads put into category folders outside the app (e.g. sorted on a PC) are learned:
     * a top folder named like a category (or Default / uncategories), holding a folder named like an entry.
     */
    suspend fun learn(provider: DownloadProvider, topDirs: List<UniFile>, sourceDirNames: Set<String>) {
        val categoryFolders = Injekt.get<GetCategories>().await()
            .filter { !it.isSystemCategory }
            .map { folderName(it.name) }
            .plus(listOf(DEFAULT_FOLDER, NO_INFO_FOLDER))
            .associateBy { it.lowercase() }
        var byDirName: Map<String, List<Manga>>? = null
        for (top in topDirs) {
            val topName = top.name?.lowercase() ?: continue
            if (topName in sourceDirNames) continue
            val folder = categoryFolders[topName] ?: continue
            for (child in top.listFiles().orEmpty()) {
                if (!child.isDirectory) continue
                val name = child.name ?: continue
                if (byDirName == null) {
                    // every entry the app knows (not only the library: uncategories holds the others)
                    byDirName = Injekt.get<tachiyomi.domain.manga.interactor.GetAllManga>().await()
                        .flatMap { m -> provider.getMangaDirNames(m.ogTitle).map { it.lowercase() to m } }
                        .groupBy({ it.first }, { it.second })
                }
                byDirName[name.lowercase()]?.forEach { manga ->
                    if (folderOf(manga.source, manga.ogTitle) != folder) remember(manga.source, manga.ogTitle, folder)
                }
            }
        }
    }
}
// KMK <--
