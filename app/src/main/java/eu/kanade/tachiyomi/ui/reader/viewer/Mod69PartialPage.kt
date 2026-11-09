package eu.kanade.tachiyomi.ui.reader.viewer

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.RenderEffect
import android.graphics.Shader
import android.os.Build
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import eu.kanade.tachiyomi.ui.reader.model.ReaderPage
import kotlinx.coroutines.flow.MutableStateFlow
import okhttp3.Response
import okio.buffer
import okio.sink
import java.io.File
import java.io.OutputStream

// KMK -->
/**
 * 69 mod N2: a page shows a blurry first look while it downloads and sharpens as more of it arrives.
 * The download is written to the normal cache and, at the same time, to a temporary file that is
 * decoded every so often (JPEG pictures decode even when half downloaded).
 */
object Mod69PartialPage {
    /** Partial file per page; the number goes up each time more of the picture arrived. */
    private val ticks = java.util.concurrent.ConcurrentHashMap<ReaderPage, MutableStateFlow<Long>>()
    private val files = java.util.concurrent.ConcurrentHashMap<ReaderPage, File>()

    fun flowFor(page: ReaderPage): MutableStateFlow<Long> = ticks.getOrPut(page) { MutableStateFlow(0L) }

    fun fileFor(page: ReaderPage): File? = files[page]

    /** Copies [response] into [out] (the cache) and a temporary file, telling the page as it goes. */
    fun copy(page: ReaderPage, response: Response, out: OutputStream, tempDir: File) {
        val temp = File(tempDir, "mod69_partial_${System.identityHashCode(page)}").apply { delete() }
        files[page] = temp
        val tick = flowFor(page)
        try {
            val buffer = ByteArray(32 * 1024)
            val input = response.body.byteStream()
            var sinceUpdate = 0L
            temp.outputStream().use { partial ->
                out.use { cache ->
                    while (true) {
                        val read = input.read(buffer)
                        if (read < 0) break
                        cache.write(buffer, 0, read)
                        partial.write(buffer, 0, read)
                        sinceUpdate += read
                        if (sinceUpdate >= 96 * 1024) {
                            partial.flush()
                            sinceUpdate = 0
                            tick.value = tick.value + 1
                        }
                    }
                }
            }
        } finally {
            response.body.close()
            files.remove(page)
            ticks.remove(page)
            temp.delete()
        }
    }

    /** Decodes what has arrived so far, small, or null when it cannot be read yet. */
    fun decode(page: ReaderPage, targetWidth: Int): Bitmap? {
        val file = files[page] ?: return null
        return runCatching {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(file.path, bounds)
            if (bounds.outWidth <= 0) return null
            var sample = 1
            while (bounds.outWidth / (sample * 2) >= targetWidth / 2) sample *= 2
            BitmapFactory.decodeFile(
                file.path,
                BitmapFactory.Options().apply {
                    inSampleSize = sample
                    inPreferredConfig = Bitmap.Config.RGB_565
                },
            )
        }.getOrNull()
    }

    /** The preview view placed under the progress circle. */
    fun previewView(parent: ViewGroup): ImageView = ImageView(parent.context).apply {
        layoutParams = FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        scaleType = ImageView.ScaleType.FIT_CENTER
        adjustViewBounds = true
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            setRenderEffect(RenderEffect.createBlurEffect(14f, 14f, Shader.TileMode.CLAMP))
        } else {
            alpha = 0.6f
        }
    }
}
// KMK <--
