package app.nudge.reminders.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import app.nudge.reminders.NudgeApp
import app.nudge.reminders.Undo
import app.nudge.reminders.data.Task
import app.nudge.reminders.data.TaskStatus
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class UiMessage(val text: String, val undo: Undo? = null)

class HomeViewModel(app: Application) : AndroidViewModel(app) {
    private val reminders = (app as NudgeApp).reminders

    /** Null until the first database emission, so empty states don't flash on launch. */
    val active: StateFlow<List<Task>?> =
        reminders.active.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
    val finished: StateFlow<List<Task>?> =
        reminders.finished.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val messageChannel = Channel<UiMessage>(Channel.BUFFERED)
    val messages = messageChannel.receiveAsFlow()

    /** Task the user opened from a notification; the list scrolls to it and highlights it. */
    val focusedTaskId = MutableStateFlow<Long?>(null)

    fun focus(taskId: Long) {
        focusedTaskId.value = taskId
    }

    fun save(task: Task) = viewModelScope.launch {
        val isNew = task.id == 0L
        val saved = reminders.save(task)
        val text = if (isNew) "Reminder set for ${formatDue(getApplication(), saved.dueAt)}" else "Reminder updated"
        messageChannel.send(UiMessage(text))
    }

    fun finish(task: Task, status: TaskStatus) = viewModelScope.launch {
        val (undo, next) = reminders.finish(task, status)
        val verb = if (status == TaskStatus.DONE) "Done" else "Skipped"
        val text = if (next != null) "$verb · next one ${formatDue(getApplication(), next.dueAt)}" else "$verb · moved to Completed"
        messageChannel.send(UiMessage(text, undo))
    }

    fun restore(task: Task) = viewModelScope.launch {
        reminders.restore(task)
        messageChannel.send(UiMessage("Moved back to Active"))
    }

    fun delete(task: Task) = viewModelScope.launch {
        messageChannel.send(UiMessage("Reminder deleted", reminders.delete(task)))
    }

    fun undo(undo: Undo) = viewModelScope.launch { reminders.undo(undo) }

    fun clearFinished() = viewModelScope.launch { reminders.clearFinished() }

    fun sync() = viewModelScope.launch { reminders.syncAll() }
}
