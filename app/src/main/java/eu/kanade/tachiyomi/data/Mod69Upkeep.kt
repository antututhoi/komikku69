package eu.kanade.tachiyomi.data

import android.content.Context
import app.cash.sqldelight.db.SqlDriver
import coil3.imageLoader
import eu.kanade.tachiyomi.data.cache.ChapterCache
import eu.kanade.tachiyomi.data.notification.Mod69Progress
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import logcat.LogPriority
import tachiyomi.core.common.util.system.logcat
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.io.File

// KMK -->
/** 69 mod M1 / M2: database clean-up and cache clean-up, each with a finished notification. */
object Mod69Upkeep {

    private fun dbFiles(context: Context): List<File> {
        val names = listOf("tachiyomi.db", "tachiyomi.db-wal", "tachiyomi.db-shm", "tachiyomi.db-journal")
        return names.map { context.getDatabasePath(it) }.filter { it.exists() }
    }

    private fun dbSize(context: Context): Long = dbFiles(context).sumOf { it.length() }

    /** Rebuilds the database file (smaller) and refreshes its search statistics (faster). */
    suspend fun optimiseDatabase(context: Context) = withContext(Dispatchers.IO) {
        val title = "Database clean-up"
        val started = System.currentTimeMillis()
        Mod69Progress.start(context, Mod69Progress.ID_DB, title)
        val before = dbSize(context)
        val result = runCatching {
            val driver = Injekt.get<SqlDriver>()
            driver.execute(null, "PRAGMA wal_checkpoint(TRUNCATE)", 0)
            driver.execute(null, "ANALYZE", 0)
            driver.execute(null, "VACUUM", 0)
            driver.execute(null, "PRAGMA optimize", 0)
        }
        result.exceptionOrNull()?.let { logcat(LogPriority.ERROR, it) { "Database clean-up failed" } }
        val after = dbSize(context)
        Mod69Progress.finish(
            context,
            Mod69Progress.ID_DB,
            if (result.isSuccess) "Database clean-up finished" else "Database clean-up failed",
            if (result.isSuccess) {
                listOf(
                    "Before: ${Mod69Progress.size(before)}",
                    "After: ${Mod69Progress.size(after)}",
                    "Saved: ${Mod69Progress.size((before - after).coerceAtLeast(0))}",
                    "Time: ${Mod69Progress.duration(System.currentTimeMillis() - started)}",
                )
            } else {
                listOf(result.exceptionOrNull()?.message ?: "Unknown error")
            },
        )
    }

    private fun folderSize(file: File): Long =
        if (file.isFile) file.length() else file.listFiles().orEmpty().sumOf { folderSize(it) }

    /** Empties the page cache, the picture cache and other temporary files. */
    suspend fun clearAllCaches(context: Context) = withContext(Dispatchers.IO) {
        val title = "Cache clean-up"
        val started = System.currentTimeMillis()
        Mod69Progress.start(context, Mod69Progress.ID_CACHE, title)
        val before = folderSize(context.cacheDir) + (context.externalCacheDir?.let { folderSize(it) } ?: 0L)
        val lines = mutableListOf<String>()
        runCatching {
            val chapters = Injekt.get<ChapterCache>().clear()
            lines += "Page cache files deleted: $chapters"
        }
        runCatching {
            val loader = context.imageLoader
            loader.diskCache?.clear()
            loader.memoryCache?.clear()
            lines += "Picture cache emptied"
        }
        runCatching {
            context.cacheDir.listFiles().orEmpty()
                .filter { it.name.startsWith("tmp") || it.name.endsWith(".tmp") }
                .forEach { it.deleteRecursively() }
        }
        val after = folderSize(context.cacheDir) + (context.externalCacheDir?.let { folderSize(it) } ?: 0L)
        Mod69Progress.finish(
            context,
            Mod69Progress.ID_CACHE,
            "Cache clean-up finished",
            lines + listOf(
                "Before: ${Mod69Progress.size(before)}",
                "After: ${Mod69Progress.size(after)}",
                "Freed: ${Mod69Progress.size((before - after).coerceAtLeast(0))}",
                "Time: ${Mod69Progress.duration(System.currentTimeMillis() - started)}",
            ),
        )
    }
}
// KMK <--
