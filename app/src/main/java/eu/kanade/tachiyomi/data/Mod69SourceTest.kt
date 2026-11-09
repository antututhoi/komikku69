package eu.kanade.tachiyomi.data

import android.content.Context
import eu.kanade.domain.source.service.SourcePreferences
import eu.kanade.tachiyomi.data.notification.Mod69Progress
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import tachiyomi.domain.source.service.SourceManager
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.util.concurrent.atomic.AtomicInteger

// KMK -->
/** 69 mod B8: open the popular list of every enabled source and report the ones that fail. */
object Mod69SourceTest {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @Volatile
    var running = false
        private set

    fun start(context: Context) {
        if (running) return
        running = true
        scope.launch {
            val prefs = Injekt.get<SourcePreferences>()
            val languages = prefs.enabledLanguages().get()
            val disabled = prefs.disabledSources().get()
            val sources = Injekt.get<SourceManager>().getOnlineSources()
                .filter { it.lang in languages && it.id.toString() !in disabled }
            val title = "Testing sources"
            val started = System.currentTimeMillis()
            Mod69Progress.start(context, Mod69Progress.ID_SOURCE_TEST, title)
            val done = AtomicInteger(0)
            val broken = java.util.concurrent.ConcurrentLinkedQueue<Pair<Long, String>>()
            val empty = java.util.concurrent.ConcurrentLinkedQueue<String>()
            for (group in sources.chunked(6)) {
                if (Mod69Progress.isStopped(Mod69Progress.ID_SOURCE_TEST)) break
                coroutineScope {
                    group.map { source ->
                        async {
                            val result = withTimeoutOrNull(20_000) {
                                runCatching { source.getPopularManga(1) }
                            }
                            when {
                                result == null -> broken += source.id to "${source.name} (${source.lang}): too slow"
                                result.isFailure ->
                                    broken += source.id to
                                        "${source.name} (${source.lang}): ${result.exceptionOrNull()?.message?.take(60)}"
                                result.getOrNull()?.mangas.isNullOrEmpty() -> empty += "${source.name} (${source.lang})"
                            }
                            Mod69Progress.update(
                                context,
                                Mod69Progress.ID_SOURCE_TEST,
                                title,
                                source.name,
                                done.incrementAndGet(),
                                sources.size,
                            )
                        }
                    }.awaitAll()
                }
            }
            prefs.mod69BrokenSources().set(broken.map { it.first.toString() }.toSet())
            val lines = buildList {
                add("Tested ${done.get()} of ${sources.size} in ${Mod69Progress.duration(System.currentTimeMillis() - started)}")
                add("Working: ${done.get() - broken.size - empty.size}, broken: ${broken.size}, empty list: ${empty.size}")
                if (broken.isNotEmpty()) add("Broken:")
                broken.take(25).forEach { add("• ${it.second}") }
                if (empty.isNotEmpty()) add("Empty list: " + empty.take(15).joinToString(", "))
                if (broken.isNotEmpty()) add("Settings → Browse → \"Hide sources that failed\" hides them")
            }
            Mod69Progress.finish(context, Mod69Progress.ID_SOURCE_TEST, "Source test finished", lines)
            running = false
        }
    }

    /** Hides the sources that failed the last test. Returns how many. */
    fun hideBroken(): Int {
        val prefs = Injekt.get<SourcePreferences>()
        val broken = prefs.mod69BrokenSources().get()
        prefs.disabledSources().set(prefs.disabledSources().get() + broken)
        return broken.size
    }
}
// KMK <--
