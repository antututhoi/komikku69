package eu.kanade.presentation.webview

import android.webkit.WebResourceResponse
import java.io.ByteArrayInputStream

// KMK -->
/** 69 mod B3: simple ad blocker and desktop mode for the built-in browser. */
object Mod69Web {
    const val DESKTOP_UA =
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36"

    private val adHosts = setOf(
        "doubleclick.net", "googlesyndication.com", "googleadservices.com", "adservice.google.com",
        "google-analytics.com", "googletagmanager.com", "googletagservices.com", "adnxs.com",
        "popads.net", "popcash.net", "propellerads.com", "propellerclick.com", "exoclick.com",
        "exosrv.com", "juicyads.com", "trafficjunky.net", "adsterra.com", "a-ads.com",
        "hilltopads.net", "mgid.com", "taboola.com", "outbrain.com", "onclickads.net",
        "ad-maven.com", "adcash.com", "revcontent.com", "pubmatic.com", "rubiconproject.com",
        "criteo.com", "criteo.net", "amazon-adsystem.com", "scorecardresearch.com", "zedo.com",
        "yllix.com", "clickadu.com", "highperformanceformat.com", "profitablegatecpm.com",
        "effectivegatecpm.com", "realsrv.com", "tsyndicate.com", "adskeeper.com", "bidgear.com",
        "dtscout.com", "histats.com", "yandex.ru/ads", "an.yandex.ru", "mc.yandex.ru",
        "popunder.net", "pushwhy.com", "richpush.co", "adspyglass.com", "displayvertising.com",
    )

    fun isAd(host: String?): Boolean {
        val h = host?.lowercase() ?: return false
        return adHosts.any { h == it || h.endsWith(".$it") }
    }

    fun empty(): WebResourceResponse = WebResourceResponse("text/plain", "utf-8", ByteArrayInputStream(ByteArray(0)))
}
// KMK <--
