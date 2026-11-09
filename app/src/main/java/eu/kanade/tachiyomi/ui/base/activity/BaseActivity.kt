package eu.kanade.tachiyomi.ui.base.activity

import android.content.Context
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import eu.kanade.tachiyomi.ui.base.delegate.SecureActivityDelegate
import eu.kanade.tachiyomi.ui.base.delegate.SecureActivityDelegateImpl
import eu.kanade.tachiyomi.ui.base.delegate.ThemingDelegate
import eu.kanade.tachiyomi.ui.base.delegate.ThemingDelegateImpl
import eu.kanade.tachiyomi.util.system.prepareTabletUiContext
import uy.kohesive.injekt.api.get

open class BaseActivity :
    AppCompatActivity(),
    SecureActivityDelegate by SecureActivityDelegateImpl(),
    ThemingDelegate by ThemingDelegateImpl() {

    override fun attachBaseContext(newBase: Context) {
        // KMK --> 69 mod U3: display size
        val context = newBase.prepareTabletUiContext()
        val scale = runCatching {
            uy.kohesive.injekt.Injekt.get<eu.kanade.domain.ui.UiPreferences>().mod69DisplaySize().get()
        }.getOrDefault(100)
        super.attachBaseContext(if (scale in 60..150 && scale != 100) context.mod69Scaled(scale) else context)
        // KMK <--
    }

    // KMK -->
    private fun Context.mod69Scaled(percent: Int): Context {
        val configuration = android.content.res.Configuration(resources.configuration)
        configuration.densityDpi = configuration.densityDpi * percent / 100
        configuration.screenWidthDp = configuration.screenWidthDp * 100 / percent
        configuration.screenHeightDp = configuration.screenHeightDp * 100 / percent
        configuration.smallestScreenWidthDp = configuration.smallestScreenWidthDp * 100 / percent
        return createConfigurationContext(configuration)
    }
    // KMK <--

    override fun onCreate(savedInstanceState: Bundle?) {
        applyAppTheme(this)
        super.onCreate(savedInstanceState)
    }
}
