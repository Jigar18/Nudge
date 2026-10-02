package app.nudge.reminders.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.DoneAll
import androidx.compose.material.icons.rounded.NotificationsNone
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.nudge.reminders.R
import app.nudge.reminders.data.Task
import app.nudge.reminders.data.TaskStatus
import app.nudge.reminders.ui.theme.BrandGradient
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private sealed interface ListRow {
    val key: Any
}

private data class HeaderRow(val title: String, val count: Int, val urgent: Boolean = false) : ListRow {
    override val key = "header-$title"
}

private data class TaskRow(val task: Task) : ListRow {
    override val key = task.id
}

private fun groupActive(tasks: List<Task>, now: Long): List<ListRow> {
    val today = LocalDate.now()
    return tasks
        .groupBy { task ->
            val date = task.dueAt.toLocalDateTime().toLocalDate()
            when {
                task.dueAt <= now -> "Ringing now"
                date == today -> "Today"
                date == today.plusDays(1) -> "Tomorrow"
                daysBetween(today, date) < 7 -> "This week"
                else -> "Later"
            }
        }
        .flatMap { (title, group) -> listOf(HeaderRow(title, group.size, urgent = title == "Ringing now")) + group.map(::TaskRow) }
}

private fun groupFinished(tasks: List<Task>): List<ListRow> =
    tasks
        .groupBy { dayLabel((it.completedAt ?: it.dueAt).toLocalDateTime().toLocalDate()) }
        .flatMap { (title, group) -> listOf(HeaderRow(title, group.size)) + group.map(::TaskRow) }

private val EaseOutQuint = CubicBezierEasing(0.22f, 1f, 0.36f, 1f)

@Composable
private fun Modifier.riseIn(delayMillis: Int): Modifier {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        delay(delayMillis.toLong())
        progress.animateTo(1f, tween(650, easing = EaseOutQuint))
    }
    return graphicsLayer {
        alpha = progress.value
        translationY = (1f - progress.value) * 36.dp.toPx()
    }
}

@Composable
fun HomeScreen(viewModel: HomeViewModel) {
    val active by viewModel.active.collectAsStateWithLifecycle()
    val finished by viewModel.finished.collectAsStateWithLifecycle()
    val focusedTaskId by viewModel.focusedTaskId.collectAsStateWithLifecycle()

    val now by produceState(System.currentTimeMillis()) {
        while (true) {
            delay(15_000)
            value = System.currentTimeMillis()
        }
    }

    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val pagerState = rememberPagerState(pageCount = { 2 })
    val activeListState = rememberLazyListState()

    var editorOpen by remember { mutableStateOf(false) }
    var editingTask by remember { mutableStateOf<Task?>(null) }
    var highlightedId by remember { mutableStateOf<Long?>(null) }

    val activeRows = remember(active, now) { groupActive(active.orEmpty(), now) }
    val finishedRows = remember(finished) { groupFinished(finished.orEmpty()) }

    LaunchedEffect(Unit) {
        viewModel.messages.collect { message ->
            snackbarHostState.currentSnackbarData?.dismiss()
            launch {
                val result = snackbarHostState.showSnackbar(message.text, actionLabel = message.undo?.let { "Undo" })
                if (result == SnackbarResult.ActionPerformed) message.undo?.let(viewModel::undo)
            }
        }
    }

    LaunchedEffect(focusedTaskId, active) {
        val id = focusedTaskId ?: return@LaunchedEffect
        if (active == null) return@LaunchedEffect
        pagerState.animateScrollToPage(0)
        val index = activeRows.indexOfFirst { it is TaskRow && it.task.id == id }
        if (index >= 0) activeListState.animateScrollToItem(index)
        highlightedId = id
        viewModel.focusedTaskId.value = null
        delay(1_400)
        highlightedId = null
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = {
            SnackbarHost(snackbarHostState) { data ->
                Snackbar(
                    snackbarData = data,
                    shape = RoundedCornerShape(16.dp),
                    containerColor = MaterialTheme.colorScheme.inverseSurface,
                    contentColor = MaterialTheme.colorScheme.inverseOnSurface,
                    actionColor = MaterialTheme.colorScheme.inversePrimary,
                )
            }
        },
        floatingActionButton = {
            NewReminderFab(
                open = editorOpen,
                modifier = Modifier.riseIn(350),
                onClick = {
                    editingTask = null
                    editorOpen = true
                },
            )
        },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(top = padding.calculateTopPadding()),
        ) {
            Header(now, Modifier.riseIn(0))
            SummaryCard(
                active = active.orEmpty(),
                finished = finished.orEmpty(),
                now = now,
                modifier = Modifier
                    .padding(horizontal = 20.dp)
                    .riseIn(80),
            )
            SetupBanner(Modifier.padding(start = 20.dp, end = 20.dp, top = 14.dp))
            SegmentedTabs(
                pagerState = pagerState,
                activeCount = active?.size ?: 0,
                finishedCount = finished?.size ?: 0,
                onSelect = { scope.launch { pagerState.animateScrollToPage(it) } },
                modifier = Modifier
                    .padding(start = 20.dp, end = 20.dp, top = 18.dp, bottom = 4.dp)
                    .riseIn(160),
            )
            val listPadding = PaddingValues(
                start = 20.dp,
                end = 20.dp,
                top = 4.dp,
                bottom = padding.calculateBottomPadding() + 110.dp,
            )
            HorizontalPager(
                state = pagerState,
                beyondViewportPageCount = 1,
                modifier = Modifier
                    .weight(1f)
                    .riseIn(220),
            ) { page ->
                when (page) {
                    0 -> ActiveList(
                        loaded = active != null,
                        rows = activeRows,
                        now = now,
                        highlightedId = highlightedId,
                        listState = activeListState,
                        contentPadding = listPadding,
                        onEdit = {
                            editingTask = it
                            editorOpen = true
                        },
                        onFinish = viewModel::finish,
                    )
                    else -> FinishedList(
                        loaded = finished != null,
                        rows = finishedRows,
                        contentPadding = listPadding,
                        onRestore = viewModel::restore,
                        onDelete = viewModel::delete,
                        onClearAll = viewModel::clearFinished,
                    )
                }
            }
        }
    }

    if (editorOpen) {
        TaskEditorSheet(
            task = editingTask,
            onDismiss = { editorOpen = false },
            onSave = viewModel::save,
            onDelete = viewModel::delete,
        )
    }
}

@Composable
private fun Header(now: Long, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    val dateTime = now.toLocalDateTime()
    val greeting = when (dateTime.hour) {
        in 5..11 -> "Good morning"
        in 12..16 -> "Good afternoon"
        in 17..21 -> "Good evening"
        else -> "Still up?"
    }
    val date = dateTime.format(DateTimeFormatter.ofPattern("EEEE, d MMMM", Locale.getDefault()))
    Row(
        modifier = modifier.padding(start = 24.dp, end = 20.dp, top = 14.dp, bottom = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(date, style = MaterialTheme.typography.labelLarge, color = colors.onSurfaceVariant)
            Spacer(Modifier.height(2.dp))
            Text(greeting, style = MaterialTheme.typography.headlineMedium, color = colors.onBackground)
        }
        Box(
            modifier = Modifier
                .size(48.dp)
                .shadow(10.dp, RoundedCornerShape(16.dp), spotColor = Color(0xFF5B47E0))
                .clip(RoundedCornerShape(16.dp))
                .background(BrandGradient),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                painter = painterResource(R.drawable.ic_launcher_foreground),
                contentDescription = "Nudge",
                modifier = Modifier.requiredSize(74.dp),
            )
        }
    }
}

@Composable
private fun SummaryCard(active: List<Task>, finished: List<Task>, now: Long, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val today = LocalDate.now()
    val ringing = active.count { it.dueAt <= now }
    val next = active.firstOrNull { it.dueAt > now }
    val doneToday = finished.count { it.status == TaskStatus.DONE && it.completedAt?.toLocalDateTime()?.toLocalDate() == today }
    val leftToday = active.count { !it.dueAt.toLocalDateTime().toLocalDate().isAfter(today) }
    val total = doneToday + leftToday
    val progress by animateFloatAsState(if (total == 0) 0f else doneToday / total.toFloat(), tween(900, easing = EaseOutQuint), label = "progress")

    val (eyebrow, title, subtitle) = when {
        ringing > 0 -> Triple(
            "NEEDS YOUR ATTENTION",
            if (ringing == 1) "1 reminder is ringing" else "$ringing reminders are ringing",
            "Tick them off to clear your notifications",
        )
        next != null -> Triple("NEXT UP", next.title, "${formatDue(context, next.dueAt)} · ${formatRelative(next.dueAt, now)}")
        else -> Triple("ALL CLEAR", "Nothing on your plate", "Tap + to set your next reminder")
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .shadow(18.dp, RoundedCornerShape(28.dp), spotColor = Color(0xFF5B47E0), ambientColor = Color(0xFF5B47E0))
            .clip(RoundedCornerShape(28.dp))
            .background(BrandGradient)
            .drawBehind {
                drawCircle(Color.White.copy(alpha = 0.07f), radius = 110.dp.toPx(), center = Offset(size.width - 10.dp.toPx(), -20.dp.toPx()))
                drawCircle(Color.White.copy(alpha = 0.05f), radius = 60.dp.toPx(), center = Offset(size.width * 0.55f, size.height + 18.dp.toPx()))
                drawCircle(Color(0xFFFF7A59).copy(alpha = 0.22f), radius = 70.dp.toPx(), center = Offset(-10.dp.toPx(), size.height + 10.dp.toPx()))
            }
            .padding(20.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (ringing > 0) {
                        PulsingDot()
                        Spacer(Modifier.width(8.dp))
                    }
                    Text(eyebrow, style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.75f))
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    title,
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.82f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.width(16.dp))
            Box(Modifier.size(78.dp), contentAlignment = Alignment.Center) {
                Canvas(Modifier.fillMaxSize()) {
                    val stroke = 7.dp.toPx()
                    val inset = stroke / 2
                    val arcSize = androidx.compose.ui.geometry.Size(size.width - stroke, size.height - stroke)
                    drawArc(Color.White.copy(alpha = 0.18f), 0f, 360f, false, Offset(inset, inset), arcSize, style = Stroke(stroke))
                    drawArc(Color.White, -90f, 360f * progress, false, Offset(inset, inset), arcSize, style = Stroke(stroke, cap = StrokeCap.Round))
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("$doneToday/$total", style = MaterialTheme.typography.titleMedium, color = Color.White)
                    Text("today", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.7f))
                }
            }
        }
    }
}

@Composable
private fun PulsingDot() {
    val transition = rememberInfiniteTransition(label = "dot")
    val scale by transition.animateFloat(0.7f, 1.25f, infiniteRepeatable(tween(700), RepeatMode.Reverse), label = "scale")
    Box(
        Modifier
            .size(8.dp)
            .scale(scale)
            .clip(CircleShape)
            .background(Color(0xFFFFB199)),
    )
}

@Composable
private fun SegmentedTabs(
    pagerState: PagerState,
    activeCount: Int,
    finishedCount: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    BoxWithConstraints(
        modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(CircleShape)
            .background(colors.surfaceContainerHigh)
            .padding(5.dp),
    ) {
        val tabWidth = maxWidth / 2
        val position = pagerState.currentPage + pagerState.currentPageOffsetFraction
        Box(
            Modifier
                .offset(x = tabWidth * position)
                .width(tabWidth)
                .fillMaxHeight()
                .shadow(4.dp, CircleShape)
                .clip(CircleShape)
                .background(colors.surfaceContainerLowest),
        )
        Row(Modifier.fillMaxSize()) {
            listOf("Active" to activeCount, "Completed" to finishedCount).forEachIndexed { index, (label, count) ->
                val selected = (1f - kotlin.math.abs(position - index)).coerceIn(0f, 1f)
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(CircleShape)
                        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onSelect(index) },
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    val textColor = androidx.compose.ui.graphics.lerp(colors.onSurfaceVariant, colors.onSurface, selected)
                    Text(label, style = MaterialTheme.typography.titleSmall, color = textColor)
                    Spacer(Modifier.width(8.dp))
                    val badge = if (index == 0) colors.primary else colors.tertiary
                    Text(
                        count.toString(),
                        style = MaterialTheme.typography.labelMedium,
                        color = androidx.compose.ui.graphics.lerp(colors.onSurfaceVariant, if (index == 0) colors.onPrimary else colors.onTertiary, selected),
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(androidx.compose.ui.graphics.lerp(colors.outlineVariant, badge, selected))
                            .padding(horizontal = 8.dp, vertical = 1.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun ActiveList(
    loaded: Boolean,
    rows: List<ListRow>,
    now: Long,
    highlightedId: Long?,
    listState: LazyListState,
    contentPadding: PaddingValues,
    onEdit: (Task) -> Unit,
    onFinish: (Task, TaskStatus) -> Unit,
) {
    if (!loaded) return
    if (rows.isEmpty()) {
        EmptyState(
            icon = Icons.Rounded.NotificationsNone,
            title = "All clear",
            message = "Tap the + button to create a reminder.\nIt will stay pinned in your notifications until you tick it off.",
        )
        return
    }
    LazyColumn(
        state = listState,
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        items(rows, key = { it.key }) { row ->
            when (row) {
                is HeaderRow -> SectionHeader(
                    row.title,
                    row.count,
                    modifier = Modifier.animateItem(),
                    accent = if (row.urgent) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant,
                )
                is TaskRow -> ActiveTaskCard(
                    task = row.task,
                    now = now,
                    highlighted = row.task.id == highlightedId,
                    onClick = { onEdit(row.task) },
                    onFinish = { status -> onFinish(row.task, status) },
                    modifier = Modifier.animateItem(),
                )
            }
        }
    }
}

@Composable
private fun FinishedList(
    loaded: Boolean,
    rows: List<ListRow>,
    contentPadding: PaddingValues,
    onRestore: (Task) -> Unit,
    onDelete: (Task) -> Unit,
    onClearAll: () -> Unit,
) {
    if (!loaded) return
    if (rows.isEmpty()) {
        EmptyState(
            icon = Icons.Rounded.DoneAll,
            title = "Nothing completed yet",
            message = "Reminders you tick off or skip will land here, so you can look back or bring them back.",
        )
        return
    }
    var confirmClear by remember { mutableStateOf(false) }
    LazyColumn(
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        item(key = "clear") {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = { confirmClear = true }) {
                    Text("Clear all", color = MaterialTheme.colorScheme.error)
                }
            }
        }
        items(rows, key = { it.key }) { row ->
            when (row) {
                is HeaderRow -> SectionHeader(row.title, row.count, Modifier.animateItem())
                is TaskRow -> FinishedTaskCard(
                    task = row.task,
                    onRestore = { onRestore(row.task) },
                    onDelete = { onDelete(row.task) },
                    modifier = Modifier.animateItem(),
                )
            }
        }
    }
    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text("Clear completed?") },
            text = { Text("This permanently removes everything in the Completed list.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmClear = false
                    onClearAll()
                }) { Text("Clear all", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { confirmClear = false }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun EmptyState(icon: ImageVector, title: String, message: String) {
    val colors = MaterialTheme.colorScheme
    val float = rememberInfiniteTransition(label = "float")
    val offset by float.animateFloat(-6f, 6f, infiniteRepeatable(tween(2200), RepeatMode.Reverse), label = "offset")
    val halo by float.animateFloat(0.9f, 1.08f, infiniteRepeatable(tween(2200), RepeatMode.Reverse), label = "halo")
    Column(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 40.dp)
            .padding(bottom = 80.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(Modifier.size(170.dp), contentAlignment = Alignment.Center) {
            Canvas(
                Modifier
                    .fillMaxSize()
                    .scale(halo),
            ) {
                drawCircle(colors.primary.copy(alpha = 0.06f))
                drawCircle(colors.primary.copy(alpha = 0.09f), radius = size.minDimension * 0.36f)
                drawCircle(colors.secondary.copy(alpha = 0.9f), radius = 5.dp.toPx(), center = Offset(size.width * 0.82f, size.height * 0.28f))
                drawCircle(colors.primary.copy(alpha = 0.5f), radius = 3.dp.toPx(), center = Offset(size.width * 0.16f, size.height * 0.7f))
                drawCircle(colors.tertiary.copy(alpha = 0.7f), radius = 4.dp.toPx(), center = Offset(size.width * 0.24f, size.height * 0.2f))
            }
            Box(
                Modifier
                    .offset(y = offset.dp)
                    .size(76.dp)
                    .shadow(16.dp, RoundedCornerShape(26.dp), spotColor = Color(0xFF5B47E0))
                    .clip(RoundedCornerShape(26.dp))
                    .background(BrandGradient),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(36.dp))
            }
        }
        Spacer(Modifier.height(18.dp))
        Text(title, style = MaterialTheme.typography.titleLarge, color = colors.onBackground)
        Spacer(Modifier.height(6.dp))
        Text(message, style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant, textAlign = TextAlign.Center)
    }
}

@Composable
private fun NewReminderFab(open: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.9f else 1f, label = "fabScale")
    val rotation by animateFloatAsState(if (open) 135f else 0f, tween(450, easing = EaseOutQuint), label = "fabRotation")
    Box(
        modifier = modifier
            .size(66.dp)
            .scale(scale)
            .shadow(20.dp, RoundedCornerShape(22.dp), spotColor = Color(0xFF5B47E0), ambientColor = Color(0xFF5B47E0))
            .clip(RoundedCornerShape(22.dp))
            .background(BrandGradient)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            Icons.Rounded.Add,
            contentDescription = "New reminder",
            tint = Color.White,
            modifier = Modifier
                .size(32.dp)
                .rotate(rotation),
        )
    }
}
