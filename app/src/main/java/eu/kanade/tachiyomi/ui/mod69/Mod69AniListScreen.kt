package eu.kanade.tachiyomi.ui.mod69

import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import coil3.compose.AsyncImage
import eu.kanade.domain.manga.interactor.UpdateManga
import eu.kanade.domain.source.service.SourcePreferences
import eu.kanade.presentation.components.AppBar
import eu.kanade.presentation.util.Screen
import eu.kanade.tachiyomi.data.notification.Mod69Progress
import eu.kanade.tachiyomi.data.track.TrackerManager
import eu.kanade.tachiyomi.data.track.anilist.AnilistInterceptor
import eu.kanade.tachiyomi.network.NetworkHelper
import eu.kanade.tachiyomi.network.awaitSuccess
import eu.kanade.tachiyomi.ui.browse.source.globalsearch.GlobalSearchScreen
import eu.kanade.tachiyomi.util.system.toast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
import mihon.feature.migration.list.search.SmartSourceSearchEngine
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import tachiyomi.core.common.util.lang.withIOContext
import tachiyomi.domain.manga.interactor.NetworkToLocalManga
import tachiyomi.domain.source.service.SourceManager
import tachiyomi.domain.track.interactor.GetTracks
import tachiyomi.i18n.kmk.KMR
import tachiyomi.presentation.core.components.material.Scaffold
import tachiyomi.presentation.core.i18n.stringResource
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

// KMK -->
/** 69 mod T4: your AniList manga list inside the app; open or auto-match titles to your sources. */
data class Mod69AniEntry(
    val mediaId: Long,
    val title: String,
    val cover: String?,
    val status: String,
    val progress: Int,
    val total: Int?,
    val inLibrary: Boolean,
)

class Mod69AniListScreen : Screen() {

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val context = LocalContext.current
        var entries by remember { mutableStateOf<List<Mod69AniEntry>?>(null) }
        var error by remember { mutableStateOf<String?>(null) }
        var status by remember { mutableStateOf("CURRENT") }

        LaunchedEffect(Unit) {
            runCatching { entries = load() }.onFailure {
                error = it.message
                entries = emptyList()
            }
        }

        Scaffold(
            topBar = { scrollBehavior ->
                AppBar(
                    title = stringResource(KMR.strings.mod69_anilist_list),
                    navigateUp = navigator::pop,
                    scrollBehavior = scrollBehavior,
                )
            },
        ) { padding ->
            Column(modifier = Modifier.padding(padding).fillMaxSize()) {
                val all = entries
                if (all == null) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                    return@Column
                }
                if (all.isEmpty()) {
                    Text(error ?: stringResource(KMR.strings.mod69_anilist_empty), modifier = Modifier.padding(16.dp))
                    return@Column
                }
                val statuses = listOf("CURRENT", "PLANNING", "COMPLETED", "PAUSED", "DROPPED", "REPEATING")
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    items(statuses) { s ->
                        FilterChip(
                            selected = s == status,
                            onClick = { status = s },
                            label = { Text("${statusName(s)} (${all.count { it.status == s }})") },
                        )
                    }
                }
                val shown = all.filter { it.status == status }
                val missing = shown.filter { !it.inLibrary }
                if (missing.isNotEmpty()) {
                    Button(
                        onClick = {
                            Mod69AniMatch.start(context, missing)
                            context.toast(KMR.strings.mod69_anilist_matching)
                        },
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                    ) {
                        Text(stringResource(KMR.strings.mod69_anilist_match, missing.size))
                    }
                }
                LazyColumn(
                    contentPadding = PaddingValues(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(shown, key = { it.mediaId }) { item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { navigator.push(GlobalSearchScreen(item.title)) },
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            AsyncImage(
                                model = item.cover,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.width(56.dp).height(80.dp).clip(RoundedCornerShape(6.dp)),
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    item.title,
                                    style = MaterialTheme.typography.titleSmall,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Text(
                                    "${item.progress}${item.total?.let { " / $it" }.orEmpty()}",
                                    style = MaterialTheme.typography.bodySmall,
                                )
                                Text(
                                    stringResource(
                                        if (item.inLibrary) KMR.strings.mod69_anilist_in_library else KMR.strings.mod69_anilist_not_in_library,
                                    ),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (item.inLibrary) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    private fun statusName(s: String) = when (s) {
        "CURRENT" -> "Reading"
        "PLANNING" -> "Planning"
        "COMPLETED" -> "Completed"
        "PAUSED" -> "Paused"
        "DROPPED" -> "Dropped"
        else -> "Rereading"
    }

    private suspend fun load(): List<Mod69AniEntry> = withIOContext {
        val anilist = Injekt.get<TrackerManager>().aniList
        if (!anilist.isLoggedIn) error("Log in to AniList first (Settings → Tracking)")
        val userId = anilist.getUsername().toInt()
        val linked = Injekt.get<GetTracks>().await()
            .filter { it.trackerId == TrackerManager.ANILIST }
            .map { it.remoteId }
            .toSet()
        val query = """
            query (${'$'}userId: Int) {
              MediaListCollection(userId: ${'$'}userId, type: MANGA) {
                lists {
                  entries {
                    status
                    progress
                    media { id chapters coverImage { large } title { userPreferred } }
                  }
                }
              }
            }
        """.trimIndent()
        val body = buildJsonObject {
            put("query", query)
            putJsonObject("variables") { put("userId", userId) }
        }.toString().toRequestBody("application/json".toMediaType())
        val client = Injekt.get<NetworkHelper>().client.newBuilder()
            .addInterceptor(AnilistInterceptor(anilist, anilist.getPassword()))
            .build()
        val text = client.newCall(Request.Builder().url("https://graphql.anilist.co/").post(body).build())
            .awaitSuccess()
            .use { it.body.string() }
        val lists = Json.parseToJsonElement(text).jsonObject["data"]!!.jsonObject["MediaListCollection"]!!
            .jsonObject["lists"]!!.jsonArray
        lists.flatMap { it.jsonObject["entries"]!!.jsonArray }.mapNotNull { e ->
            val o = e.jsonObject
            val media = o["media"]?.jsonObject ?: return@mapNotNull null
            val id = media["id"]?.jsonPrimitive?.longOrNull ?: return@mapNotNull null
            Mod69AniEntry(
                mediaId = id,
                title = media["title"]?.jsonObject?.get("userPreferred")?.jsonPrimitive?.content ?: return@mapNotNull null,
                cover = media["coverImage"]?.jsonObject?.get("large")?.jsonPrimitive?.content,
                status = o["status"]?.jsonPrimitive?.content ?: "CURRENT",
                progress = o["progress"]?.jsonPrimitive?.intOrNull ?: 0,
                total = media["chapters"]?.jsonPrimitive?.intOrNull,
                inLibrary = id in linked,
            )
        }.distinctBy { it.mediaId }
    }
}

/** Finds each AniList title on your sources, adds it to the library and links it to its AniList entry. */
object Mod69AniMatch {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun start(context: Context, entries: List<Mod69AniEntry>) {
        scope.launch {
            val id = Mod69Progress.ID_ANILIST_MATCH
            val title = "Matching AniList titles"
            val started = System.currentTimeMillis()
            Mod69Progress.start(context, id, title)
            val prefs = Injekt.get<SourcePreferences>()
            val languages = prefs.enabledLanguages().get()
            val disabled = prefs.disabledSources().get()
            val pinned = prefs.pinnedSources().get()
            val sources = Injekt.get<SourceManager>().getOnlineSources()
                .filter { it.lang in languages && it.id.toString() !in disabled }
                .sortedByDescending { it.id.toString() in pinned }
                .take(8)
            val engine = SmartSourceSearchEngine(null)
            val anilist = Injekt.get<TrackerManager>().aniList
            var added = 0
            val notFound = mutableListOf<String>()
            entries.forEachIndexed { index, entry ->
                if (Mod69Progress.isStopped(id)) return@forEachIndexed
                Mod69Progress.update(context, id, title, entry.title, index, entries.size)
                val found = sources.firstNotNullOfOrNull { source ->
                    withTimeoutOrNull(20_000) { runCatching { engine.regularSearch(source, entry.title) }.getOrNull() }
                }
                if (found == null) {
                    notFound += entry.title
                    return@forEachIndexed
                }
                runCatching {
                    val local = Injekt.get<NetworkToLocalManga>()(found)
                    Injekt.get<UpdateManga>().awaitUpdateFavorite(local.id, true)
                    anilist.searchById(entry.mediaId.toString())?.let { anilist.register(it, local.id) }
                    added++
                }.onFailure { notFound += entry.title }
            }
            val lines = buildList {
                add("Added and linked $added of ${entries.size}; not found ${notFound.size}")
                add("Time: ${Mod69Progress.duration(System.currentTimeMillis() - started)}")
                if (notFound.isNotEmpty()) add("Not found: " + notFound.take(20).joinToString(", "))
            }
            Mod69Progress.finish(context, id, "AniList matching finished", lines)
        }
    }
}
// KMK <--
