package app.nudge.reminders

import android.app.Application
import app.nudge.reminders.data.NudgeDatabase

class NudgeApp : Application() {
    val reminders by lazy { Reminders(this, NudgeDatabase.create(this).tasks()) }

    override fun onCreate() {
        super.onCreate()
        reminders.notifier.createChannel()
    }
}
