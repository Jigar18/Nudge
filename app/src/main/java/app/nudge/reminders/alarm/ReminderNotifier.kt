package app.nudge.reminders.alarm

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import app.nudge.reminders.MainActivity
import app.nudge.reminders.R
import app.nudge.reminders.data.Priority
import app.nudge.reminders.data.Task
import app.nudge.reminders.ui.formatTime
import java.time.Instant
import java.time.ZoneId

class ReminderNotifier(private val context: Context) {
    private val manager = NotificationManagerCompat.from(context)

    fun canPost(): Boolean {
        if (!manager.areNotificationsEnabled()) return false
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
    }

    fun createChannel() {
        val channel = NotificationChannel(CHANNEL_ID, "Reminders", NotificationManager.IMPORTANCE_HIGH).apply {
            description = "Rings when a reminder is due. Stays pinned until you mark it done in the app."
            enableVibration(true)
            vibrationPattern = longArrayOf(0, 250, 120, 250)
            enableLights(true)
            lightColor = 0xFF5B47E0.toInt()
            lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
            setSound(
                RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION),
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION_EVENT)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build(),
            )
        }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    /**
     * Posts the pinned reminder. [silent] is used when re-posting a reminder that already rang,
     * e.g. after the user swiped it away on Android 14+, where ongoing notifications became dismissible.
     */
    fun show(task: Task, silent: Boolean) {
        if (!canPost()) return
        val id = task.id.toInt()
        val openApp = PendingIntent.getActivity(
            context,
            id,
            Intent(context, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                .putExtra(ReminderReceiver.EXTRA_TASK_ID, task.id),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val repostOnDismiss = PendingIntent.getBroadcast(
            context,
            id,
            Intent(context, ReminderReceiver::class.java)
                .setAction(ReminderReceiver.ACTION_DISMISSED)
                .putExtra(ReminderReceiver.EXTRA_TASK_ID, task.id),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val time = Instant.ofEpochMilli(task.dueAt).atZone(ZoneId.systemDefault()).toLocalTime()
        val body = task.notes.ifBlank { "Due at ${formatTime(context, time)} · Open Nudge to tick it off" }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setColor(if (task.priority == Priority.HIGH) 0xFFF04E5E.toInt() else 0xFF5B47E0.toInt())
            .setContentTitle(task.title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setSubText(if (task.priority == Priority.HIGH) "High priority" else null)
            .setWhen(task.dueAt)
            .setShowWhen(true)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setAutoCancel(false)
            .setSilent(silent)
            .setContentIntent(openApp)
            .setDeleteIntent(repostOnDismiss)
            .build()
            .apply { flags = flags or android.app.Notification.FLAG_NO_CLEAR }

        manager.notify(id, notification)
    }

    fun cancel(taskId: Long) = manager.cancel(taskId.toInt())

    companion object {
        const val CHANNEL_ID = "reminders"
    }
}
