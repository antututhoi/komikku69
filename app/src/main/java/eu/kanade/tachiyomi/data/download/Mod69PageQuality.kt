package eu.kanade.tachiyomi.data.download

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.hippo.unifile.UniFile
import eu.kanade.tachiyomi.source.Source
import kotlinx.coroutines.delay
import logcat.LogPriority
import tachiyomi.core.common.util.lang.launchIO
import tachiyomi.core.common.util.system.logcat
import tachiyomi.domain.chapter.interactor.GetChapter
import tachiyomi.domain.chapter.interactor.GetChaptersByMangaId
import tachiyomi.domain.chapter.model.Chapter
import tachiyomi.domain.download.service.DownloadPreferences
import tachiyomi.domain.manga.interactor.GetFavorites
import tachiyomi.domain.manga.interactor.GetManga
import tachiyomi.domain.manga.model.Manga
import tachiyomi.domain.source.service.SourceManager
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.io.ByteArrayOutputStream

// KMK -->
/**
 * 69 mod: page quality for downloads.
 * "original" keeps the pages as the source sends them, "smaller" resizes to a max width and saves as WebP
 * (kept original when the new file is not smaller). A redownload can choose the quality for its chapters.
 */
object Mod69PageQuality {
    const val ORIGINAL = "original"
    const val SMALLER = "smaller"

    private val prefs by lazy { Injekt.get<DownloadPreferences>() }

    /** chapterId -> quality, for chapters queued by "redownload". */
    private val overrides = java.util.concurrent.ConcurrentHashMap<Long, String>()

    fun modeFor(chapterId: Long): String = overrides[chapterId] ?: prefs.mod69PageQuality().get()

    fun done(chapterId: Long) {
        overrides.remove(chapterId)
    }

    /** Called for every newly downloaded page. Returns the file to use (maybe a new one). */
    fun process(file: UniFile, tmpDir: UniFile, filename: String, chapterId: Long, mangaId: Long = -1L): UniFile {
        if (modeFor(chapterId) != SMALLER) return file
        if (isEntryExcluded(mangaId)) return file
        return shrinkFile(file, tmpDir, filename)
    }

    fun isEntryExcluded(mangaId: Long): Boolean = mangaId.toString() in prefs.mod69NoResizeEntries().get()

    /** Turns "never make this entry's downloads smaller" on or off. Returns the new state (true = excluded). */
    fun toggleEntryExcluded(mangaId: Long): Boolean {
        val key = mangaId.toString()
        val set = prefs.mod69NoResizeEntries().get()
        val excluded = key !in set
        prefs.mod69NoResizeEntries().set(if (excluded) set + key else set - key)
        return excluded
    }

    private fun isShrinkable(name: String): Boolean {
        val n = name.lowercase()
        return n.endsWith(".jpg") || n.endsWith(".jpeg") || n.endsWith(".png") || n.endsWith(".webp") || n.endsWith(".bmp")
    }

    /**
     * A smaller WebP of the picture (fits inside max width x max height, keeps its shape),
     * or null when it would not be smaller.
     */
    private fun shrinkBytes(bytes: ByteArray): ByteArray? {
        val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, opts)
        if (opts.outWidth <= 0 || opts.outHeight <= 0) return null
        // fit inside max width x max height (0 = no limit), keeping the page's shape
        // long webtoon strips (much taller than wide, or much wider than tall) are left as they are
        val long = opts.outHeight > opts.outWidth * 2.5 || opts.outWidth > opts.outHeight * 2.5
        if (prefs.mod69KeepTallPages().get() && long) return null
        val maxW = prefs.mod69PageMaxWidth().let { if (it == 0) Int.MAX_VALUE else it.coerceAtLeast(100) }
        val maxH = prefs.mod69PageMaxHeight().let { if (it == 0) Int.MAX_VALUE else it.coerceAtLeast(100) }
        val scale = minOf(1.0, maxW.toDouble() / opts.outWidth, maxH.toDouble() / opts.outHeight)
        val targetW = (opts.outWidth * scale).toInt().coerceAtLeast(1)
        val targetH = (opts.outHeight * scale).toInt().coerceAtLeast(1)
        var sample = 1
        while (opts.outWidth / (sample * 2) >= targetW && opts.outHeight / (sample * 2) >= targetH) sample *= 2
        val decoded = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, BitmapFactory.Options().apply { inSampleSize = sample })
            ?: return null
        val bmp = if (decoded.width > targetW || decoded.height > targetH) {
            Bitmap.createScaledBitmap(decoded, targetW, targetH, true).also { if (it !== decoded) decoded.recycle() }
        } else {
            decoded
        }
        val out = ByteArrayOutputStream()

        @Suppress("DEPRECATION")
        val format = if (android.os.Build.VERSION.SDK_INT >= 30) Bitmap.CompressFormat.WEBP_LOSSY else Bitmap.CompressFormat.WEBP
        bmp.compress(format, prefs.mod69PageWebpQuality(), out)
        bmp.recycle()
        return if (out.size() <= 0 || out.size() >= bytes.size) null else out.toByteArray()
    }

    private fun shrinkFile(file: UniFile, dir: UniFile, filename: String): UniFile {
        val name = file.name.orEmpty()
        if (!isShrinkable(name)) return file
        return try {
            val bytes = file.openInputStream().use { it.readBytes() }
            val small = shrinkBytes(bytes) ?: return file
            val newFile = dir.createFile("$filename.webp.tmp") ?: return file
            newFile.openOutputStream().use { it.write(small) }
            file.delete()
            newFile.renameTo("$filename.webp")
            dir.findFile("$filename.webp") ?: newFile
        } catch (e: Throwable) {
            logcat(LogPriority.ERROR, e) { "69: page shrink failed" }
            file
        }
    }

    /** Makes already downloaded chapters smaller right on the phone (nothing is downloaded again). */
    fun convertLocal(chapters: List<Chapter>, onDone: ((Int) -> Unit)? = null) {
        if (chapters.isEmpty()) return
        launchIO {
            val provider = Injekt.get<DownloadProvider>()
            val sources = Injekt.get<SourceManager>()
            val getManga = Injekt.get<GetManga>()
            val context = Injekt.get<android.app.Application>()
            val p = eu.kanade.tachiyomi.data.notification.Mod69Progress
            val id = p.ID_CONVERT
            val title = "Making downloads smaller"
            val started = System.currentTimeMillis()
            p.start(context, id, title)
            var done = 0
            var failed = 0
            var missing = 0
            var count = 0
            var before = 0L
            var after = 0L
            fun sizeOf(f: UniFile): Long =
                if (f.isDirectory) f.listFiles().orEmpty().sumOf { it.length() } else f.length()
            run loop@{
                chapters.groupBy { it.mangaId }.forEach { (mangaId, list) ->
                    if (isEntryExcluded(mangaId)) return@forEach
                    val manga = getManga.await(mangaId) ?: return@forEach
                    val source = sources.get(manga.source) ?: return@forEach
                    list.forEach { ch ->
                        if (p.isStopped(id)) return@loop
                        count++
                        p.update(context, id, title, "${manga.title} – ${ch.name}", count, chapters.size)
                        val dir = provider.findChapterDir(ch.name, ch.scanlator, ch.url, manga.ogTitle, source)
                        if (dir == null) {
                            missing++
                            return@forEach
                        }
                        val b = sizeOf(dir)
                        if (convertOne(dir)) {
                            done++
                            before += b
                            after += provider.findChapterDir(ch.name, ch.scanlator, ch.url, manga.ogTitle, source)
                                ?.let { sizeOf(it) } ?: b
                        } else {
                            failed++
                        }
                    }
                }
            }
            // 69 mod: entries waiting in a staging folder (e.g. "z 0") move to their next category now
            val leftStaging = chapters.map { it.mangaId }.distinct().count { mangaId ->
                val manga = getManga.await(mangaId) ?: return@count false
                runCatching { Mod69DownloadFolders.leaveStaging(context, manga) != null }.getOrDefault(false)
            }
            if (leftStaging > 0) Injekt.get<DownloadCache>().invalidateCache()
            p.finish(
                context,
                id,
                "Making downloads smaller finished",
                listOf(
                    "Chapters made smaller: $done",
                    "Size: ${p.size(before)} → ${p.size(after)} (saved ${p.size((before - after).coerceAtLeast(0))})",
                    "Not found: $missing",
                    "Failed: $failed",
                    "Moved out of the staging folder: $leftStaging",
                    "Time: ${p.duration(System.currentTimeMillis() - started)}",
                ),
            )
            onDone?.invoke(done)
        }
    }

    fun convertLocalIds(chapterIds: List<Long>) {
        launchIO {
            val getChapter = Injekt.get<GetChapter>()
            convertLocal(chapterIds.mapNotNull { getChapter.await(it) })
        }
    }

    /** Every downloaded chapter of every library entry, made smaller on the phone. */
    suspend fun convertEverything(): Int {
        val dm = Injekt.get<DownloadManager>()
        val getChapters = Injekt.get<GetChaptersByMangaId>()
        val all = mutableListOf<Chapter>()
        for (manga in Injekt.get<GetFavorites>().await()) {
            all += getChapters.await(manga.id).filter {
                dm.isChapterDownloaded(it.name, it.scanlator, it.url, manga.ogTitle, manga.source)
            }
        }
        convertLocal(all)
        return all.size
    }

    private fun convertOne(target: UniFile): Boolean {
        return try {
            if (target.isDirectory) {
                target.listFiles()?.forEach { f ->
                    val n = f.name ?: return@forEach
                    // WebP pages too: many sources send big WebP pages
                    if (isShrinkable(n)) {
                        shrinkFile(f, target, n.substringBeforeLast('.'))
                    }
                }
                true
            } else if (target.name.orEmpty().endsWith(".cbz", ignoreCase = true)) {
                convertCbz(target)
            } else {
                false
            }
        } catch (e: Throwable) {
            logcat(LogPriority.ERROR, e) { "69: convert failed" }
            false
        }
    }

    /** Re-packs a CBZ with smaller pages. */
    private fun convertCbz(cbz: UniFile): Boolean {
        val parent = cbz.parentFile ?: return false
        val name = cbz.name ?: return false
        val tmp = parent.createFile("$name.69tmp") ?: return false
        var changed = false
        java.util.zip.ZipInputStream(cbz.openInputStream().buffered()).use { zin ->
            java.util.zip.ZipOutputStream(tmp.openOutputStream().buffered()).use { zout ->
                while (true) {
                    val e = zin.nextEntry ?: break
                    if (e.isDirectory) continue
                    val bytes = zin.readBytes()
                    var entryName = e.name
                    var data = bytes
                    if (isShrinkable(entryName)) {
                        shrinkBytes(bytes)?.let {
                            data = it
                            entryName = entryName.substringBeforeLast('.') + ".webp"
                            changed = true
                        }
                    }
                    zout.putNextEntry(java.util.zip.ZipEntry(entryName))
                    zout.write(data)
                    zout.closeEntry()
                }
            }
        }
        if (!changed) {
            tmp.delete()
            return true
        }
        cbz.delete()
        return tmp.renameTo(name)
    }

    /** Deletes the given downloaded chapters and downloads them again in [mode]. */
    fun redownload(chapters: List<Chapter>, mode: String) {
        if (chapters.isEmpty()) return
        launchIO {
            val sources = Injekt.get<SourceManager>()
            val getManga = Injekt.get<GetManga>()
            val groups = chapters.groupBy { it.mangaId }.mapNotNull { (mangaId, list) ->
                val manga = getManga.await(mangaId) ?: return@mapNotNull null
                val source = sources.get(manga.source) ?: return@mapNotNull null
                Triple(manga, source, list)
            }
            runRedownload(groups, mode)
        }
    }

    fun redownloadIds(chapterIds: List<Long>, mode: String) {
        launchIO {
            val getChapter = Injekt.get<GetChapter>()
            redownload(chapterIds.mapNotNull { getChapter.await(it) }, mode)
        }
    }

    /** Every downloaded chapter of every library entry. Returns how many were queued. */
    suspend fun redownloadEverything(mode: String): Int {
        val dm = Injekt.get<DownloadManager>()
        val sources = Injekt.get<SourceManager>()
        val getChapters = Injekt.get<GetChaptersByMangaId>()
        val groups = mutableListOf<Triple<Manga, Source, List<Chapter>>>()
        for (manga in Injekt.get<GetFavorites>().await()) {
            val source = sources.get(manga.source) ?: continue
            val downloaded = getChapters.await(manga.id).filter {
                dm.isChapterDownloaded(it.name, it.scanlator, it.url, manga.ogTitle, manga.source)
            }
            if (downloaded.isNotEmpty()) groups += Triple(manga, source, downloaded)
        }
        launchIO { runRedownload(groups, mode) }
        return groups.sumOf { it.third.size }
    }

    /**
     * Deletes and queues the chapters again, with a progress notification while they download
     * and a summary at the end (chapters done, size before -> after, time).
     */
    private suspend fun runRedownload(groups: List<Triple<Manga, Source, List<Chapter>>>, mode: String) {
        if (groups.isEmpty()) return
        val dm = Injekt.get<DownloadManager>()
        val provider = Injekt.get<DownloadProvider>()
        val context = Injekt.get<android.app.Application>()
        val p = eu.kanade.tachiyomi.data.notification.Mod69Progress
        val id = p.ID_REDOWNLOAD
        val title = if (mode == SMALLER) "Downloading again, smaller" else "Downloading again, original quality"
        val started = System.currentTimeMillis()
        val total = groups.sumOf { it.third.size }
        fun sizeOf(f: UniFile): Long =
            if (f.isDirectory) f.listFiles().orEmpty().sumOf { it.length() } else f.length()
        fun sizeNow(): Long = groups.sumOf { (manga, source, list) ->
            list.sumOf { ch ->
                provider.findChapterDir(ch.name, ch.scanlator, ch.url, manga.ogTitle, source)?.let { sizeOf(it) } ?: 0L
            }
        }
        p.start(context, id, title)
        p.update(context, id, title, "Measuring the current size…", 0, 0, force = true)
        val before = sizeNow()
        for ((i, g) in groups.withIndex()) {
            if (p.isStopped(id)) break
            p.update(context, id, title, "Queueing ${g.first.title}", i + 1, groups.size)
            redownloadOne(dm, g.first, g.second, g.third, mode)
        }
        // wait while they download (the download notification shows the details of each page)
        val ids = groups.flatMap { g -> g.third.map { it.id } }.toSet()
        val deadline = System.currentTimeMillis() + 12 * 60 * 60 * 1000L
        while (System.currentTimeMillis() < deadline && !p.isStopped(id)) {
            val left = dm.queueState.value.count { it.chapter.id in ids }
            p.update(context, id, title, "${total - left} of $total chapters downloaded", total - left, total)
            if (left == 0) break
            delay(2000)
        }
        val done = groups.sumOf { (manga, _, list) ->
            list.count { dm.isChapterDownloaded(it.name, it.scanlator, it.url, manga.ogTitle, manga.source) }
        }
        val after = sizeNow()
        // 69 mod: entries waiting in a staging folder (e.g. "z 0") move to their next category now
        val leftStaging = if (mode == SMALLER) {
            groups.count { (manga, _, _) ->
                runCatching { Mod69DownloadFolders.leaveStaging(context, manga) != null }.getOrDefault(false)
            }.also { if (it > 0) Injekt.get<DownloadCache>().invalidateCache() }
        } else {
            0
        }
        p.finish(
            context,
            id,
            "Downloading again finished",
            listOf(
                "Chapters downloaded again: $done of $total",
                "Not downloaded (failed or cancelled): ${total - done}",
                "Size: ${p.size(before)} → ${p.size(after)}" +
                    if (after < before) " (saved ${p.size(before - after)})" else "",
                "Moved out of the staging folder: $leftStaging",
                "Time: ${p.duration(System.currentTimeMillis() - started)}",
            ),
        )
    }

    private suspend fun redownloadOne(dm: DownloadManager, manga: Manga, source: Source, chapters: List<Chapter>, mode: String) {
        chapters.forEach { overrides[it.id] = mode }
        dm.deleteChapters(chapters, manga, source, ignoreCategoryExclusion = true)
        // deleting runs in the background; wait until the files are gone before queueing again
        repeat(50) {
            if (chapters.none { dm.isChapterDownloaded(it.name, it.scanlator, it.url, manga.ogTitle, manga.source) }) {
                return@repeat
            }
            delay(200)
        }
        dm.downloadChapters(manga, chapters)
    }
}
// KMK <--
