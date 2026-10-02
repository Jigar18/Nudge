package app.nudge.reminders.ui

import android.content.Context
import android.text.format.DateFormat
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

fun Long.toLocalDateTime(): LocalDateTime =
    LocalDateTime.ofInstant(Instant.ofEpochMilli(this), ZoneId.systemDefault())

fun LocalDateTime.toEpochMillis(): Long = atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

fun formatTime(context: Context, time: LocalTime): String {
    val pattern = if (DateFormat.is24HourFormat(context)) "HH:mm" else "h:mm a"
    return time.format(DateTimeFormatter.ofPattern(pattern, Locale.getDefault()))
}

fun formatDate(date: LocalDate, today: LocalDate = LocalDate.now()): String {
    val pattern = if (date.year == today.year) "EEE, d MMM" else "EEE, d MMM yyyy"
    return date.format(DateTimeFormatter.ofPattern(pattern, Locale.getDefault()))
}

fun dayLabel(date: LocalDate, today: LocalDate = LocalDate.now()): String = when (date) {
    today -> "Today"
    today.plusDays(1) -> "Tomorrow"
    today.minusDays(1) -> "Yesterday"
    else -> formatDate(date, today)
}

/** "Today · 6:30 PM", "Tomorrow · 09:00", "Sat, 4 Oct · 9:00 AM" */
fun formatDue(context: Context, epochMillis: Long): String {
    val dateTime = epochMillis.toLocalDateTime()
    return "${dayLabel(dateTime.toLocalDate())} · ${formatTime(context, dateTime.toLocalTime())}"
}

fun formatDuration(millis: Long): String {
    val minutes = (millis + 59_999) / 60_000
    return when {
        minutes < 60 -> "$minutes min"
        minutes < 24 * 60 -> {
            val h = minutes / 60
            val m = minutes % 60
            if (m == 0L) "$h h" else "$h h $m min"
        }
        minutes < 14 * 24 * 60 -> plural(minutes / (24 * 60), "day")
        else -> plural(minutes / (7 * 24 * 60), "week")
    }
}

/** "in 45 min", "5 min ago", "now" */
fun formatRelative(epochMillis: Long, now: Long): String {
    val diff = epochMillis - now
    return when {
        diff in -60_000..0 -> "now"
        diff > 0 -> "in ${formatDuration(diff)}"
        else -> "${formatDuration(-diff - 59_999)} ago"
    }
}

fun daysBetween(from: LocalDate, to: LocalDate) = ChronoUnit.DAYS.between(from, to)

private fun plural(count: Long, unit: String) = if (count == 1L) "1 $unit" else "$count ${unit}s"
