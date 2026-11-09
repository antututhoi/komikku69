package eu.kanade.presentation.reader

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.ui.reader.model.ReaderPage
import tachiyomi.core.common.util.lang.withIOContext
import tachiyomi.i18n.MR
import tachiyomi.i18n.kmk.KMR
import tachiyomi.presentation.core.i18n.stringResource
import java.util.concurrent.ConcurrentHashMap

// KMK -->
/** 69 mod R18 / N23: small pictures of the pages of the chapter that is open. */
object Mod69Thumbs {
    private val cache = ConcurrentHashMap<String, Bitmap>()

    private fun key(page: ReaderPage) = "${page.chapter.chapter.id}_${page.index}"

    fun cached(page: ReaderPage): Bitmap? = cache[key(page)]

    /** Makes a small picture (about [target] pixels wide) once the page is downloaded. */
    suspend fun load(page: ReaderPage, target: Int = 240): Bitmap? {
        cache[key(page)]?.let { return it }
        if (page.status != Page.State.Ready) return null
        val stream = page.stream ?: return null
        return withIOContext {
            try {
                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                stream().use { BitmapFactory.decodeStream(it, null, bounds) }
                var sample = 1
                while (bounds.outWidth / (sample * 2) >= target) sample *= 2
                val options = BitmapFactory.Options().apply {
                    inSampleSize = sample
                    inPreferredConfig = Bitmap.Config.RGB_565
                }
                val bitmap = stream().use { BitmapFactory.decodeStream(it, null, options) }
                // Very tall long-strip pages: keep only the top part so the picture stays readable
                val trimmed = bitmap?.let {
                    if (it.height > it.width * 3) Bitmap.createBitmap(it, 0, 0, it.width, it.width * 3) else it
                }
                trimmed?.also { cache[key(page)] = it }
            } catch (_: Throwable) {
                null
            }
        }
    }

    fun clear() = cache.clear()
}

@Composable
fun Mod69PageThumb(page: ReaderPage, modifier: Modifier = Modifier) {
    val status by page.statusFlow.collectAsState()
    val image: ImageBitmap? by produceState<ImageBitmap?>(
        initialValue = Mod69Thumbs.cached(page)?.asImageBitmap(),
        page,
        status,
    ) {
        if (value == null) value = Mod69Thumbs.load(page)?.asImageBitmap()
    }
    Box(
        modifier = modifier.background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center,
    ) {
        image?.let {
            Image(
                bitmap = it,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
        Text(
            text = "${page.index + 1}",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(2.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.85f))
                .padding(horizontal = 4.dp),
        )
    }
}

@Composable
fun Mod69PageThumbnailsDialog(
    pages: List<ReaderPage>,
    currentIndex: Int,
    onPick: (Int) -> Unit,
    onDismissRequest: () -> Unit,
) {
    val gridState = rememberLazyGridState(initialFirstVisibleItemIndex = (currentIndex - 3).coerceAtLeast(0))
    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text(stringResource(KMR.strings.mod69_all_pages)) },
        text = {
            if (pages.isEmpty()) {
                Text(stringResource(KMR.strings.mod69_all_pages_empty))
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    state = gridState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 520.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    itemsIndexed(pages, key = { _, page -> page.index }) { index, page ->
                        Mod69PageThumb(
                            page = page,
                            modifier = Modifier
                                .aspectRatio(0.7f)
                                .clip(RoundedCornerShape(6.dp))
                                .then(
                                    if (index == currentIndex) {
                                        Modifier.border(3.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(6.dp))
                                    } else {
                                        Modifier
                                    },
                                )
                                .clickable { onPick(index) },
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismissRequest) { Text(stringResource(MR.strings.action_cancel)) }
        },
    )
}
// KMK <--
