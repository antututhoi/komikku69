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

    /** The staging folder names typed in the settings, lowercased. */
    private fun excludedNames(): List<String> =
        downloadPreferences.mod69ExcludedFolderCategories().get()
            .split(',')
            .map { it.trim().lowercase() }
            .filter { it.isNotEmpty() }

    /**
     * True when the category (or folder) name is a staging one (e.g. "z 0"): entries wait there
     * until they're made smaller / downloaded again, then move to their next category's folder.
     */
    fun isExcluded(name: String): Boolean {
        val n = name.trim().lowercase()
        val folder = folderName(name).lowercase()
        return excludedNames().any { n.startsWith(it) || folder.startsWith(folderName(it).lowercase()) }
    }

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
     * Earlier downloads in the source folder are moved there by the caller.
     */
    fun folderForNewDownload(source: Source, title: String): String? {
        if (isDecided(source.id, title)) return folderOf(source.id, title)
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
            val folder = folderOf(manga.source, manga.ogTitle)
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

    /**
     * When the entry's downloads sit in a staging folder (e.g. "z 0"), moves them to the folder of its
     * next category that isn't a staging one (or Default). Returns the new folder, or null when nothing moved.
     */
    suspend fun leaveStaging(context: Context, manga: Manga): String? {
        if (!enabled() || excludedNames().isEmpty()) return null
        val source = Injekt.get<tachiyomi.domain.source.service.SourceManager>().get(manga.source) ?: return null
        val dir = Injekt.get<DownloadProvider>().findMangaDir(manga.ogTitle, source) ?: return null
        val here = dir.parentFile?.name ?: return null
        if (!isExcluded(here)) return null
        val target = categoriesOf(manga).firstOrNull()?.let { folderName(it) } ?: DEFAULT_FOLDER
        return if (moveEntry(context, manga, source, target)) target else null
    }

    /** True when every downloaded page of the entry is WebP (= made smaller already). */
    private fun looksProcessed(entryDir: UniFile): Boolean {
        var pages = 0
        for (ch in entryDir.listFiles().orEmpty()) {
            val n = ch.name.orEmpty().lowercase()
            if (ch.isDirectory) {
                for (f in ch.listFiles().orEmpty()) {
                    val fn = f.name.orEmpty().lowercase()
                    if (fn.endsWith(".jpg") || fn.endsWith(".jpeg") || fn.endsWith(".png")) return false
                    if (fn.endsWith(".webp")) pages++
                }
            } else if (n.endsWith(".cbz")) {
                runCatching {
                    java.util.zip.ZipInputStream(ch.openInputStream().buffered()).use { zin ->
                        while (true) {
                            val e = zin.nextEntry ?: break
                            val en = e.name.lowercase()
                            if (en.endsWith(".jpg") || en.endsWith(".jpeg") || en.endsWith(".png")) return false
                            if (en.endsWith(".webp")) pages++
                        }
                    }
                }
            }
        }
        return pages > 0
    }

    /**
     * Moves every entry in a staging folder that's already been made smaller (all pages WebP)
     * to its next category's folder. Shows progress and a summary. Returns how many moved.
     */
    suspend fun moveProcessedOutOfStaging(context: Context): Int {
        if (excludedNames().isEmpty()) return 0
        val provider = Injekt.get<DownloadProvider>()
        val sources = Injekt.get<tachiyomi.domain.source.service.SourceManager>()
        val p = eu.kanade.tachiyomi.data.notification.Mod69Progress
        val id = p.ID_MOVE
        val title = "Moving finished entries out of staging folders"
        val started = System.currentTimeMillis()
        p.start(context, id, title)
        val favorites = Injekt.get<GetFavorites>().await()
        var moved = 0
        var notDone = 0
        var failed = 0
        for ((i, manga) in favorites.withIndex()) {
            if (p.isStopped(id)) break
            val source = sources.get(manga.source) ?: continue
            val dir = provider.findMangaDir(manga.ogTitle, source) ?: continue
            if (!isExcluded(dir.parentFile?.name ?: continue)) continue
            p.update(context, id, title, manga.title, i + 1, favorites.size)
            if (!looksProcessed(dir)) {
                notDone++
                continue
            }
            try {
                if (leaveStaging(context, manga) != null) moved++ else failed++
            } catch (e: Exception) {
                failed++
                logcat(LogPriority.ERROR, e) { "69: moving ${manga.title} out of staging failed" }
            }
        }
        Injekt.get<DownloadCache>().invalidateCache()
        p.finish(
            context,
            id,
            "Moving finished entries done",
            listOf(
                "Moved to their next category: $moved",
                "Still waiting (not made smaller yet): $notDone",
                "Failed: $failed",
                "Time: ${p.duration(System.currentTimeMillis() - started)}",
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
        if (!enabled() || repairing) return
        val catNames = Injekt.get<GetCategories>().await()
            .filter { !it.isSystemCategory }
            .associate { it.id to (it.order to folderName(it.name)) }
        val sources = Injekt.get<tachiyomi.domain.source.service.SourceManager>()
        var changed = false
        for (item in list) {
            val manga = item.manga
            if (!isDecided(manga.source, manga.ogTitle)) continue
            val current = folderOf(manga.source, manga.ogTitle) ?: continue // kept in the source folder by choice
            val folders = item.categories.mapNotNull { catNames[it] }.sortedBy { it.first }.map { it.second }
            if (current in folders) continue
            // category changed: follow it to its (first) category, or the Default folder
            val target = folders.firstOrNull() ?: DEFAULT_FOLDER
            if (target == current) continue
            val source = sources.get(manga.source) ?: continue
            // never move the same entry twice within 10 minutes (stops back-and-forth moving)
            val k = key(manga.source, manga.ogTitle)
            val now = System.currentTimeMillis()
            if ((recentMoves[k] ?: 0L) > now - 10 * 60_000L) continue
            recentMoves[k] = now
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
                    // library entries only (others never decide a folder)
                    byDirName = Injekt.get<GetFavorites>().await()
                        .flatMap { m -> provider.getMangaDirNames(m.ogTitle).map { it.lowercase() to m } }
                        .groupBy({ it.first }, { it.second })
                }
                val candidates = byDirName[name.lowercase()].orEmpty()
                // entries whose own categories point at this folder
                val fitting = candidates.filter { manga -> fits(manga, folder) }
                // same title in several sources and none of them fits: we can't tell whose it is, leave it
                val chosen = when {
                    fitting.isNotEmpty() -> fitting
                    candidates.size == 1 && !isDecided(candidates[0].source, candidates[0].ogTitle) -> candidates
                    else -> emptyList()
                }
                chosen.forEach { manga ->
                    if (folderOf(manga.source, manga.ogTitle) != folder) remember(manga.source, manga.ogTitle, folder)
                }
            }
        }
    }

    /** True when [folder] is one of the entry's category folders (or Default when it has none). */
    private suspend fun fits(manga: Manga, folder: String): Boolean {
        val cats = categoriesOf(manga).map { folderName(it).lowercase() }
        return if (cats.isEmpty()) folder.equals(DEFAULT_FOLDER, true) else folder.lowercase() in cats
    }

    /**
     * Repair: puts every library entry's downloads into the folder of its first category (staging ones like
     * "z 0" skipped; Default when it has none), merging copies found in other category folders or the
     * source folder, and resets what was remembered. Shows progress and a summary.
     */
    suspend fun repairAll(context: Context): Int {
        val provider = Injekt.get<DownloadProvider>()
        val sources = Injekt.get<tachiyomi.domain.source.service.SourceManager>()
        val root = Injekt.get<StorageManager>().getDownloadsDirectory() ?: return 0
        val p = eu.kanade.tachiyomi.data.notification.Mod69Progress
        val id = p.ID_MOVE
        val title = "Fixing download folders"
        val started = System.currentTimeMillis()
        p.start(context, id, title)
        repairing = true
        var moved = 0
        var ok = 0
        var unsure = 0
        var failed = 0
        var notFound = 0
        val report = mutableListOf<String>()
        try {
            val favorites = Injekt.get<GetFavorites>().await()
            val sourceDirOf = favorites.map { it.source }.distinct()
                .mapNotNull { sid -> sources.get(sid)?.let { sid to provider.getSourceDirName(it).lowercase() } }
                .toMap()
            val sourceDirNames = sourceDirOf.values.toSet()
            // every entry folder, per simplified name (only letters and digits, so small name
            // differences from a PC or another tool still match): (top folder name, the folder)
            val byName = HashMap<String, MutableList<Pair<String, UniFile>>>()
            root.listFiles().orEmpty().filter { it.isDirectory }.forEach { top ->
                val topName = top.name?.lowercase() ?: return@forEach
                top.listFiles().orEmpty().filter { it.isDirectory }.forEach { child ->
                    val n = simple(child.name ?: return@forEach)
                    if (n.isNotEmpty()) byName.getOrPut(n) { mutableListOf() } += topName to child
                }
            }
            report += "Download folder: ${root.uri}"
            report += "Top folders: ${root.listFiles().orEmpty().filter { it.isDirectory }.joinToString { it.name.orEmpty() }}"
            report += ""
            val sameTitle = favorites.groupBy { it.ogTitle.lowercase() }
            for ((i, manga) in favorites.withIndex()) {
                if (p.isStopped(id)) break
                p.update(context, id, title, manga.title, i + 1, favorites.size)
                val names = provider.getMangaDirNames(manga.ogTitle).map { simple(it) }.toSet()
                val ownSourceDir = sourceDirOf[manga.source]
                val shared = (sameTitle[manga.ogTitle.lowercase()]?.size ?: 1) > 1
                val target = categoriesOf(manga).firstOrNull()?.let { folderName(it) } ?: DEFAULT_FOLDER
                val found = names.flatMap { byName[it].orEmpty() }.filter { (top, _) ->
                    when {
                        top == ownSourceDir -> true
                        top in sourceDirNames -> false // another source's folder
                        shared -> false // same title in 2 sources: only its own source folder is certain
                        else -> true
                    }
                }
                val line = "${manga.title} | categories: ${categoriesOf(manga).joinToString()} | target: $target"
                if (found.isEmpty()) {
                    if (shared && names.any { byName.containsKey(it) }) {
                        unsure++
                        report += "$line | SAME TITLE IN 2 SOURCES, left alone: " +
                            names.flatMap { byName[it].orEmpty() }.joinToString { "${it.first}/${it.second.name}" }
                    } else {
                        notFound++
                    }
                    continue
                }
                report += "$line | found in: ${found.joinToString { "${it.first}/${it.second.name}" }}"
                remember(manga.source, manga.ogTitle, target)
                val parent = root.createDirectory(target) ?: run {
                    failed++
                    continue
                }
                var movedThis = false
                for ((top, dir) in found) {
                    if (top == target.lowercase()) continue
                    try {
                        if (move(context, dir, parent) != null) {
                            movedThis = true
                        } else {
                            failed++
                            report += "   couldn't move $top/${dir.name}"
                        }
                    } catch (e: Exception) {
                        failed++
                        logcat(LogPriority.ERROR, e) { "69: fixing ${manga.title} failed" }
                    }
                }
                if (movedThis) moved++ else ok++
            }
            // empty category folders left behind
            root.listFiles().orEmpty().filter { it.isDirectory && it.listFiles().isNullOrEmpty() }
                .filter { it.name?.lowercase() !in sourceDirNames }
                .forEach { it.delete() }
        } catch (e: Exception) {
            report += "Stopped by an error: $e"
            logcat(LogPriority.ERROR, e) { "69: fixing download folders failed" }
        } finally {
            repairing = false
        }
        // a report next to the downloads, so what happened to each entry can be checked
        runCatching {
            root.findFile(REPORT_NAME)?.delete()
            root.createFile(REPORT_NAME)?.openOutputStream()?.use { it.write(report.joinToString("\n").toByteArray()) }
        }
        Injekt.get<DownloadCache>().invalidateCache()
        p.finish(
            context,
            id,
            "Fixing download folders done",
            listOf(
                "Moved into the right folder: $moved",
                "Already right: $ok",
                "Same title in two sources, left alone: $unsure",
                "No download folder found: $notFound",
                "Failed: $failed",
                "Details: $REPORT_NAME in the download folder",
                "Time: ${p.duration(System.currentTimeMillis() - started)}",
            ),
        )
        return moved
    }

    /**
     * Sort by a backup file: every library entry in the backup gets its downloads moved into the folder of
     * its first category in that backup (Default when it has none). Uses the same loose name matching as
     * "Fix download folders", shows progress and writes a report. Returns how many entries moved.
     */
    suspend fun sortFromBackup(context: Context, uri: android.net.Uri): Int {
        val provider = Injekt.get<DownloadProvider>()
        val sources = Injekt.get<tachiyomi.domain.source.service.SourceManager>()
        val root = Injekt.get<StorageManager>().getDownloadsDirectory() ?: return 0
        val p = eu.kanade.tachiyomi.data.notification.Mod69Progress
        val id = p.ID_MOVE
        val title = "Sorting downloads by backup"
        val started = System.currentTimeMillis()
        p.start(context, id, title)
        repairing = true
        var moved = 0
        var ok = 0
        var unsure = 0
        var notFound = 0
        var failed = 0
        val report = mutableListOf<String>()
        try {
            val backup = eu.kanade.tachiyomi.data.backup.BackupDecoder(context).decode(uri)
            val catByOrder = backup.backupCategories.associateBy { it.order }
            val entries = backup.backupManga.filter { it.favorite }
            val sourceDirOf = entries.map { it.source }.distinct()
                .mapNotNull { sid -> sources.get(sid)?.let { sid to provider.getSourceDirName(it).lowercase() } }
                .toMap()
            val sourceDirNames = sourceDirOf.values.toSet()
            val byName = HashMap<String, MutableList<Pair<String, UniFile>>>()
            root.listFiles().orEmpty().filter { it.isDirectory }.forEach { top ->
                val topName = top.name?.lowercase() ?: return@forEach
                top.listFiles().orEmpty().filter { it.isDirectory }.forEach { child ->
                    val n = simple(child.name ?: return@forEach)
                    if (n.isNotEmpty()) byName.getOrPut(n) { mutableListOf() } += topName to child
                }
            }
            val sameTitle = entries.groupBy { simple(it.title) }
            report += "Backup: ${entries.size} library entries, ${backup.backupCategories.size} categories"
            report += ""
            for ((i, bm) in entries.withIndex()) {
                if (p.isStopped(id)) break
                p.update(context, id, title, bm.title, i + 1, entries.size)
                val cats = bm.categories.mapNotNull { catByOrder[it] }.sortedBy { it.order }.map { it.name }
                val target = cats.firstOrNull()?.let { folderName(it) } ?: DEFAULT_FOLDER
                val names = provider.getMangaDirNames(bm.title).map { simple(it) }.toSet() + simple(bm.title)
                val ownSourceDir = sourceDirOf[bm.source]
                val shared = (sameTitle[simple(bm.title)]?.size ?: 1) > 1
                val found = names.flatMap { byName[it].orEmpty() }.distinctBy { it.second.uri }.filter { (top, _) ->
                    when {
                        top == ownSourceDir -> true
                        top in sourceDirNames -> false
                        shared -> false
                        else -> true
                    }
                }
                val line = "${bm.title} | categories: ${cats.joinToString()} | target: $target"
                if (found.isEmpty()) {
                    if (shared && names.any { byName.containsKey(it) }) {
                        unsure++
                        report += "$line | same title twice in the backup, left alone"
                    } else {
                        notFound++
                    }
                    continue
                }
                remember(bm.source, bm.title, target)
                val parent = root.createDirectory(target) ?: run {
                    failed++
                    continue
                }
                var movedThis = false
                for ((top, dir) in found) {
                    if (top == target.lowercase()) continue
                    try {
                        if (move(context, dir, parent) != null) {
                            movedThis = true
                        } else {
                            failed++
                            report += "   couldn't move $top/${dir.name}"
                        }
                    } catch (e: Exception) {
                        failed++
                        report += "   couldn't move $top/${dir.name}: ${e.message}"
                    }
                }
                report += "$line | ${if (movedThis) "moved from" else "already in"}: ${found.joinToString { it.first }}"
                if (movedThis) moved++ else ok++
            }
            root.listFiles().orEmpty().filter { it.isDirectory && it.listFiles().isNullOrEmpty() }
                .filter { it.name?.lowercase() !in sourceDirNames }
                .forEach { it.delete() }
        } catch (e: Exception) {
            report += "Stopped by an error: $e"
            logcat(LogPriority.ERROR, e) { "69: sorting by backup failed" }
        } finally {
            repairing = false
        }
        runCatching {
            root.findFile(SORT_REPORT_NAME)?.delete()
            root.createFile(SORT_REPORT_NAME)?.openOutputStream()?.use { it.write(report.joinToString("\n").toByteArray()) }
        }
        Injekt.get<DownloadCache>().invalidateCache()
        p.finish(
            context,
            id,
            "Sorting downloads by backup done",
            listOf(
                "Moved into their category folder: $moved",
                "Already right: $ok",
                "Same title twice, left alone: $unsure",
                "No download folder found: $notFound",
                "Failed: $failed",
                "Details: $SORT_REPORT_NAME in the download folder",
                "Time: ${p.duration(System.currentTimeMillis() - started)}",
            ),
        )
        return moved
    }

    private const val SORT_REPORT_NAME = "69 sort by backup report.txt"

    private const val REPORT_NAME = "69 fix download folders report.txt"

    /** Only letters and digits, lowercased: folder names that differ only in symbols / spaces match. */
    private fun simple(name: String): String = name.lowercase().filter { it.isLetterOrDigit() }

    @Volatile
    private var repairing = false

    /** When onLibrary last moved each entry (key -> time), so it can never move one back and forth. */
    private val recentMoves = java.util.concurrent.ConcurrentHashMap<String, Long>()
}
// KMK <--
