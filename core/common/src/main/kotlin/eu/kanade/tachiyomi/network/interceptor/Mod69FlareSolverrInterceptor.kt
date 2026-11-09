package eu.kanade.tachiyomi.network.interceptor

import eu.kanade.tachiyomi.network.AndroidCookieJar
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.add
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import okhttp3.Cookie
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

// KMK -->
/**
 * 69 mod B2: when a site answers with a Cloudflare check, ask a FlareSolverr server (that the user runs)
 * to pass it, keep its cookies and browser name, and try again. Without a server set it does nothing,
 * and the app's normal WebView check still runs after it when this fails.
 */
class Mod69FlareSolverrInterceptor(
    private val serverUrl: () -> String,
    private val cookieJar: AndroidCookieJar,
) : Interceptor {

    private val solverClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(90, TimeUnit.SECONDS)
            .callTimeout(100, TimeUnit.SECONDS)
            .build()
    }

    /** Browser name FlareSolverr used per site; Cloudflare cookies only work with the same one. */
    private val userAgents = ConcurrentHashMap<String, String>()

    override fun intercept(chain: Interceptor.Chain): Response {
        val server = serverUrl().trim().trimEnd('/')
        var request = chain.request()
        if (server.isEmpty()) return chain.proceed(request)

        userAgents[request.url.host]?.let { request = request.newBuilder().header("User-Agent", it).build() }
        val response = chain.proceed(request)
        if (response.code !in listOf(403, 503) || response.header("Server") !in listOf("cloudflare-nginx", "cloudflare")) {
            return response
        }

        val solved = runCatching { solve(server, request.url.toString()) }.getOrNull() ?: return response
        response.close()
        if (solved.second.isNotEmpty()) cookieJar.saveFromResponse(request.url, solved.second)
        solved.first?.let { userAgents[request.url.host] = it }
        val retry = request.newBuilder()
            .apply { solved.first?.let { header("User-Agent", it) } }
            .build()
        return chain.proceed(retry)
    }

    private fun solve(server: String, url: String): Pair<String?, List<Cookie>>? {
        val body = buildJsonObject {
            put("cmd", "request.get")
            put("url", url)
            put("maxTimeout", 60000)
        }.toString().toRequestBody("application/json".toMediaType())
        val endpoint = if (server.endsWith("/v1")) server else "$server/v1"
        val text = solverClient.newCall(Request.Builder().url(endpoint).post(body).build())
            .execute()
            .use { if (it.isSuccessful) it.body.string() else null }
            ?: return null
        val root = Json.parseToJsonElement(text).jsonObject
        if (root["status"]?.jsonPrimitive?.content != "ok") return null
        val solution = root["solution"]?.jsonObject ?: return null
        val userAgent = solution["userAgent"]?.jsonPrimitive?.content
        val cookies = solution["cookies"]?.jsonArray.orEmpty().mapNotNull { element ->
            val c = element as? JsonObject ?: return@mapNotNull null
            val name = c["name"]?.jsonPrimitive?.content ?: return@mapNotNull null
            val value = c["value"]?.jsonPrimitive?.content ?: return@mapNotNull null
            val domain = c["domain"]?.jsonPrimitive?.content?.trimStart('.') ?: return@mapNotNull null
            Cookie.Builder()
                .name(name)
                .value(value)
                .domain(domain)
                .path(c["path"]?.jsonPrimitive?.content ?: "/")
                .apply {
                    c["expires"]?.jsonPrimitive?.doubleOrNull?.takeIf { it > 0 }?.let { expiresAt((it * 1000).toLong()) }
                    if (c["secure"]?.jsonPrimitive?.booleanOrNull == true) secure()
                    if (c["httpOnly"]?.jsonPrimitive?.booleanOrNull == true) httpOnly()
                }
                .build()
        }
        return userAgent to cookies
    }
}
// KMK <--
