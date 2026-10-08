package cc.xdan.tempo.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import cc.xdan.tempo.model.*
import cc.xdan.tempo.schedule.*
import java.time.LocalDateTime

@Composable
fun DayScreen(timetable: Timetable, day: Int, now: LocalDateTime, today: Boolean, selectDay: (Int) -> Unit,
    edit: (Session) -> Unit, editFree: (Int, TimeSpan) -> Unit, editBreak: (ScheduledBreak) -> Unit) {
    Column(Modifier.fillMaxSize()) {
        if (!today) ScrollableTabRow(selectedTabIndex = day - 1, edgePadding = 12.dp) {
            weekdayNames.forEachIndexed { i, label -> Tab(selected = day == i + 1, onClick = { selectDay(i + 1) }, text = { Text(label.take(3)) }) }
        }
        AnimatedContent(targetState = day, label = "selected day", transitionSpec = {
            val direction = if (targetState > initialState) 1 else -1
            (slideInHorizontally(spring(stiffness = 450f, dampingRatio = .9f)) { it * direction / 3 } + fadeIn(tween(180))) togetherWith
                (slideOutHorizontally(tween(160)) { -it * direction / 5 } + fadeOut(tween(120)))
        }) { animatedDay ->
            val dayBlocks = ScheduleResolver.resolve(timetable, animatedDay)
            val lessons = dayBlocks.filterIsInstance<ScheduleBlock.Lesson>()
            val minute = now.hour * 60 + now.minute
            val current = if (animatedDay == now.dayOfWeek.value) ScheduleResolver.current(dayBlocks, minute) else null
            val next = if (animatedDay == now.dayOfWeek.value) ScheduleResolver.nextLesson(dayBlocks, minute) else null
            LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 112.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                item {
                    Text(weekdayNames[animatedDay - 1], style = MaterialTheme.typography.headlineLarge)
                    Text("${lessons.size} sessions · ${lessons.sumOf { it.time.minutes } / 60}h ${lessons.sumOf { it.time.minutes } % 60}m scheduled", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (today) item {
                    val headline = when {
                        current is ScheduleBlock.Lesson -> current.subject.name
                        current is ScheduleBlock.Break -> current.scheduled.name
                        current is ScheduleBlock.Free -> "Time for yourself"
                        lessons.isEmpty() -> "Nothing scheduled today"
                        next != null -> "Up next: ${next.subject.name}"
                        else -> "You're finished for today"
                    }
                    val detail = when {
                        current is ScheduleBlock.Lesson -> "${current.location.ifBlank { "No location set" }} · ends ${minuteLabel(current.time.end)}"
                        current is ScheduleBlock.Break -> "Until ${minuteLabel(current.time.end)}"
                        current is ScheduleBlock.Free -> "Free until ${minuteLabel(current.time.end)}"
                        next != null -> "Starts at ${minuteLabel(next.time.start)}"
                        else -> "Make room for what matters."
                    }
                    val outlined = timetable.appearance.blockStyle == BlockStyle.OUTLINED
                    Surface(color = if (outlined) Color.Transparent else MaterialTheme.colorScheme.primaryContainer, shape = RoundedCornerShape(28.dp),
                        border = if (outlined) BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null) {
                        Column(Modifier.fillMaxWidth().padding(24.dp)) {
                            Text("YOUR DAY, IN VIEW", style = MaterialTheme.typography.labelMedium)
                            Spacer(Modifier.height(8.dp))
                            AnimatedContent(headline to detail, label = "now and next", transitionSpec = {
                                (fadeIn(tween(180)) + scaleIn(initialScale = .96f)) togetherWith fadeOut(tween(100))
                            }) { (title, subtitle) -> Column { Text(title, style = MaterialTheme.typography.headlineSmall); Text(subtitle, Modifier.padding(top = 8.dp)) } }
                        }
                    }
                }
                if (ScheduleResolver.blockConflicts(timetable, animatedDay).isNotEmpty()) item {
                    Text("Some sessions or breaks overlap. Tap a block to adjust its times.", color = MaterialTheme.colorScheme.error)
                }
                if (dayBlocks.isEmpty()) item {
                    Column(Modifier.fillMaxWidth().padding(vertical = 32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Outlined.EventAvailable, null, Modifier.size(48.dp))
                        Text("A little breathing room", style = MaterialTheme.typography.titleLarge)
                        Text("Add a session or enable this day in Settings.")
                    }
                }
                items(dayBlocks, key = { when (it) {
                    is ScheduleBlock.Lesson -> "session-${it.session.id}"
                    is ScheduleBlock.Break -> "break-${it.scheduled.id}"
                    is ScheduleBlock.Free -> "free-${it.time.start}"
                } }) { block ->
                    ScheduleItem(block, timetable.appearance, block == current,
                        Modifier.animateItem(fadeInSpec = tween(160), placementSpec = spring(stiffness = 450f, dampingRatio = .9f), fadeOutSpec = tween(120)),
                        edit, { editFree(animatedDay, it) }, editBreak)
                }
            }
        }
    }
}

@Composable
private fun ScheduleItem(block: ScheduleBlock, appearance: Appearance, current: Boolean, modifier: Modifier,
    edit: (Session) -> Unit, editFree: (TimeSpan) -> Unit, editBreak: (ScheduledBreak) -> Unit) {
    val height = (block.time.minutes * 1.6f).coerceAtLeast(48f).dp
    val shape = RoundedCornerShape(24.dp)
    if (block is ScheduleBlock.Free) {
        val outline = MaterialTheme.colorScheme.outline.copy(alpha = .35f)
        Box(modifier.fillMaxWidth().heightIn(min = height).clip(shape).clickable(role = Role.Button, onClickLabel = "Edit free period", onClick = { editFree(block.time) })) {
            Canvas(Modifier.matchParentSize()) {
                val inset = 1.dp.toPx()
                drawRoundRect(outline, topLeft = androidx.compose.ui.geometry.Offset(inset, inset),
                    size = androidx.compose.ui.geometry.Size(size.width - inset * 2, size.height - inset * 2),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(24.dp.toPx()),
                    style = Stroke(width = 1.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 5.dp.toPx()))))
            }
            Row(Modifier.fillMaxWidth().heightIn(min = height).padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (appearance.showIcons) Icon(Icons.Outlined.AddCircleOutline, null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("Free", Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.titleSmall)
                Text("${minuteLabel(block.time.start)}–${minuteLabel(block.time.end)}", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelMedium)
                Icon(Icons.Outlined.ChevronRight, null, Modifier.size(18.dp))
            }
        }
        return
    }
    val outlined = appearance.blockStyle == BlockStyle.OUTLINED
    val colour = when (block) {
        is ScheduleBlock.Lesson -> Color(block.subject.colour)
        is ScheduleBlock.Break -> when (block.scheduled.kind) { BreakKind.LUNCH -> Color(0xFFFFD59B); BreakKind.CHANGEOVER -> Color(0xFFACE0D3); BreakKind.BREAK -> Color(0xFFFFB6C6); BreakKind.CUSTOM -> Color(0xFFD3BCFF) }
        is ScheduleBlock.Free -> Color.Transparent
    }
    val light = MaterialTheme.colorScheme.background.luminance() > .5f
    val accent = if (light) colour.copy(red = colour.red * .45f, green = colour.green * .45f, blue = colour.blue * .45f) else colour
    val click = { when (block) { is ScheduleBlock.Lesson -> edit(block.session); is ScheduleBlock.Break -> editBreak(block.scheduled); else -> {} } }
    Surface(onClick = click, color = if (outlined) Color.Transparent else colour,
        contentColor = if (outlined) MaterialTheme.colorScheme.onSurface else Color(0xFF172116),
        shape = shape, border = if (outlined) BorderStroke(if (current) 2.dp else 1.5.dp, accent) else null,
        modifier = modifier.fillMaxWidth().heightIn(min = height)) {
        val compact = block.time.minutes < 45
        Row(Modifier.heightIn(min = height).padding(horizontal = if (compact) 12.dp else 18.dp, vertical = if (compact) 6.dp else 14.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            val icon = when (block) { is ScheduleBlock.Lesson -> block.subject.icon; is ScheduleBlock.Break -> block.scheduled.kind.icon(); else -> SubjectIcon.NONE }
            if (appearance.showIcons) icon.vector()?.let { Icon(it, null, Modifier.size(if (compact) 22.dp else 26.dp), tint = if (outlined) accent else LocalContentColor.current) }
            Column(Modifier.weight(1f)) {
                val name = when (block) { is ScheduleBlock.Lesson -> block.subject.name; is ScheduleBlock.Break -> block.scheduled.name; else -> "" }
                Text(name, style = if (compact) MaterialTheme.typography.labelLarge else MaterialTheme.typography.titleMedium,
                    maxLines = if (block.time.minutes < 75) 1 else 2, overflow = TextOverflow.Ellipsis)
                Text("${minuteLabel(block.time.start)}–${minuteLabel(block.time.end)}", style = if (compact) MaterialTheme.typography.labelSmall else MaterialTheme.typography.labelLarge)
                if (block is ScheduleBlock.Lesson && block.location.isNotBlank() && block.time.minutes >= 75) Text(block.location, style = MaterialTheme.typography.bodySmall)
                if (current && block.time.minutes >= 60) Text("NOW", style = MaterialTheme.typography.labelSmall)
            }
            Icon(Icons.Outlined.ChevronRight, "Edit block", Modifier.size(20.dp))
        }
    }
}
