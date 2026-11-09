package eu.kanade.tachiyomi.ui.manga

import tachiyomi.domain.chapter.model.Chapter

// KMK -->
/**
 * 69 mod N9 / N10: when the same chapter number comes from several groups (scanlators),
 * keep one copy. The group is chosen by:
 * 1. the first name in the user's "preferred groups" list that has this chapter,
 * 2. otherwise the group that has the most chapters in this entry,
 * 3. otherwise the first copy.
 * Chapters without a number (or -1) are always kept.
 */
object Mod69ChapterDedupe {

    fun parseGroups(raw: String): List<String> =
        raw.split(",", "\n").map { it.trim().lowercase() }.filter { it.isNotEmpty() }

    fun <T> pick(items: List<T>, preferred: List<String>, chapterOf: (T) -> Chapter): List<T> {
        val groupSize = items.groupingBy { chapterOf(it).scanlator.orEmpty().trim().lowercase() }.eachCount()
        val keepIds = items
            .filter { chapterOf(it).chapterNumber >= 0 }
            .groupBy { chapterOf(it).chapterNumber }
            .values
            .map { copies ->
                copies.minWith(
                    compareBy<T> { copy ->
                        val group = chapterOf(copy).scanlator.orEmpty().trim().lowercase()
                        preferred.indexOfFirst { group.contains(it) }.let { if (it < 0) Int.MAX_VALUE else it }
                    }.thenByDescending { copy ->
                        groupSize[chapterOf(copy).scanlator.orEmpty().trim().lowercase()] ?: 0
                    },
                )
            }
            .map { chapterOf(it).id }
            .toSet()
        return items.filter { chapterOf(it).chapterNumber < 0 || chapterOf(it).id in keepIds }
    }
}
// KMK <--
