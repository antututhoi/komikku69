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
    fun process(file: UniFile, tmpDir: UniFile, filename: String, chapterId: Long): UniFile {
        if (modeFor(chapterId) != SMALLER) return file
        return shrinkFile(file, tmpDir, filename)
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
        val maxW = prefs.mod69PageMaxWidth().let { if (it == 0) Int.MAX_VALUE else it.coerceAtLeast(100) }
        val tall = prefs.mod69KeepTallPages().get() && opts.outHeight > opts.outWidth * 2.5
        val maxH = prefs.mod69PageMaxHeight().let { if (it == 0 || tall) Int.MAX_VALUE else it.coerceAtLeast(100) }
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
            var done = 0
            chapters.groupBy { it.mangaId }.forEach { (mangaId, list) ->
                val manga = getManga.await(mangaId) ?: return@forEach
                val source = sources.get(manga.source) ?: return@forEach
                list.forEach { ch ->
                    val dir = provider.findChapterDir(ch.name, ch.scanlator, ch.url, manga.ogTitle, source) ?: return@forEach
                    if (convertOne(dir)) done++
                }
            }
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
                    if (isShrinkable(n) && !n.endsWith(".webp", ignoreCase = true)) {
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
                    if (isShrinkable(entryName) && !entryName.endsWith(".webp", ignoreCase = true)) {
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
            val dm = Injekt.get<DownloadManager>()
            val sources = Injekt.get<SourceManager>()
            val getManga = Injekt.get<GetManga>()
            chapters.groupBy { it.mangaId }.forEach { (mangaId, list) ->
                val manga = getManga.await(mangaId) ?: return@forEach
                val source = sources.get(manga.source) ?: return@forEach
                redownloadOne(dm, manga, source, list, mode)
            }
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
        var count = 0
        for (manga in Injekt.get<GetFavorites>().await()) {
            val source = sources.get(manga.source) ?: continue
            val downloaded = getChapters.await(manga.id).filter {
                dm.isChapterDownloaded(it.name, it.scanlator, it.url, manga.ogTitle, manga.source)
            }
            if (downloaded.isEmpty()) continue
            count += downloaded.size
            redownloadOne(dm, manga, source, downloaded, mode)
        }
        return count
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
