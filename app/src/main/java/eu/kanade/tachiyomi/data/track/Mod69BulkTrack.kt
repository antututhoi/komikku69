package eu.kanade.tachiyomi.data.track

import android.content.Context
import eu.kanade.tachiyomi.data.notification.Mod69Progress
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import tachiyomi.domain.manga.model.Manga
import tachiyomi.domain.track.interactor.GetTracks
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

// KMK -->
/**
 * 69 mod N30: link one tracker to many library entries at once.
 * For each entry it searches the tracker with the title and links the best match
 * (same title first, otherwise the first result). Entries already linked are skipped.
 */
object Mod69BulkTrack {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun trackers(): List<Tracker> =
        Injekt.get<TrackerManager>().loggedInTrackers().filter { it !is EnhancedTracker }

    fun start(context: Context, tracker: Tracker, mangas: List<Manga>) {
        if (mangas.isEmpty()) return
        scope.launch {
            val id = Mod69Progress.ID_BULK_TRACK
            val title = "Linking ${tracker.name}"
            val started = System.currentTimeMillis()
            Mod69Progress.start(context, id, title)
            val existing = Injekt.get<GetTracks>().await(mangas.map { it.id })
            var linked = 0
            var skipped = 0
            val notFound = mutableListOf<String>()
            val failed = mutableListOf<String>()
            mangas.forEachIndexed { index, manga ->
                if (Mod69Progress.isStopped(id)) return@forEachIndexed
                Mod69Progress.update(context, id, title, manga.title, index, mangas.size)
                if (existing[manga.id].orEmpty().any { it.trackerId == tracker.id }) {
                    skipped++
                    return@forEachIndexed
                }
                try {
                    val results = tracker.search(manga.title)
                    val best = results.firstOrNull { it.title.equals(manga.title, ignoreCase = true) }
                        ?: results.firstOrNull()
                    if (best == null) {
                        notFound += manga.title
                    } else {
                        tracker.register(best, manga.id)
                        linked++
                    }
                } catch (e: Exception) {
                    failed += "${manga.title}: ${e.message?.take(50)}"
                }
                delay(800) // be gentle with the tracker site
            }
            val lines = buildList {
                add("Linked $linked, already linked $skipped, not found ${notFound.size}, errors ${failed.size}")
                add("Time: ${Mod69Progress.duration(System.currentTimeMillis() - started)}")
                if (notFound.isNotEmpty()) add("Not found: " + notFound.take(15).joinToString(", "))
                failed.take(10).forEach { add("• $it") }
                add("Check the matches on each entry's tracking screen")
            }
            Mod69Progress.finish(context, id, "${tracker.name}: linking finished", lines)
        }
    }
}
// KMK <--
