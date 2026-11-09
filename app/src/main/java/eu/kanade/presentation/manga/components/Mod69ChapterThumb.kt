package eu.kanade.presentation.manga.components

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.hippo.unifile.UniFile
import eu.kanade.tachiyomi.data.download.DownloadProvider
import tachiyomi.core.common.util.lang.withIOContext
import tachiyomi.domain.chapter.model.Chapter
import tachiyomi.domain.manga.model.Manga
import tachiyomi.domain.source.service.SourceManager
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.io.InputStream
import java.util.concurrent.ConcurrentHashMap
import java.util.zip.ZipInputStream

// KMK -->
/** 69 mod P14: small picture of the first page of a downloaded chapter. */
object Mod69ChapterThumbs {
    private val cache = ConcurrentHashMap<Long, Bitmap>()
    private val missing = ConcurrentHashMap.newKeySet<Long>()
    private val imageExt = setOf("jpg", "jpeg", "png", "webp", "gif", "avif", "jxl", "heif", "heic")

    fun cached(chapterId: Long): Bitmap? = cache[chapterId]

    suspend fun load(manga: Manga, chapter: Chapter): Bitmap? {
        cache[chapter.id]?.let { return it }
        if (chapter.id in missing) return null
        return withIOContext {
            val bitmap = runCatching {
                val source = Injekt.get<SourceManager>().getOrStub(manga.source)
                val file = Injekt.get<DownloadProvider>()
                    .findChapterDir(chapter.name, chapter.scanlator, chapter.url, manga.ogTitle, source)
                    ?: return@runCatching null
                if (file.isDirectory) {
                    val first = file.listFiles().orEmpty()
                        .filter { it.isFile && it.name?.substringAfterLast('.')?.lowercase() in imageExt }
                        .minByOrNull { it.name.orEmpty() }
                        ?: return@runCatching null
                    decode { first.openInputStream() }
                } else {
                    decodeFromZip(file)
                }
            }.getOrNull()
            if (bitmap != null) cache[chapter.id] = bitmap else missing += chapter.id
            bitmap
        }
    }

    private fun decodeFromZip(file: UniFile): Bitmap? {
        val name = ZipInputStream(file.openInputStream()).use { zip ->
            generateSequence { zip.nextEntry }
                .filter { !it.isDirectory && it.name.substringAfterLast('.').lowercase() in imageExt }
                .map { it.name }
                .minOrNull()
        } ?: return null
        return decode {
            val zip = ZipInputStream(file.openInputStream())
            while (true) {
                val entry = zip.nextEntry ?: break
                if (entry.name == name) return@decode zip
            }
            zip
        }
    }

    private fun decode(open: () -> InputStream): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        open().use { BitmapFactory.decodeStream(it, null, bounds) }
        var sample = 1
        while (bounds.outWidth / (sample * 2) >= 120) sample *= 2
        val options = BitmapFactory.Options().apply {
            inSampleSize = sample
            inPreferredConfig = Bitmap.Config.RGB_565
        }
        val bitmap = open().use { BitmapFactory.decodeStream(it, null, options) } ?: return null
        return if (bitmap.height > bitmap.width * 2) {
            Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.width * 3 / 2)
        } else {
            bitmap
        }
    }
}

@Composable
fun Mod69ChapterThumb(manga: Manga, chapter: Chapter) {
    val image: ImageBitmap? by produceState(Mod69ChapterThumbs.cached(chapter.id)?.asImageBitmap(), chapter.id) {
        if (value == null) value = Mod69ChapterThumbs.load(manga, chapter)?.asImageBitmap()
    }
    image?.let {
        Image(
            bitmap = it,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .padding(start = 12.dp)
                .size(width = 40.dp, height = 56.dp)
                .clip(RoundedCornerShape(4.dp)),
        )
    }
}
// KMK <--
