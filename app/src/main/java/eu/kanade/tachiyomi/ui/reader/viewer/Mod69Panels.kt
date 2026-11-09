package eu.kanade.tachiyomi.ui.reader.viewer

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.RectF
import java.io.InputStream
import kotlin.math.abs

// KMK -->
/**
 * 69 mod R22: finds the comic panels of a page by looking for the empty gutters between them
 * (rows, then columns, of the page's background colour). Rects are fractions (0..1) of the page.
 */
object Mod69Panels {

    fun detect(open: () -> InputStream, rightToLeft: Boolean): List<RectF> {
        val bitmap = decodeSmall(open) ?: return emptyList()
        return try {
            detect(bitmap, rightToLeft)
        } finally {
            bitmap.recycle()
        }
    }

    private fun decodeSmall(open: () -> InputStream): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        open().use { BitmapFactory.decodeStream(it, null, bounds) }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
        // long strips are not page-based comics
        if (bounds.outHeight > bounds.outWidth * 3) return null
        var sample = 1
        while (bounds.outWidth / (sample * 2) >= 300) sample *= 2
        val options = BitmapFactory.Options().apply {
            inSampleSize = sample
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        return open().use { BitmapFactory.decodeStream(it, null, options) }
    }

    private fun detect(bitmap: Bitmap, rightToLeft: Boolean): List<RectF> {
        val w = bitmap.width
        val h = bitmap.height
        val pixels = IntArray(w * h)
        bitmap.getPixels(pixels, 0, w, 0, 0, w, h)
        val lum = IntArray(w * h) { i ->
            val c = pixels[i]
            ((c shr 16 and 0xFF) * 299 + (c shr 8 and 0xFF) * 587 + (c and 0xFF) * 114) / 1000
        }
        // background = most common brightness along the edges (white or black pages)
        val border = buildList {
            for (x in 0 until w) {
                add(lum[x])
                add(lum[(h - 1) * w + x])
            }
            for (y in 0 until h) {
                add(lum[y * w])
                add(lum[y * w + w - 1])
            }
        }.sorted()
        val background = border[border.size / 2]
        fun isInk(x: Int, y: Int) = abs(lum[y * w + x] - background) > 40

        val strips = runs(h, minLength = (h * 0.05f).toInt().coerceAtLeast(4)) { y ->
            (0 until w).count { isInk(it, y) } > w * 0.02f
        }
        val panels = mutableListOf<RectF>()
        strips.forEach { (top, bottom) ->
            val columns = runs(w, minLength = (w * 0.08f).toInt().coerceAtLeast(4)) { x ->
                (top..bottom).count { isInk(x, it) } > (bottom - top + 1) * 0.02f
            }.let { if (rightToLeft) it.reversed() else it }
            columns.forEach { (left, right) ->
                panels += RectF(
                    (left - w * 0.01f).coerceAtLeast(0f) / w,
                    (top - h * 0.01f).coerceAtLeast(0f) / h,
                    (right + 1 + w * 0.01f).coerceAtMost(w.toFloat()) / w,
                    (bottom + 1 + h * 0.01f).coerceAtMost(h.toFloat()) / h,
                )
            }
        }
        // one panel covering (almost) the whole page = nothing to step through
        if (panels.size <= 1) return emptyList()
        return panels
    }

    /** Ranges (start, end) along one side where [hasInk] is true, joined across tiny gaps. */
    private fun runs(size: Int, minLength: Int, hasInk: (Int) -> Boolean): List<Pair<Int, Int>> {
        val out = mutableListOf<Pair<Int, Int>>()
        var start = -1
        var gap = 0
        for (i in 0 until size) {
            if (hasInk(i)) {
                if (start < 0) start = i
                gap = 0
            } else if (start >= 0) {
                gap++
                if (gap >= 3) {
                    val end = i - gap
                    if (end - start + 1 >= minLength) out += start to end
                    start = -1
                    gap = 0
                }
            }
        }
        if (start >= 0 && size - 1 - start + 1 >= minLength) out += start to size - 1
        return out
    }
}
// KMK <--
