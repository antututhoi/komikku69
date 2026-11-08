package eu.kanade.presentation.library.components

import tachiyomi.domain.library.service.LibraryPreferences
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

// KMK -->
/** 69 mod: entries marked for "Update marked entries". */
object Mod69Marked {
    private val pref get() = Injekt.get<LibraryPreferences>().mod69MarkedForUpdate()

    fun ids(): List<Long> = pref.get().mapNotNull { it.toLongOrNull() }

    fun isMarked(id: Long): Boolean = id.toString() in pref.get()

    fun set(ids: Collection<Long>, marked: Boolean) {
        val strings = ids.map { it.toString() }.toSet()
        pref.set(if (marked) pref.get() + strings else pref.get() - strings)
    }
}
// KMK <--
