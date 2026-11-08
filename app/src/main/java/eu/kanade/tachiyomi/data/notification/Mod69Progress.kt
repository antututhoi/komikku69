package eu.kanade.tachiyomi.data.notification

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.util.system.cancelNotification
import eu.kanade.tachiyomi.util.system.notify
import java.util.concurrent.ConcurrentHashMap

// KMK -->
/**
 * 69 mod: one progress notification (with numbers, the current item and a Stop button) and one
 * "finished" notification with the details, for the app's long jobs (covers, renaming, moving).
 */
object Mod69Progress {

    const val ID_COVER_BACKUP = -6901
    const val ID_COVER_RESTORE = -6902
    const val ID_COVER_IMPORT = -6903
    const val ID_RENAME = -6904
    const val ID_MOVE = -6905
    const val ID_CONVERT = -6906
    const val ID_REDOWNLOAD = -6907

    private const val EXTRA_ID = "mod69_job_id"
    private val stopped = ConcurrentHashMap.newKeySet<Int>()
    private val lastShown = ConcurrentHashMap<Int, Long>()

    /** Call when the job starts. */
    fun start(context: Context, id: Int, title: String) {
        stopped.remove(id)
        lastShown.remove(id)
        update(context, id, title, "", 0, 0, force = true)
    }

    /** True after Stop was tapped. */
    fun isStopped(id: Int): Boolean = id in stopped

    /** Shows "[current] / [total]" plus [text] (at most ~3 times a second). */
    fun update(
        context: Context,
        id: Int,
        title: String,
        text: String,
        current: Int,
        total: Int,
        force: Boolean = false,
    ) {
        val now = System.currentTimeMillis()
        if (!force && now - (lastShown[id] ?: 0L) < 300 && current < total) return
        lastShown[id] = now
        val stop = PendingIntent.getBroadcast(
            context,
            id,
            Intent(context, Mod69StopReceiver::class.java).putExtra(EXTRA_ID, id),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val percent = if (total > 0) " (${current * 100 / total}%)" else ""
        context.notify(id, Notifications.CHANNEL_BACKUP_RESTORE_PROGRESS) {
            setSmallIcon(R.drawable.ic_komikku)
            setContentTitle(title)
            setContentText(if (total > 0) "$current / $total$percent" else text)
            if (total > 0) setStyle(NotificationCompat.BigTextStyle().bigText("$current / $total$percent\n$text"))
            setProgress(total, current, total <= 0)
            setOngoing(true)
            setOnlyAlertOnce(true)
            addAction(R.drawable.ic_close_24dp, "Stop", stop)
        }
    }

    /** Replaces the progress notification with a finished one listing [lines]. */
    fun finish(context: Context, id: Int, title: String, lines: List<String>) {
        context.cancelNotification(id)
        lastShown.remove(id)
        val stoppedNow = stopped.remove(id)
        val body = (if (stoppedNow) listOf("Stopped before the end") else emptyList()) + lines
        context.notify(id - 100, Notifications.CHANNEL_BACKUP_RESTORE_COMPLETE) {
            setSmallIcon(R.drawable.ic_komikku)
            setContentTitle(title)
            setContentText(body.firstOrNull().orEmpty())
            setStyle(NotificationCompat.BigTextStyle().bigText(body.joinToString("\n")))
            setOnlyAlertOnce(false)
            setAutoCancel(true)
        }
    }

    internal fun stop(id: Int) {
        stopped += id
    }

    /** "1.2 GB", "340 MB"… */
    fun size(bytes: Long): String = when {
        bytes >= 1L shl 30 -> "%.2f GB".format(bytes / (1L shl 30).toDouble())
        bytes >= 1L shl 20 -> "%.1f MB".format(bytes / (1L shl 20).toDouble())
        else -> "${bytes / 1024} KB"
    }

    /** "2 min 05 s" */
    fun duration(ms: Long): String {
        val s = ms / 1000
        return if (s >= 60) "${s / 60} min %02d s".format(s % 60) else "$s s"
    }

    class Mod69StopReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            stop(intent.getIntExtra(EXTRA_ID, 0))
        }
    }
}
// KMK <--
