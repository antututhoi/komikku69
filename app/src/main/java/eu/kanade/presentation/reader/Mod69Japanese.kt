package eu.kanade.presentation.reader

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.google.android.gms.tasks.Task
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.japanese.JapaneseTextRecognizerOptions
import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.network.NetworkHelper
import eu.kanade.tachiyomi.network.awaitSuccess
import eu.kanade.tachiyomi.ui.reader.model.ReaderPage
import eu.kanade.tachiyomi.util.system.toast
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.HttpUrl.Companion.toHttpUrl
import tachiyomi.core.common.util.lang.withIOContext
import tachiyomi.i18n.MR
import tachiyomi.i18n.kmk.KMR
import tachiyomi.presentation.core.i18n.stringResource
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

// KMK -->
/**
 * 69 mod O3: Japanese learning. Reads the Japanese text of a page (on the phone), looks words up
 * in the free Jisho dictionary and sends a card to AnkiDroid.
 */
data class Mod69Word(val word: String, val reading: String, val meanings: String)

private suspend fun <T> Task<T>.awaitTask(): T = suspendCancellableCoroutine { cont ->
    addOnSuccessListener { cont.resume(it) }
    addOnFailureListener { cont.resumeWithException(it) }
}

private suspend fun readJapanese(page: ReaderPage): List<String> = withIOContext {
    val bitmap = page.stream?.invoke()?.use { BitmapFactory.decodeStream(it) } ?: return@withIOContext emptyList()
    val recognizer = TextRecognition.getClient(JapaneseTextRecognizerOptions.Builder().build())
    try {
        recognizer.process(InputImage.fromBitmap(bitmap, 0)).awaitTask().textBlocks
            .map { block -> block.lines.joinToString("") { it.text } }
            .filter { it.isNotBlank() }
    } finally {
        recognizer.close()
        bitmap.recycle()
    }
}

private suspend fun jisho(query: String): List<Mod69Word> = withIOContext {
    val url = "https://jisho.org/api/v1/search/words".toHttpUrl().newBuilder()
        .addQueryParameter("keyword", query)
        .build()
    val text = Injekt.get<NetworkHelper>().client.newCall(GET(url.toString())).awaitSuccess().use { it.body.string() }
    Json.parseToJsonElement(text).jsonObject["data"]!!.jsonArray.take(15).map { e ->
        val o = e.jsonObject
        val jp = o["japanese"]?.jsonArray?.firstOrNull()?.jsonObject
        val word = jp?.get("word")?.jsonPrimitive?.content ?: jp?.get("reading")?.jsonPrimitive?.content.orEmpty()
        val reading = jp?.get("reading")?.jsonPrimitive?.content.orEmpty()
        val meanings = o["senses"]?.jsonArray?.take(3)?.joinToString("; ") { s ->
            s.jsonObject["english_definitions"]?.jsonArray?.joinToString(", ") { it.jsonPrimitive.content }.orEmpty()
        }.orEmpty()
        Mod69Word(word, reading, meanings)
    }
}

private fun sendToAnki(context: Context, word: Mod69Word, sentence: String) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        setPackage("com.ichi2.anki")
        putExtra(Intent.EXTRA_SUBJECT, word.word)
        putExtra(Intent.EXTRA_TEXT, "${word.reading}\n${word.meanings}\n\n$sentence")
    }
    try {
        context.startActivity(intent)
    } catch (_: ActivityNotFoundException) {
        context.toast(KMR.strings.mod69_jp_no_anki)
    }
}

@Composable
fun Mod69JapaneseDialog(page: ReaderPage, onDismissRequest: () -> Unit) {
    val context = LocalContext.current
    var lines by remember { mutableStateOf<List<String>?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var query by remember { mutableStateOf("") }
    var sentence by remember { mutableStateOf("") }
    var words by remember { mutableStateOf<List<Mod69Word>?>(null) }
    var searching by remember { mutableStateOf(false) }
    LaunchedEffect(page) {
        runCatching { lines = readJapanese(page) }.onFailure {
            error = it.message
            lines = emptyList()
        }
    }
    LaunchedEffect(query) {
        if (query.isBlank()) return@LaunchedEffect
        searching = true
        words = runCatching { jisho(query) }.getOrElse {
            error = it.message
            emptyList()
        }
        searching = false
    }
    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text(stringResource(KMR.strings.mod69_jp_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                val found = lines
                when {
                    found == null -> CircularProgressIndicator()
                    found.isEmpty() -> Text(error ?: stringResource(KMR.strings.mod69_jp_none))
                    words == null && !searching -> {
                        Text(stringResource(KMR.strings.mod69_jp_pick_line), style = MaterialTheme.typography.labelMedium)
                        LazyColumn(modifier = Modifier.heightIn(max = 380.dp)) {
                            items(found) { line ->
                                Text(
                                    line,
                                    style = MaterialTheme.typography.bodyLarge,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            sentence = line
                                            query = line
                                        }
                                        .padding(vertical = 8.dp),
                                )
                                HorizontalDivider()
                            }
                        }
                    }
                    else -> {
                        var edit by remember(sentence) { mutableStateOf(query) }
                        OutlinedTextField(
                            value = edit,
                            onValueChange = { edit = it },
                            label = { Text(stringResource(KMR.strings.mod69_jp_search)) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Row {
                            TextButton(onClick = { query = edit.trim() }) { Text(stringResource(KMR.strings.mod69_jp_look_up)) }
                            TextButton(onClick = {
                                words = null
                                query = ""
                            }) { Text(stringResource(KMR.strings.mod69_jp_back)) }
                        }
                        if (searching) CircularProgressIndicator()
                        LazyColumn(modifier = Modifier.heightIn(max = 320.dp)) {
                            items(words.orEmpty()) { w ->
                                Column(Modifier.padding(vertical = 6.dp)) {
                                    Text("${w.word}  【${w.reading}】", style = MaterialTheme.typography.titleSmall)
                                    Text(w.meanings, style = MaterialTheme.typography.bodySmall)
                                    TextButton(onClick = { sendToAnki(context, w, sentence) }) {
                                        Text(stringResource(KMR.strings.mod69_jp_anki))
                                    }
                                }
                                HorizontalDivider()
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismissRequest) { Text(stringResource(MR.strings.action_close)) } },
    )
}
// KMK <--
