package eu.kanade.presentation.more.stats

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import eu.kanade.tachiyomi.source.model.SManga
import tachiyomi.core.common.util.lang.withIOContext
import tachiyomi.domain.history.interactor.GetHistory
import tachiyomi.domain.manga.interactor.GetLibraryManga
import tachiyomi.domain.source.service.SourceManager
import tachiyomi.i18n.kmk.KMR
import tachiyomi.presentation.core.i18n.stringResource
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

// KMK -->
/** 69 mod S1 / S2: reading habits (streaks, average, top entries, time of day) and library breakdowns. */
data class Mod69StatsData(
    val currentStreak: Int,
    val longestStreak: Int,
    val activeDays: Int,
    val dailyAverageMs: Long,
    val chaptersRead: Int,
    val topEntries: List<Pair<String, Long>>,
    val timeOfDay: List<Pair<String, Int>>,
    val lastDays: List<Pair<LocalDate, Int>>,
    val bySource: List<Pair<String, Int>>,
    val byStatus: List<Pair<String, Int>>,
    val byLanguage: List<Pair<String, Int>>,
)

private suspend fun loadMod69Stats(): Mod69StatsData = withIOContext {
    val zone = ZoneId.systemDefault()
    val library = Injekt.get<GetLibraryManga>().await().distinctBy { it.id }
    val getHistory = Injekt.get<GetHistory>()
    val sourceManager = Injekt.get<SourceManager>()
    val perEntryTime = mutableListOf<Pair<String, Long>>()
    val days = mutableMapOf<LocalDate, Int>()
    val hours = IntArray(4)
    var total = 0L
    var chapters = 0
    library.forEach { item ->
        val history = runCatching { getHistory.await(item.manga.id) }.getOrDefault(emptyList())
        var time = 0L
        history.forEach { h ->
            time += h.readDuration.coerceAtLeast(0)
            val at = h.readAt?.time ?: return@forEach
            if (at <= 0) return@forEach
            chapters++
            val dateTime = Instant.ofEpochMilli(at).atZone(zone)
            days.merge(dateTime.toLocalDate(), 1, Int::plus)
            hours[dateTime.hour / 6]++
        }
        total += time
        if (time > 0) perEntryTime += item.manga.title to time
    }
    val sortedDays = days.keys.sorted()
    var longest = 0
    var run = 0
    var previous: LocalDate? = null
    sortedDays.forEach { day ->
        run = if (previous != null && previous!!.plusDays(1) == day) run + 1 else 1
        if (run > longest) longest = run
        previous = day
    }
    val today = LocalDate.now(zone)
    var current = 0
    var cursor = if (today in days) today else today.minusDays(1)
    while (cursor in days) {
        current++
        cursor = cursor.minusDays(1)
    }
    val statusName = mapOf(
        SManga.ONGOING to "Ongoing",
        SManga.COMPLETED to "Completed",
        SManga.LICENSED to "Licensed",
        SManga.PUBLISHING_FINISHED to "Publishing finished",
        SManga.CANCELLED to "Cancelled",
        SManga.ON_HIATUS to "On hiatus",
    )
    Mod69StatsData(
        currentStreak = current,
        longestStreak = longest,
        activeDays = days.size,
        dailyAverageMs = if (days.isEmpty()) 0 else total / days.size,
        chaptersRead = chapters,
        topEntries = perEntryTime.sortedByDescending { it.second }.take(5),
        timeOfDay = listOf("Night (0-6)", "Morning (6-12)", "Afternoon (12-18)", "Evening (18-24)")
            .mapIndexed { i, name -> name to hours[i] },
        lastDays = (13 downTo 0).map { today.minusDays(it.toLong()) }.map { it to (days[it] ?: 0) },
        bySource = library.groupingBy { sourceManager.getOrStub(it.manga.source).name }.eachCount()
            .entries.sortedByDescending { it.value }.take(8).map { it.key to it.value },
        byStatus = library.groupingBy { statusName[it.manga.status.toInt()] ?: "Unknown" }.eachCount()
            .entries.sortedByDescending { it.value }.map { it.key to it.value },
        byLanguage = library.groupingBy { sourceManager.getOrStub(it.manga.source).lang.ifBlank { "?" } }.eachCount()
            .entries.sortedByDescending { it.value }.take(8).map { it.key to it.value },
    )
}

private fun durationText(ms: Long): String {
    val minutes = ms / 60_000
    return if (minutes >= 60) "${minutes / 60} h ${minutes % 60} min" else "$minutes min"
}

@Composable
fun Mod69StatsSection() {
    val data by produceState<Mod69StatsData?>(null) { value = runCatching { loadMod69Stats() }.getOrNull() }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            stringResource(KMR.strings.mod69_stats_title),
            style = MaterialTheme.typography.titleSmall,
        )
        val d = data
        if (d == null) {
            Text("…")
            return@Column
        }
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Mod69Number(d.currentStreak.toString(), stringResource(KMR.strings.mod69_stats_streak))
            Mod69Number(d.longestStreak.toString(), stringResource(KMR.strings.mod69_stats_longest))
            Mod69Number(d.activeDays.toString(), stringResource(KMR.strings.mod69_stats_days))
            Mod69Number(durationText(d.dailyAverageMs), stringResource(KMR.strings.mod69_stats_average))
        }
        Text(stringResource(KMR.strings.mod69_stats_last14), style = MaterialTheme.typography.labelLarge)
        val maxDay = (d.lastDays.maxOfOrNull { it.second } ?: 0).coerceAtLeast(1)
        Row(
            modifier = Modifier.fillMaxWidth().height(70.dp),
            horizontalArrangement = Arrangement.spacedBy(3.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            d.lastDays.forEach { (_, count) ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(fraction = (count / maxDay.toFloat()).coerceAtLeast(0.03f))
                        .clip(RoundedCornerShape(3.dp))
                        .background(
                            if (count > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                        ),
                )
            }
        }
        Mod69Bars(stringResource(KMR.strings.mod69_stats_time_of_day), d.timeOfDay)
        if (d.topEntries.isNotEmpty()) {
            Text(stringResource(KMR.strings.mod69_stats_top), style = MaterialTheme.typography.labelLarge)
            d.topEntries.forEach { (title, ms) ->
                Row {
                    Text(
                        title,
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Text(durationText(ms), style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        Mod69Bars(stringResource(KMR.strings.mod69_stats_by_status), d.byStatus)
        Mod69Bars(stringResource(KMR.strings.mod69_stats_by_source), d.bySource)
        Mod69Bars(stringResource(KMR.strings.mod69_stats_by_language), d.byLanguage)
        Text(
            stringResource(KMR.strings.mod69_stats_note),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun Mod69Number(value: String, label: String) {
    Column {
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun Mod69Bars(title: String, rows: List<Pair<String, Int>>) {
    if (rows.isEmpty()) return
    Text(title, style = MaterialTheme.typography.labelLarge)
    val max = (rows.maxOfOrNull { it.second } ?: 0).coerceAtLeast(1)
    rows.forEach { (name, count) ->
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                name,
                modifier = Modifier.width(120.dp),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodySmall,
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(10.dp),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(fraction = (count / max.toFloat()).coerceAtLeast(0.02f))
                        .clip(RoundedCornerShape(3.dp))
                        .background(MaterialTheme.colorScheme.secondary),
                )
            }
            Text(" $count", style = MaterialTheme.typography.bodySmall)
        }
    }
}
// KMK <--
