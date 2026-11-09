package eu.kanade.tachiyomi.ui.reader.viewer

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.util.Base64
import com.google.android.gms.tasks.Task
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.TranslatorOptions
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.chinese.ChineseTextRecognizerOptions
import com.google.mlkit.vision.text.japanese.JapaneseTextRecognizerOptions
import com.google.mlkit.vision.text.korean.KoreanTextRecognizerOptions
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import eu.kanade.tachiyomi.data.notification.Mod69Progress
import eu.kanade.tachiyomi.network.NetworkHelper
import eu.kanade.tachiyomi.network.awaitSuccess
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.ui.reader.model.ReaderPage
import eu.kanade.tachiyomi.ui.reader.setting.ReaderPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.add
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.io.ByteArrayOutputStream
import java.io.File
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

// KMK -->
/**
 * 69 mod R4: translate a manga page. Finds the text of the speech bubbles, translates it and paints
 * the translation over the original text.
 * - "google": Google's free on-device tools (text reading + translation models downloaded once, about 30 MB a language).
 * - "ai": your own AI key (Gemini, or OpenAI / any OpenAI-compatible service) reads and translates in one go.
 */
object Mod69Translate {

    data class Bubble(val box: RectF, val text: String)

    private val prefs get() = Injekt.get<ReaderPreferences>()

    suspend fun run(context: Context, page: ReaderPage, sourceLang: String): String = withContext(Dispatchers.Default) {
        val id = Mod69Progress.ID_TRANSLATE
        val title = "Translating page ${page.index + 1}"
        Mod69Progress.start(context, id, title)
        try {
            val stream = page.stream ?: error("The page is not loaded yet")
            val original = stream().use { BitmapFactory.decodeStream(it) } ?: error("Could not read the page")
            val target = prefs.mod69TranslateTo().get()
            val bubbles = when (prefs.mod69TranslateEngine().get()) {
                "ai" -> aiBubbles(context, original, target, id, title)
                else -> googleBubbles(context, original, sourceLang, target, id, title)
            }
            if (bubbles.isEmpty()) error("No text found on this page")
            val painted = paint(original, bubbles)
            val outFile = File(context.cacheDir, "mod69_tr_${System.identityHashCode(page)}.jpg")
            outFile.outputStream().use { painted.compress(Bitmap.CompressFormat.JPEG, 92, it) }
            painted.recycle()
            withContext(Dispatchers.Main) {
                page.stream = { outFile.inputStream() }
                page.status = Page.State.LoadPage
                page.status = Page.State.Ready
            }
            Mod69Progress.finish(context, id, "Page ${page.index + 1} translated", listOf("${bubbles.size} text boxes"))
            "OK"
        } catch (e: Throwable) {
            Mod69Progress.finish(context, id, "Translation failed", listOf(e.message ?: e.toString()))
            e.message ?: "Failed"
        }
    }

    // ---------- Google (on-device) ----------

    private suspend fun googleBubbles(
        context: Context,
        bitmap: Bitmap,
        sourceLang: String,
        target: String,
        id: Int,
        title: String,
    ): List<Bubble> {
        val lang = sourceLang.substringBefore('-').lowercase()
        val recognizer = when (lang) {
            "ja" -> TextRecognition.getClient(JapaneseTextRecognizerOptions.Builder().build())
            "zh" -> TextRecognition.getClient(ChineseTextRecognizerOptions.Builder().build())
            "ko" -> TextRecognition.getClient(KoreanTextRecognizerOptions.Builder().build())
            else -> TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        }
        Mod69Progress.update(context, id, title, "Reading the text…", 0, 0, force = true)
        val result = recognizer.process(InputImage.fromBitmap(bitmap, 0)).await()
        recognizer.close()
        val blocks = result.textBlocks.filter { it.boundingBox != null && it.text.isNotBlank() }
        if (blocks.isEmpty()) return emptyList()
        val from = TranslateLanguage.fromLanguageTag(lang) ?: TranslateLanguage.JAPANESE
        val to = TranslateLanguage.fromLanguageTag(target) ?: TranslateLanguage.ENGLISH
        val translator = Translation.getClient(
            TranslatorOptions.Builder().setSourceLanguage(from).setTargetLanguage(to).build(),
        )
        try {
            Mod69Progress.update(context, id, title, "Getting the translation model (first time only)…", 0, 0, force = true)
            translator.downloadModelIfNeeded().await()
            return blocks.mapIndexed { i, block ->
                Mod69Progress.update(context, id, title, "Translating box ${i + 1}", i + 1, blocks.size)
                // CJK text is read without spaces; join lines without spaces for those languages
                val text = block.lines.joinToString(if (lang in setOf("ja", "zh")) "" else " ") { it.text }
                Bubble(RectF(block.boundingBox!!), translator.translate(text).await())
            }
        } finally {
            translator.close()
        }
    }

    private suspend fun <T> Task<T>.await(): T = suspendCancellableCoroutine { cont ->
        addOnSuccessListener { cont.resume(it) }
        addOnFailureListener { cont.resumeWithException(it) }
        addOnCanceledListener { cont.cancel() }
    }

    // ---------- your own AI key ----------

    private suspend fun aiBubbles(context: Context, bitmap: Bitmap, target: String, id: Int, title: String): List<Bubble> {
        val key = prefs.mod69AiKey().get().trim()
        if (key.isEmpty()) error("Add your AI key in Settings → Reader → 69- Reader extras")
        val provider = prefs.mod69AiProvider().get()
        Mod69Progress.update(context, id, title, "Asking the AI…", 0, 0, force = true)
        // send a smaller copy to save time and money
        val scale = (1600f / maxOf(bitmap.width, bitmap.height)).coerceAtMost(1f)
        val small = if (scale < 1f) {
            Bitmap.createScaledBitmap(bitmap, (bitmap.width * scale).toInt(), (bitmap.height * scale).toInt(), true)
        } else {
            bitmap
        }
        val jpeg = ByteArrayOutputStream().use {
            small.compress(Bitmap.CompressFormat.JPEG, 85, it)
            it.toByteArray()
        }
        if (small != bitmap) small.recycle()
        val b64 = Base64.encodeToString(jpeg, Base64.NO_WRAP)
        val prompt = "This is a comic page. Find every speech bubble and caption with text. " +
            "Translate each one to the language with code '$target' (natural, short, like a comic). " +
            "Answer ONLY with JSON: {\"bubbles\":[{\"box\":[ymin,xmin,ymax,xmax],\"text\":\"translation\"}]} " +
            "where box values are 0-1000 relative to the image size. Keep the reading order."
        val client = Injekt.get<NetworkHelper>().client.newBuilder()
            .readTimeout(120, java.util.concurrent.TimeUnit.SECONDS)
            .callTimeout(150, java.util.concurrent.TimeUnit.SECONDS)
            .build()
        val answer: String = if (provider == "gemini") {
            val model = prefs.mod69AiModel().get().ifBlank { "gemini-2.5-flash" }
            val body = buildJsonObject {
                putJsonArray("contents") {
                    addJsonObject {
                        putJsonArray("parts") {
                            addJsonObject { put("text", prompt) }
                            addJsonObject {
                                putJsonObject("inline_data") {
                                    put("mime_type", "image/jpeg")
                                    put("data", b64)
                                }
                            }
                        }
                    }
                }
                putJsonObject("generationConfig") { put("response_mime_type", "application/json") }
            }
            val url = "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$key"
            val text = client.newCall(
                Request.Builder().url(url).post(body.toString().toRequestBody("application/json".toMediaType())).build(),
            ).awaitSuccess().use { it.body.string() }
            Json.parseToJsonElement(text).jsonObject["candidates"]!!.jsonArray[0].jsonObject["content"]!!
                .jsonObject["parts"]!!.jsonArray[0].jsonObject["text"]!!.jsonPrimitive.content
        } else {
            val base = prefs.mod69AiBaseUrl().get().ifBlank { "https://api.openai.com/v1" }.trimEnd('/')
            val model = prefs.mod69AiModel().get().ifBlank { "gpt-4o-mini" }
            val body = buildJsonObject {
                put("model", model)
                putJsonObject("response_format") { put("type", "json_object") }
                putJsonArray("messages") {
                    addJsonObject {
                        put("role", "user")
                        putJsonArray("content") {
                            addJsonObject {
                                put("type", "text")
                                put("text", prompt)
                            }
                            addJsonObject {
                                put("type", "image_url")
                                putJsonObject("image_url") { put("url", "data:image/jpeg;base64,$b64") }
                            }
                        }
                    }
                }
            }
            val text = client.newCall(
                Request.Builder()
                    .url("$base/chat/completions")
                    .header("Authorization", "Bearer $key")
                    .post(body.toString().toRequestBody("application/json".toMediaType()))
                    .build(),
            ).awaitSuccess().use { it.body.string() }
            Json.parseToJsonElement(text).jsonObject["choices"]!!.jsonArray[0].jsonObject["message"]!!
                .jsonObject["content"]!!.jsonPrimitive.content
        }
        val json = answer.substringAfter("```json").substringAfter("```").substringBeforeLast("```").trim()
            .ifEmpty { answer }
        val root = runCatching { Json.parseToJsonElement(json) }.getOrElse { Json.parseToJsonElement(answer) }
        val list = (root as? JsonObject)?.get("bubbles")?.jsonArray ?: root as? JsonArray ?: return emptyList()
        return list.mapNotNull { element ->
            val o = element as? JsonObject ?: return@mapNotNull null
            val box = o["box"]?.jsonArray?.mapNotNull { it.jsonPrimitive.doubleOrNull } ?: return@mapNotNull null
            if (box.size != 4) return@mapNotNull null
            val text = o["text"]?.jsonPrimitive?.content ?: return@mapNotNull null
            val (ymin, xmin, ymax, xmax) = box
            Bubble(
                RectF(
                    (xmin / 1000 * bitmap.width).toFloat(),
                    (ymin / 1000 * bitmap.height).toFloat(),
                    (xmax / 1000 * bitmap.width).toFloat(),
                    (ymax / 1000 * bitmap.height).toFloat(),
                ),
                text,
            )
        }
    }

    // ---------- painting ----------

    private fun paint(original: Bitmap, bubbles: List<Bubble>): Bitmap {
        val out = original.copy(Bitmap.Config.ARGB_8888, true)
        original.recycle()
        val canvas = Canvas(out)
        val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }
        val textPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.BLACK }
        bubbles.forEach { bubble ->
            val pad = bubble.box.height().coerceAtMost(bubble.box.width()) * 0.08f
            val box = RectF(bubble.box).apply { inset(-pad, -pad) }
            canvas.drawRoundRect(box, pad * 2, pad * 2, fill)
            val width = box.width().toInt().coerceAtLeast(10)
            var size = (box.height() / 2).coerceIn(10f, 90f)
            var layout: StaticLayout
            while (true) {
                textPaint.textSize = size
                layout = StaticLayout.Builder.obtain(bubble.text, 0, bubble.text.length, textPaint, width)
                    .setAlignment(Layout.Alignment.ALIGN_CENTER)
                    .setLineSpacing(0f, 0.95f)
                    .build()
                if (layout.height <= box.height() || size <= 9f) break
                size *= 0.9f
            }
            canvas.save()
            canvas.translate(box.left, box.top + (box.height() - layout.height) / 2f)
            layout.draw(canvas)
            canvas.restore()
        }
        return out
    }
}
// KMK <--
