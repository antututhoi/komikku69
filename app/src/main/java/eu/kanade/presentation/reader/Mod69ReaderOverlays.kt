package eu.kanade.presentation.reader

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import eu.kanade.tachiyomi.ui.reader.setting.ReaderPreferences
import kotlinx.coroutines.delay
import tachiyomi.presentation.core.util.collectAsState
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

/** 69 mod R12: one small line with clock, battery, chapter and page. */
@Composable
fun Mod69ReaderInfoBar(
    readerPreferences: ReaderPreferences,
    chapterName: String?,
    pageText: String,
    totalPages: Int,
) {
    val enabled by readerPreferences.mod69InfoBar().collectAsState()
    if (!enabled) return
    val showClock by readerPreferences.mod69InfoBarClock().collectAsState()
    val showBattery by readerPreferences.mod69InfoBarBattery().collectAsState()
    val showChapter by readerPreferences.mod69InfoBarChapter().collectAsState()
    val showPage by readerPreferences.mod69InfoBarPage().collectAsState()
    val position by readerPreferences.mod69InfoBarPosition().collectAsState()
    val transparent by readerPreferences.mod69InfoBarTransparent().collectAsState()
    val clockFormat by readerPreferences.mod69ClockFormat().collectAsState()
    val context = LocalContext.current

    val time by produceState(initialValue = mod69TimeText(context, clockFormat), clockFormat) {
        while (true) {
            value = mod69TimeText(context, clockFormat)
            delay(15_000)
        }
    }

    var battery by remember { mutableIntStateOf(-1) }
    DisposableEffect(showBattery) {
        if (!showBattery) return@DisposableEffect onDispose { }
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

    val parts = buildList {
        if (showClock) add(time)
        if (showBattery && battery >= 0) add("$battery%")
        if (showChapter && !chapterName.isNullOrBlank()) add(chapterName)
        if (showPage && totalPages > 0) add("$pageText/$totalPages")
    }
    if (parts.isEmpty()) return

    Box(modifier = Modifier.fillMaxSize().systemBarsPadding()) {
        Row(
            modifier = Modifier
                .align(if (position == "bottom") Alignment.BottomCenter else Alignment.TopCenter)
                .fillMaxWidth()
                .background(if (transparent) Color.Transparent else Color.Black.copy(alpha = 0.7f))
                .padding(horizontal = 12.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            parts.forEach {
                Text(
                    text = it,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontSize = 11.sp,
                    color = Color.White.copy(alpha = 0.85f),
                    modifier = Modifier.weight(1f, fill = false),
                )
            }
        }
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
// KMK <--
