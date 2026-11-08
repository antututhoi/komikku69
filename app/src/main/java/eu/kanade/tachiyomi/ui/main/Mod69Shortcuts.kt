package eu.kanade.tachiyomi.ui.main

import android.content.Context
import android.content.Intent
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import eu.kanade.tachiyomi.R
import kotlinx.coroutines.flow.first
import tachiyomi.core.common.Constants
import tachiyomi.domain.history.interactor.GetHistory
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

// KMK -->
/** 69 mod S4: long-press the app icon to jump to the entries you read last. */
object Mod69Shortcuts {
    suspend fun refresh(context: Context) {
        try {
            val recent = Injekt.get<GetHistory>()
                .subscribe("", null, null, null)
                .first()
                .distinctBy { it.mangaId }
                .take(3)
            val shortcuts = recent.map { item ->
                ShortcutInfoCompat.Builder(context, "mod69_manga_${item.mangaId}")
                    .setShortLabel(item.title.take(24))
                    .setLongLabel(item.title)
                    .setIcon(IconCompat.createWithResource(context, R.mipmap.ic_launcher))
                    .setIntent(
                        Intent(context, MainActivity::class.java)
                            .setAction(Constants.SHORTCUT_MANGA)
                            .putExtra(Constants.MANGA_EXTRA, item.mangaId),
                    )
                    .build()
            }
            ShortcutManagerCompat.setDynamicShortcuts(context, shortcuts)
        } catch (_: Throwable) {
            // shortcuts are a nice extra; never crash for them
        }
    }
}
// KMK <--
