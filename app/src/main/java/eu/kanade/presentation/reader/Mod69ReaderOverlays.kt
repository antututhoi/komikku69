package eu.kanade.presentation.reader

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import eu.kanade.tachiyomi.ui.reader.setting.ReaderPreferences
import kotlinx.coroutines.delay
import tachiyomi.presentation.core.util.collectAsState
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// KMK -->
/** 69 mod: formats the time of day according to the "Clock format" setting (U9). */
fun mod69TimeText(context: Context, format: String, now: Long = System.currentTimeMillis()): String {
    val use24 = when (format) {
        "12" -> false
        "24" -> true
        else -> android.text.format.DateFormat.is24HourFormat(context)
    }
    return SimpleDateFormat(if (use24) "HH:mm" else "h:mm a", Locale.getDefault()).format(Date(now))
}

/**
 * 69 mod R12: reader info in the same look as the page number (theme colour with a dark outline).
 * Top: one centred line "entry · chapter", shown a few seconds then fades.
 * Bottom: battery on the left, chapter x / y in the middle (above the page number), clock on the right.
 * Clock and battery only in fullscreen (otherwise the status bar shows them).
 */
@Composable
fun Mod69ReaderInfoBar(
    readerPreferences: ReaderPreferences,
    mangaTitle: String?,
    chapterName: String?,
    chapterPosition: Pair<Int, Int>?,
) {
    val enabled by readerPreferences.mod69InfoBar().collectAsState()
    if (!enabled) return
    val showClock by readerPreferences.mod69InfoBarClock().collectAsState()
    val showBattery by readerPreferences.mod69InfoBarBattery().collectAsState()
    val showChapter by readerPreferences.mod69InfoBarChapter().collectAsState()
    val showCount by readerPreferences.mod69InfoBarPage().collectAsState()
    val fullscreen by readerPreferences.fullscreen().collectAsState()
    val clockFormat by readerPreferences.mod69ClockFormat().collectAsState()
    val context = LocalContext.current
    val clockOn = showClock && fullscreen
    val batteryOn = showBattery && fullscreen

    val time by produceState(initialValue = mod69TimeText(context, clockFormat), clockFormat) {
        while (true) {
            value = mod69TimeText(context, clockFormat)
            delay(15_000)
        }
    }

    var battery by remember { mutableIntStateOf(-1) }
    DisposableEffect(batteryOn) {
        if (!batteryOn) return@DisposableEffect onDispose { }
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(c: Context, intent: Intent) {
                val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, 100)
                if (level >= 0 && scale > 0) battery = level * 100 / scale
            }
        }
        val sticky = context.registerReceiver(receiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        sticky?.let { receiver.onReceive(context, it) }
        onDispose { runCatching { context.unregisterReceiver(receiver) } }
    }

    Box(modifier = Modifier.fillMaxSize().systemBarsPadding().padding(horizontal = 8.dp, vertical = 2.dp)) {
        if (showChapter) {
            // One short centred line "Series · Chapter", shown for a few seconds when a chapter opens
            // or the menu closes, then it fades away so it doesn't cover the page.
            var titleVisible by remember { mutableStateOf(true) }
            LaunchedEffect(chapterName) {
                titleVisible = true
                delay(4_000)
                titleVisible = false
            }
            val line = listOfNotNull(
                mangaTitle?.takeIf { it.isNotBlank() },
                chapterName?.takeIf { it.isNotBlank() },
            ).joinToString("  ·  ")
            AnimatedVisibility(
                visible = titleVisible,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.align(Alignment.TopCenter).fillMaxWidth().padding(horizontal = 24.dp),
            ) {
                Mod69OutlinedText(text = line, modifier = Modifier.fillMaxWidth())
            }
        }
        if (batteryOn && battery >= 0) {
            Mod69OutlinedText(
                text = "$battery%",
                modifier = Modifier.align(Alignment.BottomStart).fillMaxWidth(),
                textAlign = TextAlign.Start,
            )
        }
        if (showCount && chapterPosition != null && chapterPosition.second > 0) {
            Mod69OutlinedText(
                text = "Ch ${chapterPosition.first} / ${chapterPosition.second}",
                // one line above the app's own page number
                modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(bottom = 18.dp),
            )
        }
        if (clockOn) {
            Mod69OutlinedText(
                text = time,
                modifier = Modifier.align(Alignment.BottomEnd).fillMaxWidth(),
                textAlign = TextAlign.End,
            )
        }
    }
}

/** Text in the same style as the reader's page number: theme colour, bold, dark outline. */
@Composable
private fun Mod69OutlinedText(text: String, modifier: Modifier = Modifier, textAlign: TextAlign = TextAlign.Center) {
    if (text.isBlank()) return
    val style = TextStyle(
        color = MaterialTheme.colorScheme.primary,
        fontSize = MaterialTheme.typography.bodySmall.fontSize,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.sp,
        textAlign = textAlign,
    )
    val outline = style.copy(color = Color(45, 45, 45), drawStyle = Stroke(width = 4f))
    Box(modifier = modifier) {
        Text(text = text, style = outline, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.fillMaxWidth())
        Text(text = text, style = style, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.fillMaxWidth())
    }
}

/** 69 mod R14: round + and - zoom buttons on the right edge. */
@Composable
fun Mod69ZoomButtons(
    readerPreferences: ReaderPreferences,
    onZoom: (Float) -> Unit,
) {
    val enabled by readerPreferences.mod69ZoomButtons().collectAsState()
    if (!enabled) return
    Box(modifier = Modifier.fillMaxSize().navigationBarsPadding()) {
        Column(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            listOf(Icons.Filled.Add to 1.4f, Icons.Filled.Remove to (1f / 1.4f)).forEach { (icon, factor) ->
                IconButton(
                    onClick = { onZoom(factor) },
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.6f)),
                ) {
                    Icon(imageVector = icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}

/** 69 mod R15: how see-through the reader bars are. */
@Composable
fun mod69BarAlpha(default: Float): Float {
    val percent by remember { Injekt.get<ReaderPreferences>().mod69BarOpacity() }.collectAsState()
    return if (percent in 1..100) percent / 100f else default
}

/** 69 mod N23: true while the page slider is being dragged. */
object Mod69SliderDrag {
    var dragging by mutableStateOf(false)
}

/** 69 mod R15: a small round button in the corner that shows / hides the menu. */
@Composable
fun Mod69FloatingMenuButton(
    readerPreferences: ReaderPreferences,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    val enabled by readerPreferences.mod69FloatingMenuButton().collectAsState()
    if (!enabled) return
    Box(modifier = Modifier.fillMaxSize().navigationBarsPadding()) {
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 16.dp, bottom = 24.dp)
                .size(44.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.55f))
                .combinedClickable(onClick = onClick, onLongClick = onLongClick),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.Menu,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
        }
    }
}
// KMK <--
