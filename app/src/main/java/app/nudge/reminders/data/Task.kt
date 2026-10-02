package app.nudge.reminders.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.DayOfWeek
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime

enum class TaskStatus { ACTIVE, DONE, SKIPPED }

enum class Priority(val label: String) { LOW("Low"), MEDIUM("Medium"), HIGH("High") }

enum class Repeat(val label: String) {
    NONE("Once"),
    DAILY("Daily"),
    WEEKDAYS("Weekdays"),
    WEEKLY("Weekly"),
    MONTHLY("Monthly");

    /** First occurrence strictly after [now], keeping the original time of day. */
    fun nextAfter(dueAt: Long, now: Long): Long {
        val zone = ZoneId.systemDefault()
        var next = ZonedDateTime.ofInstant(Instant.ofEpochMilli(dueAt), zone)
        do {
            next = when (this) {
                NONE -> return dueAt
                DAILY -> next.plusDays(1)
                WEEKLY -> next.plusWeeks(1)
                MONTHLY -> next.plusMonths(1)
                WEEKDAYS -> {
                    var d = next.plusDays(1)
                    while (d.dayOfWeek == DayOfWeek.SATURDAY || d.dayOfWeek == DayOfWeek.SUNDAY) d = d.plusDays(1)
                    d
                }
            }
        } while (next.toInstant().toEpochMilli() <= now)
        return next.toInstant().toEpochMilli()
    }
}

@Entity(tableName = "tasks")
data class Task(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val notes: String = "",
    val dueAt: Long,
    val repeat: Repeat = Repeat.NONE,
    val priority: Priority = Priority.MEDIUM,
    val status: TaskStatus = TaskStatus.ACTIVE,
    val createdAt: Long = System.currentTimeMillis(),
    val completedAt: Long? = null,
    /** When the reminder notification was first shown; null while it is still upcoming. */
    val firedAt: Long? = null,
)
