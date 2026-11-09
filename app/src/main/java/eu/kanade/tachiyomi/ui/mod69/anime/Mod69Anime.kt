package eu.kanade.tachiyomi.ui.mod69.anime

import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.core.content.edit
import eu.kanade.tachiyomi.animesource.AnimeCatalogueSource
import eu.kanade.tachiyomi.animesource.AnimeSource
import eu.kanade.tachiyomi.animesource.AnimeSourceFactory
import eu.kanade.tachiyomi.animesource.model.SAnime
import eu.kanade.tachiyomi.animesource.model.SEpisode
import eu.kanade.tachiyomi.animesource.model.Video
import eu.kanade.tachiyomi.animesource.online.AnimeHttpSource
import eu.kanade.tachiyomi.util.system.ChildFirstPathClassLoader
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import tachiyomi.core.common.util.lang.withIOContext

// KMK -->
/** 69 mod O2: anime from Aniyomi anime extensions installed on the phone; videos play in your video app. */
object Mod69Anime {
    private const val FEATURE = "tachiyomi.animeextension"
    private const val META_CLASS = "tachiyomi.animeextension.class"
    private val LIB_VERSIONS = listOf(12.0, 13.0, 14.0, 15.0, 16.0, 17.0)

    data class Extension(val name: String, val pkg: String, val version: String, val sources: List<AnimeSource>)

    @Volatile
    private var cache: List<Extension>? = null

    fun sources(): List<AnimeCatalogueSource> =
        cache.orEmpty().flatMap { it.sources }.filterIsInstance<AnimeCatalogueSource>()

    fun source(id: Long): AnimeCatalogueSource? = sources().firstOrNull { it.id == id }

    @Suppress("DEPRECATION")
    suspend fun load(context: Context, force: Boolean = false): List<Extension> = withIOContext {
        cache?.takeIf { !force }?.let { return@withIOContext it }
        val flags = PackageManager.GET_CONFIGURATIONS or PackageManager.GET_META_DATA or
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) PackageManager.GET_SIGNING_CERTIFICATES else 0
        val pm = context.packageManager
        val packages: List<PackageInfo> = pm.getInstalledPackages(flags)
        val loaded = packages
            .filter { info -> info.reqFeatures.orEmpty().any { it.name == FEATURE } }
            .mapNotNull { info -> runCatching { loadOne(context, info) }.getOrNull() }
        cache = loaded
        loaded
    }

    private fun loadOne(context: Context, info: PackageInfo): Extension? {
        val app = info.applicationInfo ?: return null
        val meta = app.metaData ?: return null
        val version = info.versionName ?: return null
        val lib = version.substringBeforeLast('.').toDoubleOrNull() ?: return null
        if (lib !in LIB_VERSIONS) return null
        val loader = ChildFirstPathClassLoader(app.sourceDir, null, context.classLoader)
        val sources = meta.getString(META_CLASS).orEmpty().split(";").map { it.trim() }.filter { it.isNotEmpty() }
            .map { if (it.startsWith(".")) info.packageName + it else it }
            .flatMap { name ->
                when (val obj = Class.forName(name, false, loader).getDeclaredConstructor().newInstance()) {
                    is AnimeSource -> listOf(obj)
                    is AnimeSourceFactory -> obj.createSources()
                    else -> emptyList()
                }
            }
        val label = context.packageManager.getApplicationLabel(app).toString().substringAfter("Aniyomi: ")
        return Extension(label, info.packageName, version, sources)
    }

    /** All playable videos of an episode (from its hosters when the extension uses them). */
    suspend fun videos(source: AnimeSource, episode: SEpisode): List<Video> = withIOContext {
        val fromHosters = runCatching {
            source.getHosterList(episode).flatMap { hoster ->
                hoster.videoList ?: runCatching { source.getVideoList(hoster) }.getOrDefault(emptyList())
            }
        }.getOrDefault(emptyList())
        val list = fromHosters.ifEmpty { runCatching { source.getVideoList(episode) }.getOrDefault(emptyList()) }
        list.mapNotNull { video ->
            if (source is AnimeHttpSource) {
                runCatching { source.resolveVideo(video) }.getOrNull() ?: video
            } else {
                video
            }
        }.filter { it.videoUrl.isNotBlank() }
    }

    /** Opens the video in a video app (69-NextPlayer, MX Player, VLC…), with its web headers and subtitles. */
    fun play(context: Context, video: Video, title: String) {
        val headers = video.headers?.let { h -> h.names().flatMap { listOf(it, h[it].orEmpty()) }.toTypedArray() }
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(Uri.parse(video.videoUrl), "video/*")
            putExtra("title", title)
            if (headers != null) putExtra("headers", headers)
            val subs = video.subtitleTracks.map { Uri.parse(it.url) }
            if (subs.isNotEmpty()) {
                putExtra("subs", subs.toTypedArray())
                putExtra("subs.name", video.subtitleTracks.map { it.lang }.toTypedArray())
            }
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(Intent.createChooser(intent, title).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    // ---------- my anime list, kept on the phone ----------

    @Serializable
    data class Saved(val sourceId: Long, val url: String, val title: String, val thumbnail: String?)

    private fun prefs(context: Context) = context.getSharedPreferences("mod69_anime", Context.MODE_PRIVATE)

    fun saved(context: Context): List<Saved> = prefs(context).getStringSet("saved", emptySet()).orEmpty()
        .mapNotNull { runCatching { Json.decodeFromString<Saved>(it) }.getOrNull() }
        .sortedBy { it.title.lowercase() }

    fun isSaved(context: Context, sourceId: Long, url: String) = saved(context).any { it.sourceId == sourceId && it.url == url }

    fun toggleSaved(context: Context, sourceId: Long, anime: SAnime) {
        val p = prefs(context)
        val set = p.getStringSet("saved", emptySet()).orEmpty().toMutableSet()
        val existing = set.firstOrNull { runCatching { Json.decodeFromString<Saved>(it) }.getOrNull()?.let { s -> s.sourceId == sourceId && s.url == anime.url } == true }
        if (existing != null) set -= existing else set += Json.encodeToString(Saved(sourceId, anime.url, anime.title, anime.thumbnail_url))
        p.edit { putStringSet("saved", set) }
    }

    fun isWatched(context: Context, sourceId: Long, episodeUrl: String) =
        prefs(context).getBoolean("w_${sourceId}_$episodeUrl", false)

    fun setWatched(context: Context, sourceId: Long, episodeUrl: String, watched: Boolean) =
        prefs(context).edit { putBoolean("w_${sourceId}_$episodeUrl", watched) }
}
// KMK <--
