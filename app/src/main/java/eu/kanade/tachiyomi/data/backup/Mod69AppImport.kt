package eu.kanade.tachiyomi.data.backup

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.net.Uri
import eu.kanade.domain.manga.interactor.UpdateManga
import eu.kanade.domain.source.service.SourcePreferences
import eu.kanade.tachiyomi.data.notification.Mod69Progress
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import mihon.feature.migration.list.search.SmartSourceSearchEngine
import tachiyomi.domain.category.interactor.CreateCategoryWithName
import tachiyomi.domain.category.interactor.GetCategories
import tachiyomi.domain.category.interactor.SetMangaCategories
import tachiyomi.domain.manga.interactor.GetLibraryManga
import tachiyomi.domain.manga.interactor.NetworkToLocalManga
import tachiyomi.domain.source.service.SourceManager
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.io.File
import java.util.zip.ZipInputStream

// KMK -->
/**
 * 69 mod N26 / X5: bring the library of another app (Kotatsu family, Venera, or a plain list of titles).
 * Other apps use other sources, so every title is searched on your own sources and the best match
 * is added to the library, in a category with the same name as in the other app.
 */
object Mod69AppImport {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    data class Entry(val title: String, val category: String?)

    fun start(context: Context, uri: Uri) {
        scope.launch {
            val id = Mod69Progress.ID_APP_IMPORT
            val title = "Importing from another app"
            val started = System.currentTimeMillis()
            Mod69Progress.start(context, id, title)
            val entries = runCatching { read(context, uri) }.getOrElse { emptyList() }
                .distinctBy { it.title.lowercase() }
            if (entries.isEmpty()) {
                Mod69Progress.finish(
                    context,
                    id,
                    "Import: nothing found",
                    listOf("That file has no titles I can read (Kotatsu .zip, Venera .venera/.zip, or a .txt list)"),
                )
                return@launch
            }

            val sourcePrefs = Injekt.get<SourcePreferences>()
            val languages = sourcePrefs.enabledLanguages().get()
            val disabled = sourcePrefs.disabledSources().get()
            val pinned = sourcePrefs.pinnedSources().get()
            val sources = Injekt.get<SourceManager>().getOnlineSources()
                .filter { it.lang in languages && it.id.toString() !in disabled }
                .sortedByDescending { it.id.toString() in pinned }
                .take(8)
            val engine = SmartSourceSearchEngine(null)
            val networkToLocal = Injekt.get<NetworkToLocalManga>()
            val updateManga = Injekt.get<UpdateManga>()
            val setCategories = Injekt.get<SetMangaCategories>()
            val owned = Injekt.get<GetLibraryManga>().await().map { it.manga.title.lowercase() }.toMutableSet()
            val categoryIds = mutableMapOf<String, Long>()
            Injekt.get<GetCategories>().await().forEach { categoryIds[it.name.lowercase()] = it.id }

            var added = 0
            var already = 0
            val notFound = mutableListOf<String>()
            entries.forEachIndexed { index, entry ->
                if (Mod69Progress.isStopped(id)) return@forEachIndexed
                Mod69Progress.update(context, id, title, entry.title, index, entries.size)
                if (entry.title.lowercase() in owned) {
                    already++
                    return@forEachIndexed
                }
                val found = sources.firstNotNullOfOrNull { source ->
                    withTimeoutOrNull(20_000) {
                        runCatching { engine.regularSearch(source, entry.title) }.getOrNull()
                    }
                }
                if (found == null) {
                    notFound += entry.title
                    return@forEachIndexed
                }
                runCatching {
                    val local = networkToLocal(found)
                    updateManga.awaitUpdateFavorite(local.id, true)
                    entry.category?.takeIf { it.isNotBlank() }?.let { name ->
                        val catId = categoryIds[name.lowercase()] ?: run {
                            val result = Injekt.get<CreateCategoryWithName>().await(name)
                            (result as? CreateCategoryWithName.Result.Success)?.category?.id
                                ?.also { categoryIds[name.lowercase()] = it }
                        }
                        if (catId != null) setCategories.await(local.id, listOf(catId))
                    }
                    owned += entry.title.lowercase()
                    added++
                }.onFailure { notFound += entry.title }
            }
            val lines = buildList {
                add("Found ${entries.size} titles: added $added, already in library $already, not found ${notFound.size}")
                add("Searched on: " + sources.joinToString(", ") { it.name })
                add("Time: ${Mod69Progress.duration(System.currentTimeMillis() - started)}")
                if (notFound.isNotEmpty()) add("Not found: " + notFound.take(25).joinToString(", "))
                add("Run a library update to get the chapters")
            }
            Mod69Progress.finish(context, id, "Import finished", lines)
        }
    }

    private fun read(context: Context, uri: Uri): List<Entry> {
        val cache = File(context.cacheDir, "mod69_import").apply {
            deleteRecursively()
            mkdirs()
        }
        val raw = File(cache, "input")
        context.contentResolver.openInputStream(uri)!!.use { input -> raw.outputStream().use { input.copyTo(it) } }
        val isZip = raw.inputStream().use { it.read() == 'P'.code }
        if (!isZip) {
            // plain list: one title per line
            return raw.readLines().map { it.trim() }.filter { it.isNotEmpty() }.map { Entry(it, null) }
        }
        val files = mutableMapOf<String, File>()
        ZipInputStream(raw.inputStream()).use { zip ->
            generateSequence { zip.nextEntry }.filter { !it.isDirectory }.forEach { entry ->
                val name = entry.name.substringAfterLast('/')
                val out = File(cache, name)
                out.outputStream().use { zip.copyTo(it) }
                files[name] = out
            }
        }
        return readKotatsu(files).ifEmpty { readSqlite(files) }
    }

    /** Kotatsu family: JSON files "favourites" and "categories" (plus "history" for entries not in favourites). */
    private fun readKotatsu(files: Map<String, File>): List<Entry> {
        val favourites = files["favourites"] ?: files["favourites.json"] ?: return emptyList()
        val json = Json { ignoreUnknownKeys = true }
        val categories = (files["categories"] ?: files["categories.json"])?.let { file ->
            runCatching { json.parseToJsonElement(file.readText()) as JsonArray }.getOrNull()
                ?.mapNotNull { it as? JsonObject }
                ?.associate {
                    (it["category_id"]?.jsonPrimitive?.longOrNull ?: -1L) to
                        (it["title"]?.jsonPrimitive?.content ?: "")
                }
        }.orEmpty()
        val list = runCatching { json.parseToJsonElement(favourites.readText()) as JsonArray }.getOrNull()
            ?: return emptyList()
        return list.mapNotNull { element ->
            val obj = element as? JsonObject ?: return@mapNotNull null
            val manga = obj["manga"]?.jsonObject ?: return@mapNotNull null
            val title = manga["title"]?.jsonPrimitive?.content ?: return@mapNotNull null
            val category = obj["category_id"]?.jsonPrimitive?.longOrNull?.let { categories[it] }
            Entry(title, category)
        }
    }

    /** Venera and other apps that keep favourites in an SQLite file: each folder is a table. */
    private fun readSqlite(files: Map<String, File>): List<Entry> {
        val out = mutableListOf<Entry>()
        files.values.filter { it.name.endsWith(".db") && "favorite" in it.name.lowercase() }
            .ifEmpty { files.values.filter { it.name.endsWith(".db") } }
            .forEach { file ->
                runCatching {
                    SQLiteDatabase.openDatabase(file.path, null, SQLiteDatabase.OPEN_READONLY).use { db ->
                        val tables = db.rawQuery(
                            "SELECT name FROM sqlite_master WHERE type='table' AND name NOT LIKE 'sqlite_%' " +
                                "AND name NOT LIKE 'android_%' AND name NOT LIKE 'folder_%'",
                            null,
                        ).use { c -> generateSequence { if (c.moveToNext()) c.getString(0) else null }.toList() }
                        tables.forEach { table ->
                            db.rawQuery("SELECT * FROM \"$table\" LIMIT 1", null).use { probe ->
                                val column = listOf("name", "title").firstOrNull { probe.getColumnIndex(it) >= 0 }
                                    ?: return@forEach
                                db.rawQuery("SELECT \"$column\" FROM \"$table\"", null).use { c ->
                                    while (c.moveToNext()) {
                                        c.getString(0)?.trim()?.takeIf { it.isNotEmpty() }?.let {
                                            out += Entry(it, table.takeUnless { t -> t.equals("history", true) })
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        return out
    }
}
// KMK <--
