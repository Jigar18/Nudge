package app.nudge.reminders

import android.content.Context
import app.nudge.reminders.alarm.ReminderNotifier
import app.nudge.reminders.alarm.ReminderScheduler
import app.nudge.reminders.data.Repeat
import app.nudge.reminders.data.Task
import app.nudge.reminders.data.TaskDao
import app.nudge.reminders.data.TaskStatus

/** Snapshot needed to revert a finish or delete. */
class Undo(val original: Task, val insertedRecordId: Long? = null)

/** Keeps the database, scheduled alarms and pinned notifications in agreement. */
class Reminders(context: Context, private val dao: TaskDao) {
    val scheduler = ReminderScheduler(context)
    val notifier = ReminderNotifier(context)

    val active = dao.observeActive()
    val finished = dao.observeFinished()

    suspend fun save(task: Task): Task {
        val stored = if (task.dueAt > now()) task.copy(firedAt = null) else task
        val id = if (stored.id == 0L) dao.insert(stored) else stored.id.also { dao.upsert(stored) }
        return stored.copy(id = id).also { arm(it) }
    }

    /** Marks a reminder done or skipped. Repeating reminders log the occurrence and roll forward. */
    suspend fun finish(task: Task, status: TaskStatus): Pair<Undo, Task?> {
        val now = now()
        scheduler.cancel(task.id)
        notifier.cancel(task.id)
        if (task.repeat == Repeat.NONE) {
            dao.upsert(task.copy(status = status, completedAt = now))
            return Undo(task) to null
        }
        val recordId = dao.insert(task.copy(id = 0, repeat = Repeat.NONE, status = status, completedAt = now))
        val next = task.copy(dueAt = task.repeat.nextAfter(task.dueAt, now), firedAt = null)
        dao.upsert(next)
        arm(next)
        return Undo(task, recordId) to next
    }

    suspend fun restore(task: Task) {
        val restored = task.copy(status = TaskStatus.ACTIVE, completedAt = null)
        dao.upsert(restored)
        arm(restored)
    }

    suspend fun delete(task: Task): Undo {
        scheduler.cancel(task.id)
        notifier.cancel(task.id)
        dao.delete(task)
        return Undo(task)
    }

    suspend fun undo(undo: Undo) {
        undo.insertedRecordId?.let { dao.deleteById(it) }
        dao.upsert(undo.original)
        if (undo.original.status == TaskStatus.ACTIVE) arm(undo.original)
    }

    suspend fun clearFinished() = dao.clearFinished()

    suspend fun onAlarm(taskId: Long) {
        val task = dao.find(taskId) ?: return
        if (task.status != TaskStatus.ACTIVE) return
        arm(task)
    }

    suspend fun onNotificationDismissed(taskId: Long) {
        val task = dao.find(taskId) ?: return
        if (task.status == TaskStatus.ACTIVE && task.dueAt <= now()) notifier.show(task, silent = true)
    }

    suspend fun syncAll() = dao.activeTasks().forEach { arm(it) }

    /** Schedules an upcoming reminder, or rings one that is already due. */
    private suspend fun arm(task: Task) {
        // Small tolerance: alarms can be delivered a moment before their exact time.
        if (task.dueAt > now() + 1_000) {
            notifier.cancel(task.id)
            scheduler.schedule(task)
            return
        }
        scheduler.cancel(task.id)
        val firstRing = task.firedAt == null
        if (firstRing) dao.upsert(task.copy(firedAt = now()))
        notifier.show(task, silent = !firstRing)
    }

    private fun now() = System.currentTimeMillis()
}
