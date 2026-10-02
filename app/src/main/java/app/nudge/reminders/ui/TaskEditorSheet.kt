package app.nudge.reminders.ui

import android.text.format.DateFormat
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import app.nudge.reminders.data.Priority
import app.nudge.reminders.data.Repeat
import app.nudge.reminders.data.Task
import app.nudge.reminders.ui.theme.BrandGradient
import app.nudge.reminders.ui.theme.color
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneOffset
import java.time.temporal.TemporalAdjusters

private data class QuickPick(val label: String, val at: LocalDateTime)

private fun quickPicks(now: LocalDateTime): List<QuickPick> {
    val today = now.toLocalDate()
    return buildList {
        add(QuickPick("In 15 min", now.plusMinutes(15)))
        add(QuickPick("In 1 hour", now.plusHours(1)))
        if (now.toLocalTime() < LocalTime.of(19, 30)) add(QuickPick("Tonight", today.atTime(20, 0)))
        add(QuickPick("Tomorrow morning", today.plusDays(1).atTime(9, 0)))
        add(QuickPick("This weekend", today.with(TemporalAdjusters.next(DayOfWeek.SATURDAY)).atTime(10, 0)))
        add(QuickPick("Next week", today.with(TemporalAdjusters.next(DayOfWeek.MONDAY)).atTime(9, 0)))
    }.map { it.copy(at = it.at.withSecond(0).withNano(0)) }
}

private fun defaultDueTime(): LocalDateTime {
    val inAnHour = LocalDateTime.now().plusHours(1).withSecond(0).withNano(0)
    val roundUp = (5 - inAnHour.minute % 5) % 5
    return inAnHour.plusMinutes(roundUp.toLong())
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TaskEditorSheet(
    task: Task?,
    onDismiss: () -> Unit,
    onSave: (Task) -> Unit,
    onDelete: (Task) -> Unit,
) {
    val context = LocalContext.current
    val colors = MaterialTheme.colorScheme
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    val initialDue = remember { task?.dueAt?.toLocalDateTime() ?: defaultDueTime() }
    var title by rememberSaveable { mutableStateOf(task?.title.orEmpty()) }
    var notes by rememberSaveable { mutableStateOf(task?.notes.orEmpty()) }
    var date by rememberSaveable { mutableStateOf(initialDue.toLocalDate()) }
    var time by rememberSaveable { mutableStateOf(initialDue.toLocalTime()) }
    var repeat by rememberSaveable { mutableStateOf(task?.repeat ?: Repeat.NONE) }
    var priority by rememberSaveable { mutableStateOf(task?.priority ?: Priority.MEDIUM) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    var now by remember { mutableStateOf(LocalDateTime.now()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(10_000)
            now = LocalDateTime.now()
        }
    }

    val due = date.atTime(time)
    val dueChanged = due != initialDue
    // Editing the title of a reminder that is already ringing shouldn't force a new time.
    val timeInPast = dueChanged && !due.isAfter(now)
    val canSave = title.isNotBlank() && !timeInPast

    fun close(after: () -> Unit = {}) {
        scope.launch { sheetState.hide() }.invokeOnCompletion {
            after()
            onDismiss()
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = colors.surfaceContainerLow,
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
    ) {
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 22.dp)
                .padding(bottom = 16.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        if (task == null) "New reminder" else "Edit reminder",
                        style = MaterialTheme.typography.headlineSmall,
                        color = colors.onSurface,
                    )
                    Text(
                        "Stays pinned in notifications until you tick it off",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.onSurfaceVariant,
                    )
                }
                if (task != null) {
                    IconButton(
                        onClick = { close { onDelete(task) } },
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(colors.error.copy(alpha = 0.10f)),
                    ) {
                        Icon(Icons.Rounded.DeleteOutline, contentDescription = "Delete reminder", tint = colors.error)
                    }
                }
            }

            Spacer(Modifier.height(18.dp))
            val focusRequester = remember { FocusRequester() }
            LaunchedEffect(Unit) { if (task == null) focusRequester.requestFocus() }
            EditorField(
                value = title,
                onValueChange = { title = it },
                placeholder = "What should I remind you about?",
                modifier = Modifier.focusRequester(focusRequester),
                large = true,
            )
            Spacer(Modifier.height(10.dp))
            EditorField(
                value = notes,
                onValueChange = { notes = it },
                placeholder = "Add notes (optional)",
            )

            SheetLabel("When")
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(horizontal = 22.dp),
                modifier = Modifier.bleedHorizontally(22.dp),
            ) {
                items(quickPicks(now), key = { it.label }) { pick ->
                    val selected = pick.at == due
                    SelectPill(
                        text = pick.label,
                        selected = selected,
                        icon = Icons.Rounded.Bolt,
                        onClick = {
                            date = pick.at.toLocalDate()
                            time = pick.at.toLocalTime()
                        },
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                PickerTile(
                    icon = Icons.Rounded.CalendarMonth,
                    label = "Date",
                    value = dayLabel(date),
                    modifier = Modifier.weight(1f),
                    onClick = { showDatePicker = true },
                )
                PickerTile(
                    icon = Icons.Rounded.Schedule,
                    label = "Time",
                    value = formatTime(context, time),
                    modifier = Modifier.weight(1f),
                    onClick = { showTimePicker = true },
                )
            }
            Spacer(Modifier.height(10.dp))
            val untilDue = due.toEpochMillis() - System.currentTimeMillis()
            AnimatedContent(
                targetState = timeInPast,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "hint",
            ) { past ->
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 4.dp)) {
                    Icon(
                        if (past) Icons.Rounded.WarningAmber else Icons.Rounded.NotificationsActive,
                        contentDescription = null,
                        tint = if (past) colors.error else colors.primary,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        when {
                            past -> "That time has already passed — pick a later one"
                            untilDue <= 0 -> "Ringing now"
                            else -> "Rings in ${formatDuration(untilDue)}"
                        },
                        style = MaterialTheme.typography.labelLarge,
                        color = if (past) colors.error else colors.primary,
                    )
                }
            }

            SheetLabel("Repeat")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Repeat.entries.forEach { option ->
                    SelectPill(text = option.label, selected = repeat == option, onClick = { repeat = option })
                }
            }

            SheetLabel("Priority")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Priority.entries.forEach { option ->
                    SelectPill(
                        text = option.label,
                        selected = priority == option,
                        dotColor = option.color(),
                        modifier = Modifier.weight(1f),
                        onClick = { priority = option },
                    )
                }
            }

            Spacer(Modifier.height(26.dp))
            GradientButton(
                text = if (task == null) "Set reminder" else "Save changes",
                enabled = canSave,
                onClick = {
                    val edited = (task ?: Task(title = "", dueAt = 0)).copy(
                        title = title.trim(),
                        notes = notes.trim(),
                        dueAt = due.toEpochMillis(),
                        repeat = repeat,
                        priority = priority,
                    )
                    close { onSave(edited) }
                },
            )
        }
    }

    if (showDatePicker) {
        val todayUtc = LocalDate.now().atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long) = utcTimeMillis >= todayUtc
                override fun isSelectableYear(year: Int) = year >= LocalDate.now().year
            },
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let {
                        date = Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate()
                    }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Cancel") } },
        ) {
            DatePicker(state = pickerState, showModeToggle = false)
        }
    }

    if (showTimePicker) {
        val pickerState = rememberTimePickerState(
            initialHour = time.hour,
            initialMinute = time.minute,
            is24Hour = DateFormat.is24HourFormat(context),
        )
        BasicAlertDialog(onDismissRequest = { showTimePicker = false }) {
            Surface(shape = RoundedCornerShape(28.dp), color = colors.surfaceContainerHigh) {
                Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "Select time",
                        style = MaterialTheme.typography.labelLarge,
                        color = colors.onSurfaceVariant,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 20.dp),
                    )
                    TimePicker(
                        state = pickerState,
                        colors = TimePickerDefaults.colors(clockDialColor = colors.surfaceContainerHighest),
                    )
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = { showTimePicker = false }) { Text("Cancel") }
                        TextButton(onClick = {
                            time = LocalTime.of(pickerState.hour, pickerState.minute)
                            showTimePicker = false
                        }) { Text("OK") }
                    }
                }
            }
        }
    }
}

@Composable
private fun EditorField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    large: Boolean = false,
) {
    val colors = MaterialTheme.colorScheme
    val textStyle = if (large) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyLarge
    TextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        textStyle = textStyle,
        placeholder = { Text(placeholder, style = textStyle, color = colors.onSurfaceVariant.copy(alpha = 0.7f)) },
        shape = RoundedCornerShape(18.dp),
        maxLines = if (large) 3 else 5,
        minLines = if (large) 1 else 2,
        keyboardOptions = KeyboardOptions(
            capitalization = KeyboardCapitalization.Sentences,
            imeAction = if (large) ImeAction.Next else ImeAction.Default,
        ),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = colors.surfaceContainerHigh,
            unfocusedContainerColor = colors.surfaceContainer,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            cursorColor = colors.primary,
        ),
    )
}

@Composable
private fun SheetLabel(text: String) {
    Text(
        text.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 4.dp, top = 22.dp, bottom = 10.dp),
    )
}

@Composable
private fun PickerTile(
    icon: ImageVector,
    label: String,
    value: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        color = colors.surfaceContainer,
    ) {
        Row(Modifier.padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(colors.primary.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, contentDescription = null, tint = colors.primary, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(10.dp))
            Column {
                Text(label, style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant)
                Text(value, style = MaterialTheme.typography.titleSmall, color = colors.onSurface, maxLines = 1)
            }
        }
    }
}

@Composable
private fun SelectPill(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    dotColor: Color? = null,
) {
    val colors = MaterialTheme.colorScheme
    val container by animateColorAsState(if (selected) colors.primary else colors.surfaceContainer, label = "pill")
    val content by animateColorAsState(if (selected) colors.onPrimary else colors.onSurface, label = "pillText")
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = CircleShape,
        color = container,
        border = if (selected) null else BorderStroke(1.dp, colors.outlineVariant),
    ) {
        Row(
            Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            if (dotColor != null) {
                Box(
                    Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(if (selected) colors.onPrimary else dotColor),
                )
                Spacer(Modifier.width(7.dp))
            }
            if (icon != null) {
                Icon(icon, contentDescription = null, tint = if (selected) content else colors.primary, modifier = Modifier.size(15.dp))
                Spacer(Modifier.width(5.dp))
            }
            Text(text, style = MaterialTheme.typography.labelLarge, color = content, maxLines = 1)
        }
    }
}

@Composable
fun GradientButton(text: String, enabled: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .alpha(if (enabled) 1f else 0.4f)
            .clip(RoundedCornerShape(18.dp))
            .background(BrandGradient)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, style = MaterialTheme.typography.titleMedium, color = Color.White)
    }
}

/** Lets a horizontally scrolling row run edge to edge inside a padded column. */
private fun Modifier.bleedHorizontally(padding: Dp) = layout { measurable, constraints ->
    val extra = padding.roundToPx() * 2
    val placeable = measurable.measure(constraints.copy(minWidth = constraints.maxWidth + extra, maxWidth = constraints.maxWidth + extra))
    layout(constraints.maxWidth, placeable.height) { placeable.place(-extra / 2, 0) }
}
