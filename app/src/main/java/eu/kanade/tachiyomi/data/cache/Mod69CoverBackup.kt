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

    private fun key(manga: Manga): String {
        val md = MessageDigest.getInstance("SHA-1")
        return md.digest("${manga.source}|${manga.url}".toByteArray()).joinToString("") { "%02x".format(it) }
    }

    suspend fun export(context: Context, target: Uri): Result {
        val coverCache = Injekt.get<CoverCache>()
        val favorites = Injekt.get<GetFavorites>().await()
        var custom = 0
        var normal = 0
        val index = JSONArray()
        context.contentResolver.openOutputStream(target)?.use { raw ->
            ZipOutputStream(raw.buffered()).use { zip ->
                for (manga in favorites) {
                    val k = key(manga)
                    val entry = JSONObject().put("key", k).put("title", manga.title)
                    val customFile = coverCache.getCustomCoverFile(manga.id)
                    if (customFile.exists()) {
                        zip.putNextEntry(ZipEntry("custom/$k"))
                        customFile.inputStream().use { it.copyTo(zip) }
                        zip.closeEntry()
                        entry.put("custom", true)
                        custom++
                    }
                    val normalFile = coverCache.getCoverFile(manga.thumbnailUrl)
                    if (normalFile != null && normalFile.exists()) {
                        zip.putNextEntry(ZipEntry("normal/$k"))
                        normalFile.inputStream().use { it.copyTo(zip) }
                        zip.closeEntry()
                        entry.put("normal", true)
                        normal++
                    }
                    index.put(entry)
                }
                zip.putNextEntry(ZipEntry("index.json"))
                zip.write(index.toString(1).toByteArray())
                zip.closeEntry()
            }
        } ?: error("cannot write file")
        return Result(custom, normal)
    }

    suspend fun import(context: Context, source: Uri): Result {
        val coverCache = Injekt.get<CoverCache>()
        val byKey = Injekt.get<GetFavorites>().await().associateBy { key(it) }
        var custom = 0
        var normal = 0
        context.contentResolver.openInputStream(source)?.use { raw ->
            ZipInputStream(raw.buffered()).use { zip ->
                while (true) {
                    val e = zip.nextEntry ?: break
                    val parts = e.name.split('/')
                    if (parts.size == 2) {
                        val manga = byKey[parts[1]]
                        val dest: File? = when {
                            manga == null -> null
                            parts[0] == "custom" -> coverCache.getCustomCoverFile(manga.id)
                            parts[0] == "normal" -> coverCache.getCoverFile(manga.thumbnailUrl)
                            else -> null
                        }
                        if (dest != null) {
                            dest.parentFile?.mkdirs()
                            dest.outputStream().use { zip.copyTo(it) }
                            if (parts[0] == "custom") custom++ else normal++
                        }
                    }
                    zip.closeEntry()
                }
            }
        } ?: error("cannot read file")
        return Result(custom, normal)
    }

    /**
     * Copies covers from another Komikku's "covers" folder (e.g. app.komikku.beta/files/covers).
     * Both apps name a cover file after its picture address, so each library entry finds its own cover.
     * Own (custom) covers are named by an internal number that differs between apps, so they are skipped.
     */
    suspend fun importFolder(context: Context, tree: Uri): Result {
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
        val byName = dir.listFiles().orEmpty().filter { it.isFile }.associateBy { it.name.orEmpty() }
        var normal = 0
        for (manga in Injekt.get<GetFavorites>().await()) {
            val dest = coverCache.getCoverFile(manga.thumbnailUrl) ?: continue
            val src = byName[dest.name] ?: continue
            runCatching {
                dest.parentFile?.mkdirs()
                src.openInputStream().use { input ->
                    dest.outputStream().use { input.copyTo(it) }
                }
                normal++
            }
        }
        return Result(0, normal)
    }
}
// KMK <--
