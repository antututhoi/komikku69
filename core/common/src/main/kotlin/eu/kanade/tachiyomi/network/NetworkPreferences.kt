package eu.kanade.tachiyomi.network

import tachiyomi.core.common.preference.Preference
import tachiyomi.core.common.preference.PreferenceStore

class NetworkPreferences(
    private val preferenceStore: PreferenceStore,
) {

    /* KMK --> fun verboseLogging(): Preference<Boolean> {
        return preferenceStore.getBoolean("verbose_logging", verboseLogging)
    } KMK <-- */

    fun dohProvider(): Preference<Int> {
        return preferenceStore.getInt("doh_provider", -1)
    }

    // KMK --> 69 mod
    /** B7: address put in front of GitHub addresses, e.g. "https://ghproxy.net/". Empty = off. */
    fun mod69GithubMirror(): Preference<String> = preferenceStore.getString("mod69_github_mirror", "")

    /** B11: picture proxy address with {url} in it. Empty = off. */
    fun mod69ImageProxy(): Preference<String> = preferenceStore.getString("mod69_image_proxy", "")

    /** B3: block common ad / tracking hosts in the built-in browser. */
    fun mod69WebAdBlock(): Preference<Boolean> = preferenceStore.getBoolean("mod69_web_ad_block", true)
    // KMK <--

    fun defaultUserAgent(): Preference<String> {
        return preferenceStore.getString(
            "default_user_agent",
            "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/141.0.0.0 Mobile Safari/537.36",
        )
    }
}
