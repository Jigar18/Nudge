package app.nudge.reminders.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import app.nudge.reminders.data.Task

class ReminderScheduler(private val context: Context) {
    private val alarmManager = context.getSystemService(AlarmManager::class.java)

    fun canScheduleExact(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()

    fun schedule(task: Task) {
        val intent = alarmIntent(task.id)
        if (canScheduleExact()) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, task.dueAt, intent)
        } else {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, task.dueAt, intent)
        }
    }

    fun cancel(taskId: Long) = alarmManager.cancel(alarmIntent(taskId))

    private fun alarmIntent(taskId: Long): PendingIntent = PendingIntent.getBroadcast(
        context,
        taskId.toInt(),
        Intent(context, ReminderReceiver::class.java)
            .setAction(ReminderReceiver.ACTION_RING)
            .putExtra(ReminderReceiver.EXTRA_TASK_ID, taskId),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )
}
