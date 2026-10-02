package app.nudge.reminders.ui

import android.view.HapticFeedbackConstants
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.Replay
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.nudge.reminders.data.Repeat
import app.nudge.reminders.data.Task
import app.nudge.reminders.data.TaskStatus
import app.nudge.reminders.ui.theme.color
import kotlinx.coroutines.delay

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ActiveTaskCard(
    task: Task,
    now: Long,
    highlighted: Boolean,
    onClick: () -> Unit,
    onFinish: (TaskStatus) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val view = LocalView.current
    val colors = MaterialTheme.colorScheme
    val ringing = task.dueAt <= now
    val accent = task.priority.color()

    // Let the tick/cross animation play before the card leaves the list.
    var pendingStatus by remember { mutableStateOf<TaskStatus?>(null) }
    LaunchedEffect(pendingStatus) {
        pendingStatus?.let {
            delay(260)
            onFinish(it)
        }
    }
    val fade by animateFloatAsState(if (pendingStatus != null) 0.55f else 1f, label = "fade")

    val flash = remember { Animatable(0f) }
    LaunchedEffect(highlighted) {
        if (highlighted) repeat(2) {
            flash.animateTo(1f, tween(280))
            flash.animateTo(0f, tween(380))
        }
    }

    val borderColor = when {
        flash.value > 0f -> colors.primary.copy(alpha = flash.value)
        ringing -> colors.secondary.copy(alpha = 0.55f)
        else -> colors.outlineVariant.copy(alpha = 0.6f)
    }

    Surface(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .alpha(fade),
        shape = RoundedCornerShape(24.dp),
        color = if (ringing) colors.secondaryContainer.copy(alpha = 0.35f).compositeOver(colors.surfaceContainerLow) else colors.surfaceContainerLow,
        border = BorderStroke(if (ringing || flash.value > 0f) 1.5.dp else 1.dp, borderColor),
        shadowElevation = if (ringing) 6.dp else 1.dp,
    ) {
        Row(
            modifier = Modifier
                .height(IntrinsicSize.Min)
                .padding(start = 14.dp, end = 12.dp, top = 16.dp, bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier
                    .width(4.dp)
                    .fillMaxHeight()
                    .clip(CircleShape)
                    .background(accent),
            )
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                if (ringing) {
                    RingingPill()
                    Spacer(Modifier.height(8.dp))
                }
                Text(
                    task.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = colors.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    textDecoration = if (pendingStatus == TaskStatus.DONE) TextDecoration.LineThrough else null,
                )
                if (task.notes.isNotBlank()) {
                    Spacer(Modifier.height(3.dp))
                    Text(
                        task.notes,
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Spacer(Modifier.height(10.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    MetaChip(Icons.Rounded.Schedule, formatDue(context, task.dueAt))
                    MetaChip(
                        icon = null,
                        text = formatRelative(task.dueAt, now),
                        tint = if (ringing) colors.secondary else colors.primary,
                        container = if (ringing) colors.secondary.copy(alpha = 0.14f) else colors.primary.copy(alpha = 0.10f),
                    )
                    if (task.repeat != Repeat.NONE) MetaChip(Icons.Rounded.Repeat, task.repeat.label)
                }
            }
            Spacer(Modifier.width(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                RoundAction(
                    icon = Icons.Rounded.Close,
                    contentDescription = "Skip",
                    filled = pendingStatus == TaskStatus.SKIPPED,
                    activeColor = colors.onSurfaceVariant,
                ) {
                    if (pendingStatus == null) {
                        view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                        pendingStatus = TaskStatus.SKIPPED
                    }
                }
                RoundAction(
                    icon = Icons.Rounded.Check,
                    contentDescription = "Mark done",
                    filled = pendingStatus == TaskStatus.DONE,
                    activeColor = colors.tertiary,
                    emphasized = true,
                ) {
                    if (pendingStatus == null) {
                        view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                        pendingStatus = TaskStatus.DONE
                    }
                }
            }
        }
    }
}

@Composable
private fun RoundAction(
    icon: ImageVector,
    contentDescription: String,
    filled: Boolean,
    activeColor: Color,
    emphasized: Boolean = false,
    onClick: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = when {
            filled -> 1.12f
            pressed -> 0.88f
            else -> 1f
        },
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "scale",
    )
    val background by animateColorAsState(
        when {
            filled -> activeColor
            emphasized -> activeColor.copy(alpha = 0.14f)
            else -> colors.surfaceContainerHigh
        },
        label = "bg",
    )
    val tint by animateColorAsState(
        when {
            filled -> if (emphasized) colors.onTertiary else colors.surface
            emphasized -> activeColor
            else -> colors.onSurfaceVariant
        },
        label = "tint",
    )
    Box(
        modifier = Modifier
            .size(42.dp)
            .scale(scale)
            .clip(CircleShape)
            .background(background)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription, tint = tint, modifier = Modifier.size(22.dp))
    }
}

@Composable
private fun RingingPill() {
    val colors = MaterialTheme.colorScheme
    val wiggle = rememberInfiniteTransition(label = "wiggle")
    val angle by wiggle.animateFloat(
        initialValue = 0f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            keyframes {
                durationMillis = 1600
                0f at 0
                -16f at 80
                14f at 180
                -10f at 280
                6f at 380
                0f at 480
            },
        ),
        label = "angle",
    )
    val pulse by wiggle.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(800, easing = LinearEasing), RepeatMode.Reverse),
        label = "pulse",
    )
    Row(
        modifier = Modifier
            .clip(CircleShape)
            .background(colors.secondary.copy(alpha = 0.16f))
            .padding(start = 8.dp, end = 10.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            Icons.Rounded.NotificationsActive,
            contentDescription = null,
            tint = colors.secondary,
            modifier = Modifier
                .size(14.dp)
                .rotate(angle),
        )
        Spacer(Modifier.width(5.dp))
        Text("Ringing", style = MaterialTheme.typography.labelMedium, color = colors.secondary)
        Spacer(Modifier.width(6.dp))
        Box(
            Modifier
                .size(6.dp)
                .alpha(pulse)
                .clip(CircleShape)
                .background(colors.secondary),
        )
    }
}

@Composable
private fun MetaChip(
    icon: ImageVector?,
    text: String,
    tint: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    container: Color = MaterialTheme.colorScheme.surfaceContainerHigh,
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(container)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(13.dp))
            Spacer(Modifier.width(4.dp))
        }
        Text(text, style = MaterialTheme.typography.labelMedium, color = tint, maxLines = 1)
    }
}

@Composable
fun FinishedTaskCard(
    task: Task,
    onRestore: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val colors = MaterialTheme.colorScheme
    val done = task.status == TaskStatus.DONE
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = colors.surfaceContainerLow,
        border = BorderStroke(1.dp, colors.outlineVariant.copy(alpha = 0.6f)),
    ) {
        Row(
            modifier = Modifier.padding(start = 14.dp, end = 4.dp, top = 12.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(if (done) colors.tertiary.copy(alpha = 0.16f) else colors.surfaceContainerHigh),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    if (done) Icons.Rounded.Check else Icons.Rounded.Close,
                    contentDescription = if (done) "Done" else "Skipped",
                    tint = if (done) colors.tertiary else colors.onSurfaceVariant,
                    modifier = Modifier.size(20.dp),
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    task.title,
                    style = MaterialTheme.typography.titleSmall,
                    color = colors.onSurface.copy(alpha = 0.7f),
                    textDecoration = TextDecoration.LineThrough,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(2.dp))
                val completedAt = task.completedAt ?: task.dueAt
                Text(
                    buildString {
                        append(if (done) "Done" else "Skipped")
                        append(" · ")
                        append(formatTime(context, completedAt.toLocalDateTime().toLocalTime()))
                        append("  ·  was due ")
                        append(formatDue(context, task.dueAt))
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            IconButton(onClick = onRestore) {
                Icon(Icons.Rounded.Replay, contentDescription = "Move back to active", tint = colors.primary)
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Rounded.DeleteOutline, contentDescription = "Delete", tint = colors.onSurfaceVariant)
            }
        }
    }
}

@Composable
fun SectionHeader(title: String, count: Int, modifier: Modifier = Modifier, accent: Color = MaterialTheme.colorScheme.onSurfaceVariant) {
    Row(modifier.padding(start = 6.dp, top = 14.dp, bottom = 2.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(title.uppercase(), style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold), color = accent)
        Spacer(Modifier.width(8.dp))
        Text(
            count.toString(),
            style = MaterialTheme.typography.labelSmall,
            color = accent,
            modifier = Modifier
                .clip(CircleShape)
                .background(accent.copy(alpha = 0.12f))
                .padding(horizontal = 7.dp, vertical = 1.dp),
        )
    }
}
