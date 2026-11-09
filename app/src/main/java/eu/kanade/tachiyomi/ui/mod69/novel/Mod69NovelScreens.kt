package eu.kanade.tachiyomi.ui.mod69.novel

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.speech.tts.TextToSpeech
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.List
import androidx.compose.material.icons.automirrored.outlined.NavigateBefore
import androidx.compose.material.icons.automirrored.outlined.NavigateNext
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.FormatSize
import androidx.compose.material.icons.outlined.RecordVoiceOver
import androidx.compose.material.icons.outlined.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.edit
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.hippo.unifile.UniFile
import eu.kanade.presentation.components.AppBar
import eu.kanade.presentation.util.Screen
import kotlinx.coroutines.launch
import tachiyomi.core.common.util.lang.withIOContext
import tachiyomi.i18n.kmk.KMR
import tachiyomi.presentation.core.components.material.Scaffold
import tachiyomi.presentation.core.i18n.stringResource

// KMK -->
/** 69 mod O1: light novels / EPUB / TXT from a folder you pick. */
private fun prefs(context: Context) = context.getSharedPreferences("mod69_novels", Context.MODE_PRIVATE)

class Mod69NovelLibraryScreen : Screen() {

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val context = LocalContext.current
        var folder by remember { mutableStateOf(prefs(context).getString("folder", null)) }
        var books by remember { mutableStateOf<List<Pair<String, String>>?>(null) }
        val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
            if (uri == null) return@rememberLauncherForActivityResult
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION,
            )
            prefs(context).edit { putString("folder", uri.toString()) }
            folder = uri.toString()
        }
        LaunchedEffect(folder) {
            val f = folder ?: run {
                books = emptyList()
                return@LaunchedEffect
            }
            books = withIOContext {
                runCatching {
                    val dir = UniFile.fromUri(context, Uri.parse(f))
                    fun walk(d: UniFile?, depth: Int): List<UniFile> = d?.listFiles().orEmpty().flatMap {
                        if (it.isDirectory && depth < 3) walk(it, depth + 1) else listOf(it)
                    }
                    walk(dir, 0)
                        .filter { it.name?.lowercase()?.let { n -> n.endsWith(".epub") || n.endsWith(".txt") } == true }
                        .sortedBy { it.name?.lowercase() }
                        .map { it.name.orEmpty() to it.uri.toString() }
                }.getOrDefault(emptyList())
            }
        }
        Scaffold(
            topBar = { scrollBehavior ->
                AppBar(
                    title = stringResource(KMR.strings.mod69_novels),
                    navigateUp = navigator::pop,
                    actions = {
                        IconButton(onClick = { picker.launch(null) }) {
                            Icon(Icons.Outlined.FolderOpen, contentDescription = stringResource(KMR.strings.mod69_novels_pick))
                        }
                    },
                    scrollBehavior = scrollBehavior,
                )
            },
        ) { padding ->
            val list = books
            when {
                folder == null -> Column(Modifier.padding(padding).padding(16.dp)) {
                    Text(stringResource(KMR.strings.mod69_novels_intro))
                    TextButton(onClick = { picker.launch(null) }) { Text(stringResource(KMR.strings.mod69_novels_pick)) }
                }
                list == null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                list.isEmpty() -> Text(stringResource(KMR.strings.mod69_novels_empty), modifier = Modifier.padding(padding).padding(16.dp))
                else -> LazyColumn(contentPadding = padding) {
                    itemsIndexed(list, key = { _, it -> it.second }) { _, (name, uri) ->
                        val progress = prefs(context).getInt("pct_$uri", 0)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { navigator.push(Mod69NovelReaderScreen(uri, name)) }
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                        ) {
                            Text(name.substringBeforeLast('.'), modifier = Modifier.weight(1f))
                            if (progress > 0) Text("$progress%", color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
        }
    }
}

class Mod69NovelReaderScreen(private val uri: String, private val name: String) : Screen() {

    override val key = "mod69-novel-$uri"

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val context = LocalContext.current
        val scope = rememberCoroutineScope()
        val p = remember { prefs(context) }
        var book by remember { mutableStateOf<Mod69Book?>(null) }
        var error by remember { mutableStateOf<String?>(null) }
        var chapter by remember { mutableIntStateOf(p.getInt("ch_$uri", 0)) }
        var fontSize by remember { mutableStateOf(p.getFloat("font_size", 18f)) }
        var lineHeight by remember { mutableStateOf(p.getFloat("line_height", 1.5f)) }
        var font by remember { mutableStateOf(p.getString("font", "serif")!!) }
        var theme by remember { mutableStateOf(p.getString("theme", "sepia")!!) }
        var showSettings by remember { mutableStateOf(false) }
        var showChapters by remember { mutableStateOf(false) }
        var speaking by remember { mutableStateOf(false) }
        val listState = rememberLazyListState()

        LaunchedEffect(Unit) {
            runCatching { book = withIOContext { Mod69Book.load(context, Uri.parse(uri), name) } }
                .onFailure { error = it.message ?: it.toString() }
        }
        LaunchedEffect(book, chapter) {
            val b = book ?: return@LaunchedEffect
            chapter = chapter.coerceIn(0, b.chapters.lastIndex)
            val saved = if (p.getInt("ch_$uri", -1) == chapter) p.getInt("par_$uri", 0) else 0
            listState.scrollToItem(saved.coerceAtLeast(0))
        }
        // save the place
        LaunchedEffect(listState.firstVisibleItemIndex, chapter, book) {
            val b = book ?: return@LaunchedEffect
            p.edit {
                putInt("ch_$uri", chapter)
                putInt("par_$uri", listState.firstVisibleItemIndex)
                putInt("pct_$uri", ((chapter + 1) * 100 / b.chapters.size.coerceAtLeast(1)))
            }
        }

        val tts = remember { mutableStateOf<TextToSpeech?>(null) }
        DisposableEffect(Unit) {
            onDispose {
                tts.value?.stop()
                tts.value?.shutdown()
            }
        }
        fun speakFrom(index: Int) {
            val b = book ?: return
            val paragraphs = b.chapters[chapter].paragraphs.drop(index)
            val existing = tts.value
            if (existing != null) {
                existing.stop()
                paragraphs.forEachIndexed { i, text -> existing.speak(text, TextToSpeech.QUEUE_ADD, null, "p${index + i}") }
            } else {
                tts.value = TextToSpeech(context) { status ->
                    if (status == TextToSpeech.SUCCESS) {
                        paragraphs.forEachIndexed { i, text ->
                            tts.value?.speak(text, TextToSpeech.QUEUE_ADD, null, "p${index + i}")
                        }
                    }
                }
            }
            speaking = true
        }

        val (bg, fg) = when (theme) {
            "light" -> Color(0xFFFFFFFF) to Color(0xFF1B1B1B)
            "dark" -> Color(0xFF1E1E1E) to Color(0xFFDADADA)
            "black" -> Color(0xFF000000) to Color(0xFFBDBDBD)
            else -> Color(0xFFF4ECD8) to Color(0xFF3B2F2F)
        }
        val family = when (font) {
            "sans" -> FontFamily.SansSerif
            "mono" -> FontFamily.Monospace
            else -> FontFamily.Serif
        }

        Scaffold(
            topBar = { scrollBehavior ->
                AppBar(
                    title = book?.chapters?.getOrNull(chapter)?.title ?: name,
                    subtitle = book?.title,
                    navigateUp = navigator::pop,
                    actions = {
                        IconButton(onClick = { showChapters = true }) {
                            Icon(Icons.AutoMirrored.Outlined.List, contentDescription = null)
                        }
                        IconButton(onClick = {
                            if (speaking) {
                                tts.value?.stop()
                                speaking = false
                            } else {
                                speakFrom(listState.firstVisibleItemIndex)
                            }
                        }) {
                            Icon(if (speaking) Icons.Outlined.Stop else Icons.Outlined.RecordVoiceOver, contentDescription = null)
                        }
                        IconButton(onClick = { showSettings = true }) {
                            Icon(Icons.Outlined.FormatSize, contentDescription = null)
                        }
                    },
                    scrollBehavior = scrollBehavior,
                )
            },
            bottomBar = {
                val b = book
                if (b != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface).padding(4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        IconButton(onClick = { if (chapter > 0) chapter-- }, enabled = chapter > 0) {
                            Icon(Icons.AutoMirrored.Outlined.NavigateBefore, contentDescription = null)
                        }
                        Text(
                            "${chapter + 1} / ${b.chapters.size}",
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.labelLarge,
                        )
                        IconButton(onClick = { if (chapter < b.chapters.lastIndex) chapter++ }, enabled = chapter < b.chapters.lastIndex) {
                            Icon(Icons.AutoMirrored.Outlined.NavigateNext, contentDescription = null)
                        }
                    }
                }
            },
        ) { padding ->
            val b = book
            Box(Modifier.fillMaxSize().background(bg)) {
                when {
                    error != null -> Text(error!!, color = fg, modifier = Modifier.padding(padding).padding(16.dp))
                    b == null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                    else -> LazyColumn(
                        state = listState,
                        contentPadding = PaddingValues(
                            start = 20.dp,
                            end = 20.dp,
                            top = padding.calculateTopPadding() + 12.dp,
                            bottom = padding.calculateBottomPadding() + 24.dp,
                        ),
                        verticalArrangement = Arrangement.spacedBy((fontSize * 0.6f).dp),
                    ) {
                        itemsIndexed(b.chapters[chapter.coerceIn(0, b.chapters.lastIndex)].paragraphs) { index, text ->
                            Text(
                                text = text,
                                color = fg,
                                fontSize = fontSize.sp,
                                lineHeight = (fontSize * lineHeight).sp,
                                fontFamily = family,
                                fontWeight = if (index == 0) FontWeight.SemiBold else FontWeight.Normal,
                                modifier = Modifier.clickable { if (speaking) speakFrom(index) },
                            )
                        }
                    }
                }
            }
        }

        if (showChapters) {
            val b = book
            AlertDialog(
                onDismissRequest = { showChapters = false },
                title = { Text(stringResource(KMR.strings.mod69_novels_chapters)) },
                text = {
                    LazyColumn {
                        itemsIndexed(b?.chapters.orEmpty()) { index, c ->
                            Text(
                                c.title,
                                color = if (index == chapter) MaterialTheme.colorScheme.primary else Color.Unspecified,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        chapter = index
                                        showChapters = false
                                        scope.launch { listState.scrollToItem(0) }
                                    }
                                    .padding(vertical = 8.dp),
                            )
                        }
                    }
                },
                confirmButton = { TextButton(onClick = { showChapters = false }) { Text("OK") } },
            )
        }

        if (showSettings) {
            AlertDialog(
                onDismissRequest = { showSettings = false },
                title = { Text(stringResource(KMR.strings.mod69_novels_look)) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(stringResource(KMR.strings.mod69_novels_size, fontSize.toInt()))
                        Slider(value = fontSize, onValueChange = {
                            fontSize = it
                            p.edit { putFloat("font_size", it) }
                        }, valueRange = 12f..32f)
                        Text(stringResource(KMR.strings.mod69_novels_spacing, "%.1f".format(lineHeight)))
                        Slider(value = lineHeight, onValueChange = {
                            lineHeight = it
                            p.edit { putFloat("line_height", it) }
                        }, valueRange = 1.1f..2.2f)
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf("serif" to "Serif", "sans" to "Sans", "mono" to "Mono").forEach { (k, label) ->
                                FilterChip(selected = font == k, onClick = {
                                    font = k
                                    p.edit { putString("font", k) }
                                }, label = { Text(label) })
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf("light" to "Light", "sepia" to "Sepia", "dark" to "Dark", "black" to "Black").forEach { (k, label) ->
                                FilterChip(selected = theme == k, onClick = {
                                    theme = k
                                    p.edit { putString("theme", k) }
                                }, label = { Text(label) })
                            }
                        }
                    }
                },
                confirmButton = { TextButton(onClick = { showSettings = false }) { Text("OK") } },
            )
        }
    }
}
// KMK <--
