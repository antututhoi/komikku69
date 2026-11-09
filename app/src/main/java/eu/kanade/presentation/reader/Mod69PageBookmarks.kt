package eu.kanade.presentation.reader

import tachiyomi.core.common.preference.PreferenceStore
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

// KMK -->
/** 69 mod R17: bookmarks for single pages. Saved as "mangaId|chapterId|pageIndex|chapter name". */
object Mod69PageBookmarks {
    data class Item(val mangaId: Long, val chapterId: Long, val pageIndex: Int, val chapterName: String)

    private val pref get() = Injekt.get<PreferenceStore>().getStringSet("mod69_page_bookmarks", emptySet())

    fun forManga(mangaId: Long): List<Item> = pref.get().mapNotNull { raw ->
        val parts = raw.split("|", limit = 4)
        if (parts.size < 4) return@mapNotNull null
        Item(
            parts[0].toLongOrNull() ?: return@mapNotNull null,
            parts[1].toLongOrNull() ?: return@mapNotNull null,
            parts[2].toIntOrNull() ?: return@mapNotNull null,
            parts[3],
        )
    }.filter { it.mangaId == mangaId }.sortedWith(compareBy({ it.chapterName }, { it.pageIndex }))

    private fun key(item: Item) = "${item.mangaId}|${item.chapterId}|${item.pageIndex}|${item.chapterName}"

    fun isBookmarked(mangaId: Long, chapterId: Long, pageIndex: Int): Boolean =
        forManga(mangaId).any { it.chapterId == chapterId && it.pageIndex == pageIndex }

    /** Adds the bookmark, or removes it when it already exists. Returns true when it is now bookmarked. */
    fun toggle(item: Item): Boolean {
        val existing = pref.get().firstOrNull {
            val p = it.split("|", limit = 4)
            p.size >= 3 && p[0] == item.mangaId.toString() && p[1] == item.chapterId.toString() &&
                p[2] == item.pageIndex.toString()
        }
        return if (existing != null) {
            pref.set(pref.get() - existing)
            false
        } else {
            pref.set(pref.get() + key(item))
            true
        }
    }
}
// KMK <--
