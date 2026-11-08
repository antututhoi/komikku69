package eu.kanade.tachiyomi.network.interceptor

import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Interceptor
import okhttp3.Response
import java.net.URLEncoder

// KMK -->
/**
 * 69 mod B7 / B11:
 * - GitHub mirror: addresses of GitHub (extension lists, downloads, updates) get the mirror's address put in front.
 * - Picture proxy: picture addresses are sent through a compressing proxy (the "{url}" in its address is replaced
 *   by the real address).
 */
class Mod69UrlInterceptor(
    private val githubMirror: () -> String,
    private val imageProxy: () -> String,
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        var request = chain.request()
        val url = request.url

        val mirror = githubMirror().trim()
        if (mirror.isNotEmpty() && url.host in GITHUB_HOSTS && !url.toString().startsWith(mirror)) {
            mirror.trimEnd('/').plus("/").plus(url).toHttpUrlOrNull()?.let {
                request = request.newBuilder().url(it).build()
            }
        } else {
            val proxy = imageProxy().trim()
            val path = url.encodedPath.lowercase()
            if (proxy.contains("{url}") && IMAGE_EXTENSIONS.any { path.endsWith(it) } && !url.toString().startsWith(proxy.substringBefore("{url}"))) {
                proxy.replace("{url}", URLEncoder.encode(url.toString(), "UTF-8")).toHttpUrlOrNull()?.let {
                    request = request.newBuilder().url(it).build()
                }
            }
        }
        return chain.proceed(request)
    }

    private companion object {
        val GITHUB_HOSTS = setOf(
            "raw.githubusercontent.com",
            "github.com",
            "gist.githubusercontent.com",
            "objects.githubusercontent.com",
        )
        val IMAGE_EXTENSIONS = listOf(".jpg", ".jpeg", ".png", ".webp", ".gif", ".avif")
    }
}
// KMK <--
