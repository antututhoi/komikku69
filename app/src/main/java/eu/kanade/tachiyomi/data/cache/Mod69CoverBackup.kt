package eu.kanade.tachiyomi.data.cache

import android.content.Context
import android.net.Uri
import org.json.JSONArray
import org.json.JSONObject
import tachiyomi.domain.manga.interactor.GetFavorites
import tachiyomi.domain.manga.model.Manga
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.io.File
import java.security.MessageDigest
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

// KMK -->
/**
 * 69 mod: back up / restore the covers of all library entries (normal and custom ones) to one ZIP file.
 * Entries are matched by source + url, so a restore also works after reinstalling and restoring a backup.
 */
object Mod69CoverBackup {

    data class Result(val custom: Int, val normal: Int)

    private val P = eu.kanade.tachiyomi.data.notification.Mod69Progress

    private fun key(manga: Manga): String {
        val md = MessageDigest.getInstance("SHA-1")
        return md.digest("${manga.source}|${manga.url}".toByteArray()).joinToString("") { "%02x".format(it) }
    }

    private fun fileName(context: Context, uri: Uri): String =
        runCatching { com.hippo.unifile.UniFile.fromUri(context, uri)?.name }.getOrNull()
            ?: uri.lastPathSegment.orEmpty()

    suspend fun export(context: Context, target: Uri): Result {
        val id = P.ID_COVER_BACKUP
        val title = "Backing up covers"
        val started = System.currentTimeMillis()
        P.start(context, id, title)
        val coverCache = Injekt.get<CoverCache>()
        val favorites = Injekt.get<GetFavorites>().await()
        var custom = 0
        var normal = 0
        var noCover = 0
        var bytes = 0L
        val index = JSONArray()
        try {
            context.contentResolver.openOutputStream(target)?.use { raw ->
                ZipOutputStream(raw.buffered()).use { zip ->
                    for ((i, manga) in favorites.withIndex()) {
                        if (P.isStopped(id)) break
                        P.update(context, id, title, manga.title, i + 1, favorites.size)
                        val k = key(manga)
                        val entry = JSONObject().put("key", k).put("title", manga.title)
                        val customFile = coverCache.getCustomCoverFile(manga.id)
                        if (customFile.exists()) {
                            zip.putNextEntry(ZipEntry("custom/$k"))
                            customFile.inputStream().use { it.copyTo(zip) }
                            zip.closeEntry()
                            entry.put("custom", true)
                            bytes += customFile.length()
                            custom++
                        }
                        val normalFile = coverCache.getCoverFile(manga.thumbnailUrl)
                        if (normalFile != null && normalFile.exists()) {
                            zip.putNextEntry(ZipEntry("normal/$k"))
                            normalFile.inputStream().use { it.copyTo(zip) }
                            zip.closeEntry()
                            entry.put("normal", true)
                            bytes += normalFile.length()
                            normal++
                        } else if (!customFile.exists()) {
                            noCover++
                        }
                        index.put(entry)
                    }
                    zip.putNextEntry(ZipEntry("index.json"))
                    zip.write(index.toString(1).toByteArray())
                    zip.closeEntry()
                }
            } ?: error("cannot write file")
        } catch (e: Exception) {
            P.finish(context, id, "Cover backup failed", listOf(e.message ?: e.toString()))
            throw e
        }
        P.finish(
            context,
            id,
            "Cover backup finished",
            listOf(
                "Library entries: ${favorites.size}",
                "Covers saved: $normal",
                "Own covers saved: $custom",
                "Entries without a cover: $noCover",
                "Size: ${P.size(bytes)}",
                "Saved as: ${fileName(context, target)}",
                "Time: ${P.duration(System.currentTimeMillis() - started)}",
            ),
        )
        return Result(custom, normal)
    }

    suspend fun import(context: Context, source: Uri): Result {
        val id = P.ID_COVER_RESTORE
        val title = "Restoring covers"
        val started = System.currentTimeMillis()
        P.start(context, id, title)
        val coverCache = Injekt.get<CoverCache>()
        val byKey = Injekt.get<GetFavorites>().await().associateBy { key(it) }
        var custom = 0
        var normal = 0
        var notInLibrary = 0
        var failed = 0
        var seen = 0
        // the number of covers in the file, for "x / total" (0 = unknown)
        val total = runCatching {
            context.contentResolver.openInputStream(source)?.use { raw ->
                ZipInputStream(raw.buffered()).use { zip ->
                    var n = 0
                    while (zip.nextEntry != null) n++
                    n - 1
                }
            } ?: 0
        }.getOrDefault(0).coerceAtLeast(0)
        try {
            context.contentResolver.openInputStream(source)?.use { raw ->
                ZipInputStream(raw.buffered()).use { zip ->
                    while (true) {
                        if (P.isStopped(id)) break
                        val e = zip.nextEntry ?: break
                        val parts = e.name.split('/')
                        if (parts.size == 2) {
                            seen++
                            val manga = byKey[parts[1]]
                            P.update(context, id, title, manga?.title.orEmpty(), seen, total)
                            val dest: File? = when {
                                manga == null -> null
                                parts[0] == "custom" -> coverCache.getCustomCoverFile(manga.id)
                                parts[0] == "normal" -> coverCache.getCoverFile(manga.thumbnailUrl)
                                else -> null
                            }
                            if (manga == null) {
                                notInLibrary++
                            } else if (dest != null) {
                                runCatching {
                                    dest.parentFile?.mkdirs()
                                    dest.outputStream().use { zip.copyTo(it) }
                                    if (parts[0] == "custom") custom++ else normal++
                                }.onFailure { failed++ }
                            }
                        }
                        zip.closeEntry()
                    }
                }
            } ?: error("cannot read file")
        } catch (e: Exception) {
            P.finish(context, id, "Cover restore failed", listOf(e.message ?: e.toString()))
            throw e
        }
        P.finish(
            context,
            id,
            "Cover restore finished",
            listOf(
                "Covers restored: $normal",
                "Own covers restored: $custom",
                "Skipped (entry not in library): $notInLibrary",
                "Failed: $failed",
                "From: ${fileName(context, source)}",
                "Time: ${P.duration(System.currentTimeMillis() - started)}",
            ),
        )
        return Result(custom, normal)
    }

    /**
     * Copies covers from another Komikku's "covers" folder (e.g. app.komikku.beta/files/covers).
     * Both apps name a cover file after its picture address, so each library entry finds its own cover.
     * Own (custom) covers are named by an internal number that differs between apps, so they are skipped.
     */
    suspend fun importFolder(context: Context, tree: Uri): Result {
        val id = P.ID_COVER_IMPORT
        val title = "Importing covers"
        val started = System.currentTimeMillis()
        P.start(context, id, title)
        val coverCache = Injekt.get<CoverCache>()
        var dir = com.hippo.unifile.UniFile.fromUri(context, tree) ?: error("cannot open folder")
        // picked "app.komikku.beta" or "files": go down to "covers"
        repeat(3) {
            if (dir.findFile("covers")?.isDirectory == true) {
                dir = dir.findFile("covers")!!
            } else if (dir.findFile("files")?.isDirectory == true) {
                dir = dir.findFile("files")!!
            }
        }
        P.update(context, id, title, "Reading the folder…", 0, 0, force = true)
        val byName = dir.listFiles().orEmpty().filter { it.isFile }.associateBy { it.name.orEmpty() }
        val favorites = Injekt.get<GetFavorites>().await()
        var normal = 0
        var notFound = 0
        var failed = 0
        var bytes = 0L
        for ((i, manga) in favorites.withIndex()) {
            if (P.isStopped(id)) break
            P.update(context, id, title, manga.title, i + 1, favorites.size)
            val dest = coverCache.getCoverFile(manga.thumbnailUrl)
            val src = dest?.let { byName[it.name] }
            if (dest == null || src == null) {
                notFound++
                continue
            }
            runCatching {
                dest.parentFile?.mkdirs()
                src.openInputStream().use { input ->
                    dest.outputStream().use { input.copyTo(it) }
                }
                bytes += dest.length()
                normal++
            }.onFailure { failed++ }
        }
        P.finish(
            context,
            id,
            "Cover import finished",
            listOf(
                "Covers in the folder: ${byName.size}",
                "Covers copied: $normal",
                "Library entries with no cover there: $notFound",
                "Failed: $failed",
                "Size: ${P.size(bytes)}",
                "Time: ${P.duration(System.currentTimeMillis() - started)}",
            ),
        )
        return Result(0, normal)
    }
}
// KMK <--
