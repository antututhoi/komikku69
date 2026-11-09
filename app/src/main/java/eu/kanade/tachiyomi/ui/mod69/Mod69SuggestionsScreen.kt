package eu.kanade.tachiyomi.ui.mod69

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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import coil3.compose.AsyncImage
import eu.kanade.presentation.components.AppBar
import eu.kanade.presentation.util.Screen
import eu.kanade.tachiyomi.network.NetworkHelper
import eu.kanade.tachiyomi.network.awaitSuccess
import eu.kanade.tachiyomi.ui.browse.source.globalsearch.GlobalSearchScreen
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.int
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import tachiyomi.core.common.preference.PreferenceStore
import tachiyomi.core.common.util.lang.withIOContext
import tachiyomi.domain.manga.interactor.GetLibraryManga
import tachiyomi.i18n.kmk.KMR
import tachiyomi.presentation.core.components.material.Scaffold
import tachiyomi.presentation.core.i18n.stringResource
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

// KMK -->
/**
 * 69 mod M4: suggestions from what you read. Takes the tags of your library (weighted by chapters read),
 * asks AniList for popular titles with those tags, and leaves out what you already have and the tags you exclude.
 */
data class Mod69Suggestion(val title: String, val cover: String?, val genres: List<String>, val score: Int?)

class Mod69SuggestionsScreen : Screen() {

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val excludedPref = remember {
            Injekt.get<PreferenceStore>().getStringSet("mod69_suggest_excluded", emptySet())
        }
        var excluded by remember { mutableStateOf(excludedPref.get()) }
        var topTags by remember { mutableStateOf<List<String>>(emptyList()) }
        var results by remember { mutableStateOf<List<Mod69Suggestion>?>(null) }
        var error by remember { mutableStateOf<String?>(null) }

        LaunchedEffect(excluded) {
            results = null
            error = null
            runCatching {
                val (tags, list) = load(excluded)
                topTags = tags
                results = list
            }.onFailure {
                error = it.message
                results = emptyList()
            }
        }

        Scaffold(
            topBar = { scrollBehavior ->
                AppBar(
                    title = stringResource(KMR.strings.mod69_suggestions),
                    navigateUp = navigator::pop,
                    scrollBehavior = scrollBehavior,
                )
            },
        ) { padding ->
            Column(modifier = Modifier.padding(padding).fillMaxSize()) {
                if (topTags.isNotEmpty()) {
                    Text(
                        stringResource(KMR.strings.mod69_suggestions_tags),
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                    )
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        items(topTags) { tag ->
                            FilterChip(
                                selected = tag !in excluded,
                                onClick = {
                                    excluded = if (tag in excluded) excluded - tag else excluded + tag
                                    excludedPref.set(excluded)
                                },
                                label = { Text(tag) },
                            )
                        }
                    }
                }
                when {
                    results == null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                    results!!.isEmpty() -> Text(
                        error ?: stringResource(KMR.strings.mod69_suggestions_none),
                        modifier = Modifier.padding(16.dp),
                    )
                    else -> LazyColumn(
                        contentPadding = PaddingValues(12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        items(results!!, key = { it.title }) { item ->
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
                                    modifier = Modifier
                                        .width(70.dp)
                                        .height(100.dp)
                                        .clip(RoundedCornerShape(6.dp)),
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        item.title,
                                        style = MaterialTheme.typography.titleSmall,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    item.score?.let {
                                        Text("★ ${it / 10.0}", style = MaterialTheme.typography.bodySmall)
                                    }
                                    Text(
                                        item.genres.joinToString(", "),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    TextButton(onClick = { navigator.push(GlobalSearchScreen(item.title)) }) {
                                        Text(stringResource(KMR.strings.mod69_suggestions_find))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private suspend fun load(excluded: Set<String>): Pair<List<String>, List<Mod69Suggestion>> = withIOContext {
        val library = Injekt.get<GetLibraryManga>().await().distinctBy { it.id }
        val owned = library.map { normalize(it.manga.title) }.toSet()
        val weight = mutableMapOf<String, Long>()
        library.forEach { item ->
            val w = 1 + item.readCount
            item.manga.genre.orEmpty()
                .map { it.trim().replaceFirstChar(Char::uppercase) }
                .filter { it.isNotEmpty() }
                .forEach { weight.merge(it, w, Long::plus) }
        }
        val topTags = weight.entries.sortedByDescending { it.value }.take(12).map { it.key }
        val wanted = anilistGenres.filter { g -> topTags.any { it.equals(g, ignoreCase = true) } && g !in excluded }
            .take(3)
        val query = """
            query (${'$'}genres: [String], ${'$'}excluded: [String]) {
              Page(perPage: 50) {
                media(type: MANGA, sort: POPULARITY_DESC, genre_in: ${'$'}genres, genre_not_in: ${'$'}excluded, isAdult: false) {
                  title { romaji english }
                  coverImage { large }
                  genres
                  averageScore
                }
              }
            }
        """.trimIndent()
        val payload = buildJsonObject {
            put("query", query)
            putJsonObject("variables") {
                putJsonArray("genres") { (wanted.ifEmpty { listOf("Action") }).forEach { add(kotlinx.serialization.json.JsonPrimitive(it)) } }
                putJsonArray("excluded") { excluded.filter { it in anilistGenres }.forEach { add(kotlinx.serialization.json.JsonPrimitive(it)) } }
            }
        }
        val request = Request.Builder()
            .url("https://graphql.anilist.co/")
            .post(payload.toString().toRequestBody("application/json".toMediaType()))
            .header("Accept", "application/json")
            .build()
        val text = Injekt.get<NetworkHelper>().client.newCall(request).awaitSuccess().use { it.body.string() }
        val media = Json.parseToJsonElement(text).jsonObject["data"]!!.jsonObject["Page"]!!.jsonObject["media"]!!.jsonArray
        val list = media.mapNotNull { element ->
            val obj = element.jsonObject
            val titles = obj["title"]?.jsonObject
            val title = titles?.get("english")?.jsonPrimitive?.content?.takeIf { it != "null" }
                ?: titles?.get("romaji")?.jsonPrimitive?.content
                ?: return@mapNotNull null
            if (normalize(title) in owned) return@mapNotNull null
            Mod69Suggestion(
                title = title,
                cover = obj["coverImage"]?.jsonObject?.get("large")?.jsonPrimitive?.content,
                genres = obj["genres"]?.jsonArray?.map { it.jsonPrimitive.content }.orEmpty(),
                score = obj["averageScore"]?.jsonPrimitive?.intOrNull,
            )
        }
        topTags to list
    }

    private fun normalize(title: String) = title.lowercase().filter { it.isLetterOrDigit() }

    private val anilistGenres = listOf(
        "Action", "Adventure", "Comedy", "Drama", "Ecchi", "Fantasy", "Horror", "Mahou Shoujo", "Mecha",
        "Music", "Mystery", "Psychological", "Romance", "Sci-Fi", "Slice of Life", "Sports", "Supernatural",
        "Thriller",
    )
}
// KMK <--
