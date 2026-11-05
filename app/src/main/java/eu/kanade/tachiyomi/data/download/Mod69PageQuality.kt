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
        val name = file.name.orEmpty().lowercase()
        if (name.endsWith(".gif") || name.endsWith(".avif") || name.endsWith(".jxl")) return file
        return try {
            val before = file.length()
            val bytes = file.openInputStream().use { it.readBytes() }
            val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, opts)
            if (opts.outWidth <= 0) return file
            val maxW = prefs.mod69PageMaxWidth().get().coerceAtLeast(360)
            var sample = 1
            while (opts.outWidth / (sample * 2) >= maxW) sample *= 2
            val decoded = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, BitmapFactory.Options().apply { inSampleSize = sample })
                ?: return file
            val bmp = if (decoded.width > maxW) {
                val h = (decoded.height.toLong() * maxW / decoded.width).toInt().coerceAtLeast(1)
                Bitmap.createScaledBitmap(decoded, maxW, h, true).also { if (it !== decoded) decoded.recycle() }
            } else {
                decoded
            }
            val out = ByteArrayOutputStream()

            @Suppress("DEPRECATION")
            val format = if (android.os.Build.VERSION.SDK_INT >= 30) Bitmap.CompressFormat.WEBP_LOSSY else Bitmap.CompressFormat.WEBP
            bmp.compress(format, prefs.mod69PageWebpQuality().get().coerceIn(30, 100), out)
            bmp.recycle()
            if (out.size() <= 0 || out.size() >= before) return file
            val newFile = tmpDir.createFile("$filename.webp.tmp") ?: return file
            newFile.openOutputStream().use { it.write(out.toByteArray()) }
            file.delete()
            newFile.renameTo("$filename.webp")
            tmpDir.findFile("$filename.webp") ?: newFile
        } catch (e: Throwable) {
            logcat(LogPriority.ERROR, e) { "69: page shrink failed" }
            file
        }
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
