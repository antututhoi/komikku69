package eu.kanade.tachiyomi.ui.library

import java.time.LocalDate
import java.time.ZoneId

// KMK -->
/**
 * 69 mod L3 / P10: extra words for the library search.
 *
 * Numbers: chapters>50, unread>=10, read<5, bookmarks>0, chapters:10-50
 * Dates (year-month-day): added>2025-01-01, updated<2025-06-01, read>2025-03-01
 * Tags: tag:romance, -tag:horror, anytag:romance,comedy, notags
 * Source by name: source:mangadex
 * Anything else is searched as before.
 */
class Mod69LibraryQuery private constructor(
    val rest: String,
    private val checks: List<(LibraryItem) -> Boolean>,
) {
    val hasChecks get() = checks.isNotEmpty() || sourceChecks.isNotEmpty()

    fun matches(item: LibraryItem, sourceName: String?): Boolean = checks.all { it(item) } &&
        sourceChecks.all { name -> sourceName?.contains(name, ignoreCase = true) == true }

    private var sourceChecks: List<String> = emptyList()

    companion object {
        private val NUMBER = Regex(
            """(?i)(?<![\w-])(chapters|unread|read|bookmarks|downloaded)\s*(>=|<=|>|<|=|:)\s*(\d+)(?:-(\d+))?""",
        )
        private val DATE = Regex("""(?i)(?<![\w-])(added|updated|lastread)\s*(>=|<=|>|<|=)\s*(\d{4}-\d{1,2}-\d{1,2})""")
        private val TAG = Regex("""(?i)(?<![\w])(-?)tag:("[^"]+"|\S+)""")
        private val ANY_TAG = Regex("""(?i)(?<![\w-])anytag:(\S+)""")
        private val NO_TAGS = Regex("""(?i)(?<![\w-])notags(?![\w])""")
        private val SOURCE = Regex("""(?i)(?<![\w-])source:("[^"]+"|\S+)""")

        fun parse(query: String): Mod69LibraryQuery {
            var rest = query
            val checks = mutableListOf<(LibraryItem) -> Boolean>()

            NUMBER.findAll(query).forEach { m ->
                val field = m.groupValues[1].lowercase()
                val op = m.groupValues[2]
                val a = m.groupValues[3].toLong()
                val b = m.groupValues[4].toLongOrNull()
                checks += { item ->
                    val value = when (field) {
                        "chapters" -> item.libraryManga.totalChapters
                        "unread" -> item.libraryManga.unreadCount
                        "read" -> item.libraryManga.readCount
                        "bookmarks" -> item.libraryManga.bookmarkCount
                        else -> item.downloadCount.coerceAtLeast(0)
                    }
                    if (b != null) value in a..b else compare(value, op, a)
                }
                rest = rest.replace(m.value, " ")
            }

            DATE.findAll(query).forEach { m ->
                val field = m.groupValues[1].lowercase()
                val op = m.groupValues[2]
                val day = runCatching {
                    val (y, mo, d) = m.groupValues[3].split("-").map { it.toInt() }
                    LocalDate.of(y, mo, d).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
                }.getOrNull() ?: return@forEach
                checks += { item ->
                    val value = when (field) {
                        "added" -> item.libraryManga.manga.dateAdded
                        "updated" -> item.libraryManga.latestUpload
                        else -> item.libraryManga.lastRead
                    }
                    value > 0 && compare(value, op, day)
                }
                rest = rest.replace(m.value, " ")
            }

            TAG.findAll(query).forEach { m ->
                val exclude = m.groupValues[1] == "-"
                val tag = m.groupValues[2].trim('"').lowercase()
                checks += { item ->
                    val has = item.libraryManga.manga.genre.orEmpty().any { it.trim().lowercase() == tag }
                    if (exclude) !has else has
                }
                rest = rest.replace(m.value, " ")
            }

            ANY_TAG.findAll(query).forEach { m ->
                val tags = m.groupValues[1].split(",").map { it.trim().lowercase() }.filter { it.isNotEmpty() }
                checks += { item ->
                    item.libraryManga.manga.genre.orEmpty().any { it.trim().lowercase() in tags }
                }
                rest = rest.replace(m.value, " ")
            }

            if (NO_TAGS.containsMatchIn(query)) {
                checks += { item -> item.libraryManga.manga.genre.isNullOrEmpty() }
                rest = NO_TAGS.replace(rest, " ")
            }

            val sources = SOURCE.findAll(query).map { it.groupValues[1].trim('"') }.toList()
            rest = SOURCE.replace(rest, " ")

            return Mod69LibraryQuery(rest.replace(Regex("\\s+"), " ").trim(), checks).also {
                it.sourceChecks = sources
            }
        }

        private fun compare(value: Long, op: String, target: Long) = when (op) {
            ">" -> value > target
            ">=" -> value >= target
            "<" -> value < target
            "<=" -> value <= target
            else -> value == target
        }
    }
}
// KMK <--
