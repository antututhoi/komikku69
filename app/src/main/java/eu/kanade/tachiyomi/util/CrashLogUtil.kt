package eu.kanade.tachiyomi.util

import android.content.Context
import android.os.Build
import eu.kanade.domain.base.BasePreferences
import eu.kanade.tachiyomi.BuildConfig
import eu.kanade.tachiyomi.extension.ExtensionManager
import eu.kanade.tachiyomi.util.storage.getUriCompat
import eu.kanade.tachiyomi.util.system.WebViewUtil
import eu.kanade.tachiyomi.util.system.createFileInCacheDir
import eu.kanade.tachiyomi.util.system.toShareIntent
import eu.kanade.tachiyomi.util.system.toast
import tachiyomi.core.common.util.lang.withNonCancellableContext
import tachiyomi.core.common.util.lang.withUIContext
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.time.OffsetDateTime
import java.time.ZoneId

class CrashLogUtil(
    private val context: Context,
    private val extensionManager: ExtensionManager = Injekt.get(),
    private val preferences: BasePreferences = Injekt.get(),
) {

    suspend fun dumpLogs(exception: Throwable? = null) = withNonCancellableContext {
        try {
            val file = context.createFileInCacheDir("komikku_crash_logs.txt")

            file.appendText(getDebugInfo() + "\n\n")
            getExtensionsInfo()?.let { file.appendText("$it\n\n") }
            exception?.let { file.appendText("$it\n\n") }

            Runtime.getRuntime().exec("logcat *:E -d -v year -v zone -f ${file.absolutePath}").waitFor()

            // KMK --> 69 mod X6: remove login tokens, cookies, passwords and email addresses
            file.writeText(mod69Redact(file.readText()))
            // KMK <--

            val uri = file.getUriCompat(context)
            context.startActivity(uri.toShareIntent(context, "text/plain"))
        } catch (e: Throwable) {
            withUIContext { context.toast("Failed to get logs") }
        }
    }

    // KMK -->
    private fun mod69Redact(text: String): String {
        var result = text
        listOf(
            Regex("(?i)(authorization:?\\s*(?:bearer|basic)?\\s*)[A-Za-z0-9._~+/=-]{8,}") to "$1<hidden>",
            Regex("(?i)(access_token|refresh_token|token|api_key|apikey|password|passwd|secret|session|sessionid|code)(=|\":\\s*\")[^&\\s\"']{4,}") to "$1$2<hidden>",
            Regex("(?i)((?:set-)?cookie:?\\s*)[^\\n]+") to "$1<hidden>",
            Regex("(?i)([?&]key=)[^&\\s]+") to "$1<hidden>",
            Regex("[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}") to "<email hidden>",
        ).forEach { (regex, replacement) -> result = regex.replace(result, replacement) }
        return result
    }
    // KMK <--

    fun getDebugInfo(): String {
        return """
            App ID: ${BuildConfig.APPLICATION_ID}
            App version: ${BuildConfig.VERSION_NAME} (${BuildConfig.COMMIT_SHA}, ${BuildConfig.VERSION_CODE}, ${BuildConfig.BUILD_TIME})
            Build version: ${BuildConfig.COMMIT_COUNT}
            Installation ID: ${preferences.installationId().get()}
            Android version: ${Build.VERSION.RELEASE} (SDK ${Build.VERSION.SDK_INT}; build ${Build.DISPLAY})
            Device brand: ${Build.BRAND}
            Device manufacturer: ${Build.MANUFACTURER}
            Device name: ${Build.DEVICE} (${Build.PRODUCT})
            Device model: ${Build.MODEL}
            WebView: ${WebViewUtil.getVersion(context)}
            Current time: ${OffsetDateTime.now(ZoneId.systemDefault())}
        """.trimIndent()
    }

    private fun getExtensionsInfo(): String? {
        val availableExtensions = extensionManager.availableExtensionsFlow.value.associateBy { it.pkgName }

        val extensionInfoList = extensionManager.installedExtensionsFlow.value
            .sortedBy { it.name }
            .mapNotNull {
                val availableExtension = availableExtensions[it.pkgName]
                val hasUpdate = (availableExtension?.versionCode ?: 0) > it.versionCode

                if (!hasUpdate && !it.isObsolete) return@mapNotNull null

                """
                    - ${it.name}
                      Installed: ${it.versionName} / Available: ${availableExtension?.versionName ?: "?"}
                      Orphaned: ${it.isObsolete}
                """.trimIndent()
            }

        return if (extensionInfoList.isNotEmpty()) {
            (listOf("Problematic extensions:") + extensionInfoList)
                .joinToString("\n")
        } else {
            null
        }
    }
}
