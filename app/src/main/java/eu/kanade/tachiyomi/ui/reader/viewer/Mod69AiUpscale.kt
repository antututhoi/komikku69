package eu.kanade.tachiyomi.ui.reader.viewer

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import eu.kanade.tachiyomi.data.notification.Mod69Progress
import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.network.NetworkHelper
import eu.kanade.tachiyomi.network.awaitSuccess
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.ui.reader.model.ReaderPage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.tensorflow.lite.Interpreter
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder

// KMK -->
/**
 * 69 mod R3: makes a blurry, low-resolution page sharper with a small AI model (ESRGAN, 4x).
 * The model (about 5 MB) is downloaded the first time. Works in pieces of 50 x 50 pixels,
 * so a page takes from several seconds to a minute; progress shows in a notification.
 */
object Mod69AiUpscale {
    private const val MODEL_URL =
        "https://storage.googleapis.com/download.tensorflow.org/models/tflite/esrgan/ESRGAN.tflite"
    private const val TILE = 50
    private const val SCALE = 4
    private const val OVERLAP = 4

    /** Pages wider than this are shrunk first, so the result stays a sane size. */
    private const val MAX_INPUT_WIDTH = 900

    private suspend fun model(context: Context): File {
        val file = File(context.filesDir, "mod69_esrgan.tflite")
        if (file.exists() && file.length() > 1_000_000) return file
        val response = Injekt.get<NetworkHelper>().client.newCall(GET(MODEL_URL)).awaitSuccess()
        val temp = File(file.path + ".part")
        response.use { r -> temp.outputStream().use { r.body.byteStream().copyTo(it) } }
        temp.renameTo(file)
        return file
    }

    /** Replaces the page picture with a sharper one. Returns a short result text. */
    suspend fun run(context: Context, page: ReaderPage): String = withContext(Dispatchers.Default) {
        val id = Mod69Progress.ID_AI_UPSCALE
        val title = "Making page ${page.index + 1} sharper"
        Mod69Progress.start(context, id, title)
        try {
            val stream = page.stream ?: error("The page is not loaded yet")
            val modelFile = withContext(Dispatchers.IO) {
                Mod69Progress.update(context, id, title, "Getting the AI model (first time only)…", 0, 0, force = true)
                model(context)
            }
            val source = stream().use { BitmapFactory.decodeStream(it) } ?: error("Could not read the page")
            val input = if (source.width > MAX_INPUT_WIDTH) {
                Bitmap.createScaledBitmap(
                    source,
                    MAX_INPUT_WIDTH,
                    source.height * MAX_INPUT_WIDTH / source.width,
                    true,
                ).also { if (it != source) source.recycle() }
            } else {
                source
            }
            val output = upscale(context, modelFile, input, id, title)
            input.recycle()
            val outFile = File(context.cacheDir, "mod69_ai_${System.identityHashCode(page)}.jpg")
            outFile.outputStream().use { output.compress(Bitmap.CompressFormat.JPEG, 92, it) }
            val size = "${output.width} x ${output.height}"
            output.recycle()
            withContext(Dispatchers.Main) {
                page.stream = { outFile.inputStream() }
                // make the reader show the new picture
                page.status = Page.State.LoadPage
                page.status = Page.State.Ready
            }
            Mod69Progress.finish(context, id, "Page ${page.index + 1} is sharper", listOf("New size: $size"))
            "OK"
        } catch (e: Throwable) {
            Mod69Progress.finish(context, id, "AI sharpening failed", listOf(e.message ?: e.toString()))
            e.message ?: "Failed"
        }
    }

    private fun upscale(context: Context, modelFile: File, src: Bitmap, id: Int, title: String): Bitmap {
        val options = Interpreter.Options().apply { setNumThreads(4) }
        Interpreter(modelFile, options).use { interpreter ->
            val w = src.width
            val h = src.height
            val out = Bitmap.createBitmap(w * SCALE, h * SCALE, Bitmap.Config.ARGB_8888)
            val step = TILE - OVERLAP * 2
            val xs = (0 until w step step).toList()
            val ys = (0 until h step step).toList()
            val total = xs.size * ys.size
            var done = 0
            val inBuf = ByteBuffer.allocateDirect(TILE * TILE * 3 * 4).order(ByteOrder.nativeOrder())
            val outBuf = ByteBuffer.allocateDirect(TILE * SCALE * TILE * SCALE * 3 * 4).order(ByteOrder.nativeOrder())
            val tilePixels = IntArray(TILE * TILE)
            val outPixels = IntArray(TILE * SCALE * TILE * SCALE)
            for (y0 in ys) {
                for (x0 in xs) {
                    if (Mod69Progress.isStopped(id)) error("Stopped")
                    // tile with a margin around the useful part (clamped inside the picture)
                    val tx = (x0 - OVERLAP).coerceIn(0, (w - TILE).coerceAtLeast(0))
                    val ty = (y0 - OVERLAP).coerceIn(0, (h - TILE).coerceAtLeast(0))
                    val tw = minOf(TILE, w - tx)
                    val th = minOf(TILE, h - ty)
                    java.util.Arrays.fill(tilePixels, 0)
                    src.getPixels(tilePixels, 0, TILE, tx, ty, tw, th)
                    inBuf.rewind()
                    for (p in tilePixels) {
                        inBuf.putFloat((p shr 16 and 0xFF).toFloat())
                        inBuf.putFloat((p shr 8 and 0xFF).toFloat())
                        inBuf.putFloat((p and 0xFF).toFloat())
                    }
                    inBuf.rewind()
                    outBuf.rewind()
                    interpreter.run(inBuf, outBuf)
                    outBuf.rewind()
                    for (i in outPixels.indices) {
                        val r = outBuf.float.toInt().coerceIn(0, 255)
                        val g = outBuf.float.toInt().coerceIn(0, 255)
                        val b = outBuf.float.toInt().coerceIn(0, 255)
                        outPixels[i] = (0xFF shl 24) or (r shl 16) or (g shl 8) or b
                    }
                    // copy only the useful part of this tile
                    val useX = x0 - tx
                    val useY = y0 - ty
                    val useW = minOf(step, w - x0, tw - useX)
                    val useH = minOf(step, h - y0, th - useY)
                    if (useW > 0 && useH > 0) {
                        out.setPixels(
                            outPixels,
                            useY * SCALE * TILE * SCALE + useX * SCALE,
                            TILE * SCALE,
                            x0 * SCALE,
                            y0 * SCALE,
                            useW * SCALE,
                            useH * SCALE,
                        )
                    }
                    done++
                    Mod69Progress.update(context, id, title, "Piece $done of $total", done, total)
                }
            }
            return out
        }
    }
}
// KMK <--
