package app.nudge.reminders.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import app.nudge.reminders.NudgeApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val taskId = intent.getLongExtra(EXTRA_TASK_ID, -1)
        if (taskId < 0) return
        val reminders = (context.applicationContext as NudgeApp).reminders
        runAsync {
            when (intent.action) {
                ACTION_RING -> reminders.onAlarm(taskId)
                ACTION_DISMISSED -> reminders.onNotificationDismissed(taskId)
            }
        }
    }

    companion object {
        const val ACTION_RING = "app.nudge.reminders.RING"
        const val ACTION_DISMISSED = "app.nudge.reminders.DISMISSED"
        const val EXTRA_TASK_ID = "task_id"
    }
}

/** Restores alarms and pinned notifications after reboot, app updates and clock changes. */
class SystemEventsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val reminders = (context.applicationContext as NudgeApp).reminders
        runAsync { reminders.syncAll() }
    }
}

private fun BroadcastReceiver.runAsync(block: suspend () -> Unit) {
    val pending = goAsync()
    CoroutineScope(Dispatchers.IO).launch {
        try {
            block()
        } finally {
            pending.finish()
        }
    }
}
