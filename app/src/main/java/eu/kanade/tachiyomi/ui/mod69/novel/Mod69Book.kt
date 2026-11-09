package eu.kanade.tachiyomi.ui.mod69.novel

import android.content.Context
import android.net.Uri
import org.jsoup.Jsoup
import org.jsoup.parser.Parser
import java.io.File
import java.util.zip.ZipFile

// KMK -->
/** 69 mod O1: a book read as text: chapters made of paragraphs. EPUB and TXT. */
data class Mod69Book(val title: String, val author: String?, val chapters: List<Chapter>) {
    data class Chapter(val title: String, val paragraphs: List<String>)

    companion object {
        fun load(context: Context, uri: Uri, name: String): Mod69Book {
            val cache = File(context.cacheDir, "mod69_book").apply { mkdirs() }
            val file = File(cache, "current")
            context.contentResolver.openInputStream(uri)!!.use { input -> file.outputStream().use { input.copyTo(it) } }
            return if (name.lowercase().endsWith(".epub")) epub(file, name) else txt(file, name)
        }

        private fun txt(file: File, name: String): Mod69Book {
            val text = file.readText()
            val heading = Regex(
                """^\s*((chapter|chương|chapitre|kapitel|capítulo|capitolo|глава)\s*\d+.*|第[0-9一二三四五六七八九十百千]+[章话話回].*)$""",
                setOf(RegexOption.IGNORE_CASE, RegexOption.MULTILINE),
            )
            val starts = heading.findAll(text).map { it.range.first }.toList()
            val chapters = if (starts.size >= 2) {
                (starts + text.length).zipWithNext { a, b ->
                    val part = text.substring(a, b)
                    val lines = part.lines().map { it.trim() }.filter { it.isNotEmpty() }
                    Chapter(lines.first().take(80), lines.drop(1))
                }
            } else {
                // no chapter titles: pieces of about 40 paragraphs
                text.lines().map { it.trim() }.filter { it.isNotEmpty() }.chunked(40)
                    .mapIndexed { i, lines -> Chapter("Part ${i + 1}", lines) }
            }
            return Mod69Book(name.substringBeforeLast('.'), null, chapters)
        }

        private fun epub(file: File, name: String): Mod69Book {
            ZipFile(file).use { zip ->
                fun read(path: String) = zip.getEntry(path)?.let { e -> zip.getInputStream(e).use { it.readBytes().decodeToString() } }
                val container = read("META-INF/container.xml") ?: error("Not an EPUB")
                val opfPath = Jsoup.parse(container, "", Parser.xmlParser()).selectFirst("rootfile")?.attr("full-path")
                    ?: error("Not an EPUB")
                val base = opfPath.substringBeforeLast('/', "").let { if (it.isEmpty()) "" else "$it/" }
                val opf = Jsoup.parse(read(opfPath) ?: error("Broken EPUB"), "", Parser.xmlParser())
                val title = opf.selectFirst("dc|title, title")?.text()?.takeIf { it.isNotBlank() } ?: name.substringBeforeLast('.')
                val author = opf.selectFirst("dc|creator, creator")?.text()
                val manifest = opf.select("manifest > item").associate { it.attr("id") to it.attr("href") }
                val spine = opf.select("spine > itemref").mapNotNull { manifest[it.attr("idref")] }
                val chapters = spine.mapNotNull { href ->
                    val path = normalize(base + java.net.URLDecoder.decode(href, "UTF-8"))
                    val html = read(path) ?: return@mapNotNull null
                    val doc = Jsoup.parse(html)
                    val paragraphs = doc.body()?.select("p, h1, h2, h3, h4, li, blockquote, pre")
                        ?.filter { el -> el.parents().none { it.tagName() in setOf("p", "li", "blockquote") } }
                        ?.map { it.text().trim() }
                        ?.filter { it.isNotEmpty() }
                        .orEmpty()
                        .ifEmpty { doc.body()?.text()?.takeIf { it.isNotBlank() }?.let { listOf(it) }.orEmpty() }
                    if (paragraphs.isEmpty()) return@mapNotNull null
                    val chapterTitle = doc.selectFirst("h1, h2, h3, title")?.text()?.takeIf { it.isNotBlank() }
                        ?: paragraphs.first().take(60)
                    Chapter(chapterTitle, paragraphs)
                }
                return Mod69Book(title, author, chapters.ifEmpty { listOf(Chapter(title, listOf("(empty)"))) })
            }
        }

        private fun normalize(path: String): String {
            val out = ArrayDeque<String>()
            path.split('/').forEach { part ->
                when (part) {
                    "", "." -> Unit
                    ".." -> out.removeLastOrNull()
                    else -> out.addLast(part)
                }
            }
            return out.joinToString("/")
        }
    }
}
// KMK <--
