package eu.kanade.tachiyomi.ui.main

import android.content.Context
import android.content.Intent
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import coil3.imageLoader
import eu.kanade.tachiyomi.R
import kotlinx.coroutines.flow.first
import tachiyomi.core.common.Constants
import tachiyomi.domain.history.interactor.GetHistory
import tachiyomi.domain.manga.model.asMangaCover
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

    /** S4: puts an icon for one entry on the home screen (uses its cover when it can). */
    suspend fun pinManga(context: Context, mangaId: Long, title: String): Boolean {
        if (!ShortcutManagerCompat.isRequestPinShortcutSupported(context)) return false
        val icon = try {
            val manga = Injekt.get<tachiyomi.domain.manga.interactor.GetManga>().await(mangaId)
            val request = coil3.request.ImageRequest.Builder(context)
                .data(manga?.asMangaCover())
                .size(192)
                .build()
            val image = context.imageLoader.execute(request).image
            (image as? coil3.BitmapImage)?.bitmap?.let { IconCompat.createWithAdaptiveBitmap(squared(it)) }
        } catch (_: Throwable) {
            null
        } ?: IconCompat.createWithResource(context, R.mipmap.ic_launcher)
        val shortcut = ShortcutInfoCompat.Builder(context, "mod69_pin_$mangaId")
            .setShortLabel(title.take(24).ifBlank { "69" })
            .setLongLabel(title.ifBlank { "69" })
            .setIcon(icon)
            .setIntent(
                Intent(context, MainActivity::class.java)
                    .setAction(Constants.SHORTCUT_MANGA)
                    .putExtra(Constants.MANGA_EXTRA, mangaId),
            )
            .build()
        return ShortcutManagerCompat.requestPinShortcut(context, shortcut, null)
    }

    private fun squared(source: android.graphics.Bitmap): android.graphics.Bitmap {
        val side = minOf(source.width, source.height)
        return android.graphics.Bitmap.createBitmap(
            source,
            (source.width - side) / 2,
            (source.height - side) / 4,
            side,
            side,
        )
    }
}
// KMK <--
