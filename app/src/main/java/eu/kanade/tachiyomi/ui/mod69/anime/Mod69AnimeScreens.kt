package eu.kanade.tachiyomi.ui.mod69.anime

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import coil3.network.NetworkHeaders
import coil3.network.httpHeaders
import coil3.request.ImageRequest
import eu.kanade.presentation.components.AppBar
import eu.kanade.presentation.util.Screen
import eu.kanade.tachiyomi.animesource.model.SAnime
import eu.kanade.tachiyomi.animesource.model.SEpisode
import eu.kanade.tachiyomi.animesource.model.Video
import eu.kanade.tachiyomi.animesource.online.AnimeHttpSource
import eu.kanade.tachiyomi.util.system.toast
import kotlinx.coroutines.launch
import tachiyomi.core.common.util.lang.withIOContext
import tachiyomi.i18n.kmk.KMR
import tachiyomi.presentation.core.components.material.Scaffold
import tachiyomi.presentation.core.i18n.stringResource

// KMK -->
@Composable
private fun AnimeCover(url: String?, sourceId: Long, modifier: Modifier) {
    val context = LocalContext.current
    val source = remember(sourceId) { Mod69Anime.source(sourceId) as? AnimeHttpSource }
    AsyncImage(
        model = ImageRequest.Builder(context)
            .data(url)
            .apply {
                source?.headers?.let { h ->
                    httpHeaders(NetworkHeaders.Builder().apply { h.names().forEach { set(it, h[it].orEmpty()) } }.build())
                }
            }
            .build(),
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = modifier,
    )
}

/** Start screen: my saved anime and the anime sources. */
class Mod69AnimeHomeScreen : Screen() {

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val context = LocalContext.current
        var tab by remember { mutableIntStateOf(0) }
        var loading by remember { mutableStateOf(true) }
        var reload by remember { mutableIntStateOf(0) }
        var extensions by remember { mutableStateOf<List<Mod69Anime.Extension>>(emptyList()) }
        LaunchedEffect(reload) {
            loading = true
            extensions = runCatching { Mod69Anime.load(context, force = reload > 0) }.getOrDefault(emptyList())
            loading = false
        }
        Scaffold(
            topBar = { scrollBehavior ->
                AppBar(
                    title = stringResource(KMR.strings.mod69_anime),
                    navigateUp = navigator::pop,
                    actions = {
                        IconButton(onClick = { reload++ }) { Icon(Icons.Outlined.Refresh, contentDescription = null) }
                    },
                    scrollBehavior = scrollBehavior,
                )
            },
        ) { padding ->
            Column(Modifier.padding(padding).fillMaxSize()) {
                Row(Modifier.padding(horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = tab == 0, onClick = { tab = 0 }, label = { Text(stringResource(KMR.strings.mod69_anime_my)) })
                    FilterChip(selected = tab == 1, onClick = { tab = 1 }, label = { Text(stringResource(KMR.strings.mod69_anime_sources)) })
                }
                if (loading) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                    return@Column
                }
                if (tab == 0) {
                    val saved = remember(reload, tab) { Mod69Anime.saved(context) }
                    if (saved.isEmpty()) {
                        Text(stringResource(KMR.strings.mod69_anime_my_empty), modifier = Modifier.padding(16.dp))
                    }
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(110.dp),
                        contentPadding = PaddingValues(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        items(saved, key = { "${it.sourceId}${it.url}" }) { item ->
                            Column(
                                Modifier.clickable {
                                    val anime = SAnime.create().apply {
                                        url = item.url
                                        title = item.title
                                        thumbnail_url = item.thumbnail
                                    }
                                    navigator.push(Mod69AnimeDetailsScreen(item.sourceId, anime))
                                },
                            ) {
                                AnimeCover(item.thumbnail, item.sourceId, Modifier.fillMaxWidth().aspectRatio(0.7f).clip(RoundedCornerShape(6.dp)))
                                Text(item.title, maxLines = 2, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }
                } else {
                    val sources = Mod69Anime.sources()
                    if (sources.isEmpty()) {
                        Text(stringResource(KMR.strings.mod69_anime_no_ext), modifier = Modifier.padding(16.dp))
                    }
                    LazyColumn {
                        items(sources, key = { it.id }) { source ->
                            Text(
                                "${source.name} (${source.lang})",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { navigator.push(Mod69AnimeBrowseScreen(source.id)) }
                                    .padding(16.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

/** Popular / latest / search of one anime source. */
class Mod69AnimeBrowseScreen(private val sourceId: Long) : Screen() {

    override val key = "mod69-anime-browse-$sourceId"

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val source = remember { Mod69Anime.source(sourceId) }
        var query by remember { mutableStateOf("") }
        var mode by remember { mutableStateOf("popular") }
        var page by remember { mutableIntStateOf(1) }
        var items by remember { mutableStateOf<List<SAnime>>(emptyList()) }
        var hasNext by remember { mutableStateOf(false) }
        var loading by remember { mutableStateOf(false) }
        var error by remember { mutableStateOf<String?>(null) }
        var searchText by remember { mutableStateOf("") }
        LaunchedEffect(mode, query, page) {
            val s = source ?: return@LaunchedEffect
            loading = true
            error = null
            runCatching {
                withIOContext {
                    when {
                        query.isNotBlank() -> s.getSearchAnime(page, query, s.getFilterList())
                        mode == "latest" && s.supportsLatest -> s.getLatestUpdates(page)
                        else -> s.getPopularAnime(page)
                    }
                }
            }.onSuccess {
                items = if (page == 1) it.animes else items + it.animes
                hasNext = it.hasNextPage
            }.onFailure { error = it.message ?: it.toString() }
            loading = false
        }
        Scaffold(
            topBar = { scrollBehavior ->
                AppBar(title = source?.name ?: "?", navigateUp = navigator::pop, scrollBehavior = scrollBehavior)
            },
        ) { padding ->
            Column(Modifier.padding(padding).fillMaxSize()) {
                OutlinedTextField(
                    value = searchText,
                    onValueChange = { searchText = it },
                    singleLine = true,
                    label = { Text(stringResource(KMR.strings.mod69_anime_search)) },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
                    trailingIcon = {
                        TextButton(onClick = {
                            page = 1
                            query = searchText.trim()
                        }) { Text("OK") }
                    },
                )
                Row(Modifier.padding(horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = query.isBlank() && mode == "popular", onClick = {
                        query = ""
                        searchText = ""
                        page = 1
                        mode = "popular"
                    }, label = { Text("Popular") })
                    if (source?.supportsLatest == true) {
                        FilterChip(selected = query.isBlank() && mode == "latest", onClick = {
                            query = ""
                            searchText = ""
                            page = 1
                            mode = "latest"
                        }, label = { Text("Latest") })
                    }
                }
                error?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(12.dp)) }
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(110.dp),
                    contentPadding = PaddingValues(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    items(items, key = { it.url }) { anime ->
                        Column(Modifier.clickable { navigator.push(Mod69AnimeDetailsScreen(sourceId, anime)) }) {
                            AnimeCover(anime.thumbnail_url, sourceId, Modifier.fillMaxWidth().aspectRatio(0.7f).clip(RoundedCornerShape(6.dp)))
                            Text(anime.title, maxLines = 2, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.labelMedium)
                        }
                    }
                    if (hasNext && !loading) {
                        item {
                            TextButton(onClick = { page++ }) { Text(stringResource(KMR.strings.mod69_anime_more)) }
                        }
                    }
                }
                if (loading) Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            }
        }
    }
}

/** Details and episodes of one anime. Tap an episode to pick a video and play it. */
class Mod69AnimeDetailsScreen(private val sourceId: Long, private val start: SAnime) : Screen() {

    override val key = "mod69-anime-$sourceId-${start.url}"

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val context = LocalContext.current
        val scope = rememberCoroutineScope()
        val source = remember { Mod69Anime.source(sourceId) }
        var anime by remember { mutableStateOf(start) }
        var episodes by remember { mutableStateOf<List<SEpisode>?>(null) }
        var error by remember { mutableStateOf<String?>(null) }
        var saved by remember { mutableStateOf(Mod69Anime.isSaved(context, sourceId, start.url)) }
        var videos by remember { mutableStateOf<Pair<SEpisode, List<Video>>?>(null) }
        var loadingVideos by remember { mutableStateOf(false) }
        var watchedTick by remember { mutableIntStateOf(0) }
        LaunchedEffect(Unit) {
            val s = source ?: run {
                error = context.getString(android.R.string.unknownName)
                episodes = emptyList()
                return@LaunchedEffect
            }
            runCatching { withIOContext { s.getAnimeDetails(start) } }.onSuccess { details ->
                details.url = start.url
                if (details.title.isBlank()) details.title = start.title
                if (details.thumbnail_url.isNullOrBlank()) details.thumbnail_url = start.thumbnail_url
                anime = details
            }
            runCatching { withIOContext { s.getEpisodeList(start) } }
                .onSuccess { episodes = it }
                .onFailure {
                    error = it.message
                    episodes = emptyList()
                }
        }
        Scaffold(
            topBar = { scrollBehavior ->
                AppBar(
                    title = anime.title,
                    navigateUp = navigator::pop,
                    actions = {
                        IconButton(onClick = {
                            Mod69Anime.toggleSaved(context, sourceId, anime)
                            saved = !saved
                        }) {
                            Icon(if (saved) Icons.Outlined.Favorite else Icons.Outlined.FavoriteBorder, contentDescription = null)
                        }
                    },
                    scrollBehavior = scrollBehavior,
                )
            },
        ) { padding ->
            LazyColumn(contentPadding = padding) {
                item {
                    Row(Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        AnimeCover(anime.thumbnail_url, sourceId, Modifier.width(110.dp).height(160.dp).clip(RoundedCornerShape(8.dp)))
                        Column(Modifier.weight(1f)) {
                            Text(anime.title, style = MaterialTheme.typography.titleMedium)
                            anime.genre?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
                            Text(source?.name.orEmpty(), style = MaterialTheme.typography.labelMedium)
                        }
                    }
                    anime.description?.let {
                        Text(it, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(horizontal = 12.dp))
                    }
                    error?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(12.dp)) }
                    if (episodes == null || loadingVideos) {
                        Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                    }
                }
                items(episodes.orEmpty(), key = { it.url }) { episode ->
                    val watched = remember(watchedTick) { Mod69Anime.isWatched(context, sourceId, episode.url) }
                    Text(
                        text = episode.name,
                        color = if (watched) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f) else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                val s = source ?: return@clickable
                                loadingVideos = true
                                scope.launch {
                                    val list = runCatching { Mod69Anime.videos(s, episode) }.getOrDefault(emptyList())
                                    loadingVideos = false
                                    if (list.isEmpty()) {
                                        context.toast(KMR.strings.mod69_anime_no_video)
                                    } else {
                                        videos = episode to list
                                    }
                                }
                            }
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                    )
                }
            }
        }
        videos?.let { (episode, list) ->
            AlertDialog(
                onDismissRequest = { videos = null },
                title = { Text(episode.name) },
                text = {
                    LazyColumn {
                        items(list) { video ->
                            Text(
                                video.videoTitle.ifBlank { video.videoUrl.take(50) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        videos = null
                                        Mod69Anime.setWatched(context, sourceId, episode.url, true)
                                        watchedTick++
                                        runCatching {
                                            Mod69Anime.play(context, video, "${anime.title} - ${episode.name}")
                                        }.onFailure { context.toast(KMR.strings.mod69_anime_no_player) }
                                    }
                                    .padding(vertical = 10.dp),
                            )
                        }
                    }
                },
                confirmButton = { TextButton(onClick = { videos = null }) { Text(stringResource(tachiyomi.i18n.MR.strings.action_cancel)) } },
            )
        }
    }
}
// KMK <--
