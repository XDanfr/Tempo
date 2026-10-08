package cc.xdan.tempo.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateContentSize
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.style.TextOverflow
import cc.xdan.tempo.model.*
import cc.xdan.tempo.schedule.*
import java.time.LocalDateTime

@Composable
fun DayScreen(timetable: Timetable, day: Int, now: LocalDateTime, today: Boolean, selectDay: (Int) -> Unit, edit: (Session) -> Unit) {
    val blocks = ScheduleResolver.resolve(timetable, day)
    val lessons = blocks.filterIsInstance<ScheduleBlock.Lesson>()
    val minute = now.hour * 60 + now.minute
    val current = if (day == now.dayOfWeek.value) ScheduleResolver.current(blocks, minute) else null
    val next = if (day == now.dayOfWeek.value) ScheduleResolver.nextLesson(blocks, minute) else null
    Column(Modifier.fillMaxSize()) {
        if (!today) ScrollableTabRow(selectedTabIndex = day - 1, edgePadding = 12.dp) {
            weekdayNames.forEachIndexed { i, label -> Tab(selected = day == i + 1, onClick = { selectDay(i + 1) }, text = { Text(label.take(3)) }) }
        }
        AnimatedContent(targetState = day, label = "selected day") { animatedDay ->
            val dayBlocks = ScheduleResolver.resolve(timetable, animatedDay)
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
                    Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = RoundedCornerShape(28.dp)) {
                        Column(Modifier.fillMaxWidth().padding(24.dp)) {
                            Text("YOUR DAY, IN VIEW", style = MaterialTheme.typography.labelMedium)
                            Spacer(Modifier.height(8.dp))
                            Text(headline, style = MaterialTheme.typography.headlineSmall)
                            val detail = when {
                                current is ScheduleBlock.Lesson -> "${current.location.ifBlank { "No location set" }} · ends ${minuteLabel(current.time.end)}"
                                current is ScheduleBlock.Break -> "Until ${minuteLabel(current.time.end)}"
                                current is ScheduleBlock.Free -> "Free until ${minuteLabel(current.time.end)}"
                                next != null -> "Starts at ${minuteLabel(next.time.start)}"
                                else -> "Make room for what matters."
                            }
                            Text(detail, Modifier.padding(top = 8.dp))
                        }
                    }
                }
                if (ScheduleResolver.conflicts(timetable.sessions.filter { it.day == animatedDay }).isNotEmpty()) item {
                    Text("Some sessions overlap. Tap a session to adjust its times.", color = MaterialTheme.colorScheme.error)
                }
                if (dayBlocks.isEmpty()) item {
                    Column(Modifier.fillMaxWidth().padding(vertical = 32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Outlined.EventAvailable, null, Modifier.size(48.dp))
                        Text("A little breathing room", style = MaterialTheme.typography.titleLarge)
                        Text("Add a session or enable this day in Settings.")
                    }
                }
                items(dayBlocks, key = { when (it) { is ScheduleBlock.Lesson -> it.session.id; is ScheduleBlock.Break -> "break-${it.scheduled.id}"; is ScheduleBlock.Free -> "free-${it.time.start}" } }) { block ->
                    ScheduleItem(block, timetable.appearance.showIcons, block == current, edit)
                }
            }
        }
    }
}

@Composable
private fun ScheduleItem(block: ScheduleBlock, showIcons: Boolean, current: Boolean, edit: (Session) -> Unit) {
    // A shared time scale keeps the 60-minute Tutor / 30-minute Free split proportional.
    val height = (block.time.minutes * 1.6f).coerceAtLeast(48f).dp
    val colour = if (block is ScheduleBlock.Lesson) Color(block.subject.colour) else MaterialTheme.colorScheme.surface
    val content = if (block is ScheduleBlock.Lesson) Color(0xFF172116) else MaterialTheme.colorScheme.onSurfaceVariant
    Box(Modifier.fillMaxWidth().height(height).animateContentSize().clip(RoundedCornerShape(24.dp))) {
        if (block is ScheduleBlock.Free) {
            val outline = MaterialTheme.colorScheme.outline.copy(alpha = .35f)
            Canvas(Modifier.matchParentSize()) {
                val inset = 1.dp.toPx()
                drawRoundRect(outline, topLeft = androidx.compose.ui.geometry.Offset(inset, inset),
                    size = androidx.compose.ui.geometry.Size(size.width - inset * 2, size.height - inset * 2),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(24.dp.toPx()),
                    style = Stroke(width = 1.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 5.dp.toPx()))))
            }
            Row(Modifier.fillMaxSize().padding(horizontal = 18.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Free", color = content, style = MaterialTheme.typography.titleMedium)
                Text("${minuteLabel(block.time.start)}–${minuteLabel(block.time.end)}", color = content, style = MaterialTheme.typography.labelMedium)
            }
        } else if (block is ScheduleBlock.Break) Surface(color = MaterialTheme.colorScheme.secondaryContainer, modifier = Modifier.fillMaxSize()) {
            Row(Modifier.padding(horizontal = 18.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Text(block.scheduled.name, style = MaterialTheme.typography.titleMedium)
                Text("${minuteLabel(block.time.start)}–${minuteLabel(block.time.end)}", style = MaterialTheme.typography.labelMedium)
            }
        } else if (block is ScheduleBlock.Lesson) Surface(color = colour, contentColor = content, modifier = Modifier.fillMaxSize().clickable { edit(block.session) }) {
            val compact = block.time.minutes < 45
            Row(Modifier.padding(horizontal = if (compact) 12.dp else 18.dp, vertical = if (compact) 6.dp else 14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                if (showIcons) block.subject.icon.vector()?.let { Icon(it, null, Modifier.size(26.dp)) }
                Column(Modifier.weight(1f)) {
                    Text(block.subject.name, style = if (compact) MaterialTheme.typography.labelLarge else MaterialTheme.typography.titleMedium, maxLines = if (block.time.minutes < 75) 1 else 2, overflow = TextOverflow.Ellipsis)
                    Text("${minuteLabel(block.time.start)}–${minuteLabel(block.time.end)}", style = if (compact) MaterialTheme.typography.labelSmall else MaterialTheme.typography.labelLarge)
                    if (block.location.isNotBlank() && block.time.minutes >= 75) Text(block.location, style = MaterialTheme.typography.bodySmall)
                    if (current && block.time.minutes >= 60) Text("NOW", style = MaterialTheme.typography.labelSmall)
                }
                Icon(Icons.Outlined.ChevronRight, "Edit session", Modifier.size(20.dp))
            }
        }
    }
}
