package eu.kanade.tachiyomi.ui.reader.viewer.navigation

import android.graphics.RectF
import eu.kanade.tachiyomi.ui.reader.setting.ReaderPreferences
import eu.kanade.tachiyomi.ui.reader.viewer.ViewerNavigation
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

// KMK -->
/**
 * 69 mod R16: tap zones the user draws on a 3 x 3 grid in Settings → Reader.
 * Each cell is one letter: P previous, N next, M menu, L left, R right, - nothing (counts as menu).
 */
class Mod69CustomNavigation : ViewerNavigation() {

    override var regionList: List<Region> = build(Injekt.get<ReaderPreferences>().mod69TapGrid().get())

    private fun build(grid: String): List<Region> {
        val cells = normalize(grid)
        val edges = listOf(0f, 1f / 3f, 2f / 3f, 1f)
        return cells.mapIndexedNotNull { index, letter ->
            val type = typeOf(letter) ?: return@mapIndexedNotNull null
            val column = index % 3
            val row = index / 3
            Region(
                rectF = RectF(edges[column], edges[row], edges[column + 1], edges[row + 1]),
                type = type,
            )
        }
    }

    companion object {
        const val DEFAULT = "PPPPMNNNN"
        val LETTERS = listOf('P', 'M', 'N', 'L', 'R')

        fun normalize(grid: String): String =
            grid.uppercase().filter { it in LETTERS }.padEnd(9, 'M').take(9)

        fun typeOf(letter: Char): ViewerNavigation.NavigationRegion? = when (letter) {
            'P' -> ViewerNavigation.NavigationRegion.PREV
            'N' -> ViewerNavigation.NavigationRegion.NEXT
            'L' -> ViewerNavigation.NavigationRegion.LEFT
            'R' -> ViewerNavigation.NavigationRegion.RIGHT
            'M' -> ViewerNavigation.NavigationRegion.MENU
            else -> null
        }
    }
}
// KMK <--
