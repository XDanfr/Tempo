package cc.xdan.tempo.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import cc.xdan.tempo.design.TempoMotion
import cc.xdan.tempo.design.rememberTempoHaptics
import cc.xdan.tempo.design.subjectForeground
import cc.xdan.tempo.design.subjectOutline
import cc.xdan.tempo.model.*
import cc.xdan.tempo.schedule.*
import kotlinx.coroutines.delay
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

private const val DayPages = 20_001
private const val CentrePage = DayPages / 2
private const val WeekPages = 2_001
private const val CentreWeek = WeekPages / 2

/**
 * The single day-first Schedule destination. It opens on today, pages between real dates and
 * keeps the week strip, header and editors pointed at the same selected date.
 */
@Composable
fun ScheduleScreen(timetable: Timetable, now: LocalDateTime, date: LocalDate, selectDate: (LocalDate) -> Unit,
    edit: (Session) -> Unit, editFree: (LocalDate, TimeSpan) -> Unit, editBreak: (ScheduledBreak) -> Unit,
    onTopChanged: (Boolean) -> Unit = {}) {
    val today = now.toLocalDate()
    // Pages are counted from the day the screen opened, so page numbers stay stable while it is shown.
    val anchor = remember { today }
    fun pageOf(day: LocalDate) = (CentrePage + ChronoUnit.DAYS.between(anchor, day)).toInt().coerceIn(0, DayPages - 1)
    fun dateOf(page: Int): LocalDate = anchor.plusDays((page - CentrePage).toLong())
    val pager = rememberPagerState(initialPage = pageOf(date)) { DayPages }
    val haptics = rememberTempoHaptics()
    val latestSelect by rememberUpdatedState(selectDate)
    val latestDate by rememberUpdatedState(date)
    LaunchedEffect(pager) {
        snapshotFlow { pager.settledPage }.collect { page ->
            val settled = dateOf(page)
            if (settled != latestDate) { haptics.tick(); latestSelect(settled) }
        }
    }
    LaunchedEffect(date) {
        val target = pageOf(date)
        if (pager.currentPage != target && !pager.isScrollInProgress) {
            if (kotlin.math.abs(pager.currentPage - target) <= 7) pager.animateScrollToPage(target, animationSpec = TempoMotion.slowSpatial())
            else pager.scrollToPage(target)
        }
    }
    Column(Modifier.fillMaxSize()) {
        ScheduleHeader(timetable, date, today, selectDate)
        HorizontalPager(pager, Modifier.fillMaxSize(), beyondViewportPageCount = 1, key = { it }) { page ->
            val pageDate = dateOf(page)
            DayTimeline(timetable, pageDate, now, today, edit, { editFree(pageDate, it) }, editBreak, selectDate,
                onTopChanged = { if (page == pager.settledPage) onTopChanged(it) })
        }
    }
}

@Composable
private fun ScheduleHeader(timetable: Timetable, date: LocalDate, today: LocalDate, select: (LocalDate) -> Unit) {
    var choosing by remember { mutableStateOf(false) }
    val haptics = rememberTempoHaptics()
    val spokenDate = "${weekdayNames[date.dayOfWeek.value - 1]}, ${friendlyDate(date)}. Choose a date"
    Column(Modifier.fillMaxWidth().padding(top = 4.dp)) {
        Row(Modifier.fillMaxWidth().padding(start = 12.dp, end = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            // The date is the jump-to-date control: one obvious place, no separate calendar button.
            Surface(onClick = { choosing = true }, shape = MaterialTheme.shapes.small, color = Color.Transparent,
                modifier = Modifier.weight(1f, fill = false).semantics { contentDescription = spokenDate }) {
                Row(Modifier.padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column {
                        AnimatedContent(date, label = "weekday", transitionSpec = { fadeIn(TempoMotion.effects()) togetherWith fadeOut(TempoMotion.exit()) }) {
                            Text(relativeDayName(it, today), style = MaterialTheme.typography.headlineMedium, modifier = Modifier.semantics { heading() })
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(friendlyDate(date), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Icon(Icons.Outlined.ExpandMore, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
            Spacer(Modifier.weight(1f))
            // "Today" only exists while you are somewhere else.
            AnimatedVisibility(date != today, enter = fadeIn(TempoMotion.effects()) + scaleIn(TempoMotion.fastSpatial()),
                exit = fadeOut(TempoMotion.exit()) + scaleOut(TempoMotion.exit())) {
                FilledTonalButton(onClick = { select(today) }) {
                    Icon(Icons.Outlined.Today, null, Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text("Today")
                }
            }
        }
        WeekStrip(timetable, date, today, select)
    }
    if (choosing) DateSelectionDialog(date, { choosing = false }) { haptics.tick(); select(it) }
}

private fun relativeDayName(date: LocalDate, today: LocalDate): String = when (date) {
    today -> "Today"
    today.plusDays(1) -> "Tomorrow"
    today.minusDays(1) -> "Yesterday"
    else -> weekdayNames[date.dayOfWeek.value - 1]
}

/** Monday-first week strip. Swipe it to change week; tap a day to open it. */
@Composable
private fun WeekStrip(timetable: Timetable, date: LocalDate, today: LocalDate, select: (LocalDate) -> Unit) {
    val anchorMonday = remember { today.with(DayOfWeek.MONDAY) }
    fun weekOf(day: LocalDate) = (CentreWeek + ChronoUnit.WEEKS.between(anchorMonday, day.with(DayOfWeek.MONDAY))).toInt().coerceIn(0, WeekPages - 1)
    val weeks = rememberPagerState(initialPage = weekOf(date)) { WeekPages }
    val haptics = rememberTempoHaptics()
    val latestDate by rememberUpdatedState(date)
    val latestSelect by rememberUpdatedState(select)
    LaunchedEffect(date) {
        val target = weekOf(date)
        if (weeks.currentPage != target && !weeks.isScrollInProgress) weeks.animateScrollToPage(target, animationSpec = TempoMotion.spatial())
    }
    // Swiping the strip moves to the same weekday in the new week.
    LaunchedEffect(weeks) {
        snapshotFlow { weeks.settledPage }.collect { page ->
            val shown = weekOf(latestDate)
            if (page != shown) { haptics.tick(); latestSelect(latestDate.plusWeeks((page - shown).toLong())) }
        }
    }
    HorizontalPager(weeks, Modifier.fillMaxWidth().padding(vertical = 8.dp), key = { it }) { page ->
        val monday = anchorMonday.plusWeeks((page - CentreWeek).toLong())
        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            (0 until 7).forEach { offset ->
                val day = monday.plusDays(offset.toLong())
                DayCell(day, day == date, day == today, remember(timetable, day) { dayDots(timetable, day) }, Modifier.weight(1f)) {
                    if (day != date) select(day)
                }
            }
        }
    }
}

/** Up to three subject colours for the day, so the strip previews the week at a glance. */
private fun dayDots(timetable: Timetable, day: LocalDate): List<Long> =
    ScheduleResolver.resolve(timetable, day).filterIsInstance<ScheduleBlock.Lesson>().map { it.subject.colour }.distinct().take(3)

@Composable
private fun DayCell(day: LocalDate, selected: Boolean, today: Boolean, dots: List<Long>, modifier: Modifier, onClick: () -> Unit) {
    val container by androidx.compose.animation.animateColorAsState(
        if (selected) MaterialTheme.colorScheme.primary else Color.Transparent, TempoMotion.effects(), label = "day container")
    val content = if (selected) MaterialTheme.colorScheme.onPrimary else if (today) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
    val scale by animateFloatAsState(if (selected) 1f else .92f, TempoMotion.fastSpatial(), label = "day scale")
    Column(modifier.clip(RoundedCornerShape(20.dp)).semantics {
        contentDescription = "${weekdayNames[day.dayOfWeek.value - 1]} ${day.dayOfMonth}" + if (today) ", today" else ""
        stateDescription = if (selected) "Selected" else ""
    }, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(weekdayNames[day.dayOfWeek.value - 1].take(1), style = MaterialTheme.typography.labelMedium,
            color = if (today) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(4.dp))
        Surface(onClick = onClick, shape = CircleShape, color = container, contentColor = content,
            border = if (today && !selected) BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null,
            modifier = Modifier.size(40.dp).graphicsScale(scale)) {
            Box(contentAlignment = Alignment.Center) {
                Text(day.dayOfMonth.toString(), style = MaterialTheme.typography.titleSmall, fontWeight = if (selected || today) FontWeight.Bold else FontWeight.Medium)
            }
        }
        Spacer(Modifier.height(4.dp))
        Row(Modifier.height(6.dp), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            dots.forEach { Box(Modifier.size(5.dp).clip(CircleShape).background(Color(it))) }
        }
    }
}

private fun Modifier.graphicsScale(scale: Float) = graphicsLayer { scaleX = scale; scaleY = scale }

@Composable
private fun DayTimeline(timetable: Timetable, date: LocalDate, now: LocalDateTime, today: LocalDate,
    edit: (Session) -> Unit, editFree: (TimeSpan) -> Unit, editBreak: (ScheduledBreak) -> Unit, selectDate: (LocalDate) -> Unit,
    onTopChanged: (Boolean) -> Unit) {
    val blocks = remember(timetable, date) { ScheduleResolver.resolve(timetable, date) }
    val lessons = blocks.filterIsInstance<ScheduleBlock.Lesson>()
    val isToday = date == today
    val minute = now.hour * 60 + now.minute
    val conflicts = remember(timetable, date) { ScheduleResolver.blockConflicts(ScheduleResolver.resolvedTemplate(timetable, date), date.dayOfWeek.value).isNotEmpty() }
    val list = rememberLazyListState()
    val latestTop by rememberUpdatedState(onTopChanged)
    LaunchedEffect(list) { snapshotFlow { list.firstVisibleItemIndex == 0 && list.firstVisibleItemScrollOffset < 24 }.collect { latestTop(it) } }
    LazyColumn(Modifier.fillMaxSize(), state = list,
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 120.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item(key = "summary") { DaySummary(timetable, date, lessons) }
        if (isToday) item(key = "now") { NowCard(timetable, blocks, date, edit, editFree, editBreak, selectDate) }
        if (conflicts) item(key = "conflicts") {
            Surface(color = MaterialTheme.colorScheme.errorContainer, shape = MaterialTheme.shapes.medium) {
                Row(Modifier.fillMaxWidth().padding(14.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.WarningAmber, null, tint = MaterialTheme.colorScheme.onErrorContainer)
                    Text("Some sessions or breaks overlap. Tap one to adjust its times.", color = MaterialTheme.colorScheme.onErrorContainer, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
        if (blocks.isEmpty()) item(key = "empty") { EmptyDay(timetable, date) }
        val beforeDay = isToday && blocks.isNotEmpty() && minute < blocks.first().time.start
        val afterDay = isToday && blocks.isNotEmpty() && minute >= blocks.last().time.end
        if (beforeDay) item(key = "now-line-start") { NowLine(minute) }
        items(blocks, key = { blockKey(it) }) { block ->
            val past = date < today || (isToday && block.time.end <= minute)
            val current = isToday && minute >= block.time.start && minute < block.time.end
            val progress = if (current) (minute - block.time.start).toFloat() / block.time.minutes else 0f
            TimelineRow(block, timetable.appearance, past, current, progress, Modifier.animateItem(
                fadeInSpec = TempoMotion.effects(), placementSpec = TempoMotion.spatial(), fadeOutSpec = TempoMotion.exit()),
                edit, editFree, editBreak)
        }
        if (afterDay) item(key = "now-line-end") { NowLine(minute) }
    }
}

private fun blockKey(block: ScheduleBlock) = when (block) {
    is ScheduleBlock.Lesson -> "session-${block.session.id}"
    is ScheduleBlock.Break -> "break-${block.scheduled.id}"
    is ScheduleBlock.Free -> "free-${block.time.start}"
}

@Composable
private fun DaySummary(timetable: Timetable, date: LocalDate, lessons: List<ScheduleBlock.Lesson>) {
    val minutes = lessons.sumOf { it.time.minutes }
    val exception = timetable.exceptions.firstOrNull { it.date == date.toString() }
    val holiday = if (exception == null) timetable.holidays.firstOrNull {
        !date.isBefore(LocalDate.parse(it.startDate)) && !date.isAfter(LocalDate.parse(it.endDate))
    } else null
    if (lessons.isEmpty() && timetable.cycle.weeks <= 1 && exception == null && holiday == null) return
    Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 2.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        // Empty days say so once, in the empty state below; the summary only counts real sessions.
        Text(if (lessons.isEmpty()) "" else "${countLabel(lessons.size, "session")} · ${durationLabel(minutes)} scheduled",
            style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
        // Cycle weeks only matter when the timetable actually alternates.
        if (timetable.cycle.weeks > 1) InfoPill("Week ${ScheduleResolver.cycleWeek(timetable, date)} of ${timetable.cycle.weeks}")
        when {
            exception != null -> InfoPill(if (exception.closed) exception.name.ifBlank { "Day off" } else exception.name.ifBlank { "Changed day" }, MaterialTheme.colorScheme.tertiaryContainer)
            holiday != null -> InfoPill(holiday.name.ifBlank { "Holiday" }, MaterialTheme.colorScheme.tertiaryContainer)
        }
    }
}

@Composable
private fun InfoPill(text: String, color: Color = MaterialTheme.colorScheme.surfaceContainerHigh) {
    Surface(shape = CircleShape, color = color) {
        Text(text, Modifier.padding(horizontal = 10.dp, vertical = 4.dp), style = MaterialTheme.typography.labelMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/** "1h 30m", "45m" or "2h". */
fun durationLabel(minutes: Int): String = when {
    minutes >= 60 && minutes % 60 == 0 -> "${minutes / 60}h"
    minutes >= 60 -> "${minutes / 60}h ${minutes % 60}m"
    else -> "${minutes}m"
}

@Composable
private fun EmptyDay(timetable: Timetable, date: LocalDate) {
    val closed = timetable.exceptions.firstOrNull { it.date == date.toString() }?.closed == true
    Column(Modifier.fillMaxWidth().padding(vertical = 48.dp, horizontal = 24.dp), horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Surface(shape = CircleShape, color = MaterialTheme.colorScheme.secondaryContainer, modifier = Modifier.size(72.dp)) {
            Box(contentAlignment = Alignment.Center) { Icon(Icons.Outlined.EventAvailable, null, Modifier.size(32.dp)) }
        }
        Text(if (closed) "A day off" else "A little breathing room", style = MaterialTheme.typography.titleLarge)
        Text(if (closed) "Enjoy the time off." else "Tap Add session to plan something.",
            style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center)
    }
}

@Composable
private fun NowLine(minute: Int) {
    val colour = MaterialTheme.colorScheme.primary
    Row(Modifier.fillMaxWidth().padding(vertical = 2.dp).semantics { contentDescription = "Now, ${minuteLabel(minute)}" },
        verticalAlignment = Alignment.CenterVertically) {
        Text(minuteLabel(minute), Modifier.width(RailWidth), style = MaterialTheme.typography.labelMedium, color = colour, fontWeight = FontWeight.Bold)
        Box(Modifier.size(8.dp).clip(CircleShape).background(colour))
        Box(Modifier.weight(1f).height(2.dp).background(colour))
    }
}

private val RailWidth = 52.dp

/** One block with its start time on the rail. The current block carries the moving now-line. */
@Composable
private fun TimelineRow(block: ScheduleBlock, appearance: Appearance, past: Boolean, current: Boolean, progress: Float, modifier: Modifier,
    edit: (Session) -> Unit, editFree: (TimeSpan) -> Unit, editBreak: (ScheduledBreak) -> Unit) {
    val lineColour = MaterialTheme.colorScheme.primary
    val fade by animateFloatAsState(if (past) .5f else 1f, TempoMotion.effects(), label = "past fade")
    val animatedProgress by animateFloatAsState(progress, TempoMotion.spatial(), label = "now line")
    Row(modifier.fillMaxWidth().drawWithContent {
        drawContent()
        if (current) {
            // The marker stays on the rail; inside the block, the elapsed fill meets it at the same height.
            val y = size.height * animatedProgress
            val railEnd = RailWidth.toPx() - 6.dp.toPx()
            drawLine(lineColour, Offset(railEnd, y), Offset(RailWidth.toPx() + 2.dp.toPx(), y), 2.dp.toPx())
            drawCircle(lineColour, 4.dp.toPx(), Offset(railEnd, y))
        }
    }, verticalAlignment = Alignment.Top) {
        Text(minuteLabel(block.time.start), Modifier.width(RailWidth).padding(top = if (block is ScheduleBlock.Free) 14.dp else 12.dp).alpha(fade),
            style = MaterialTheme.typography.labelMedium, color = if (current) lineColour else MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = if (current) FontWeight.Bold else FontWeight.Normal)
        Box(Modifier.weight(1f).alpha(fade)) {
            when (block) {
                is ScheduleBlock.Free -> FreeGap(block, appearance) { editFree(block.time) }
                else -> BlockCard(block, appearance, current, animatedProgress) {
                    when (block) { is ScheduleBlock.Lesson -> edit(block.session); is ScheduleBlock.Break -> editBreak(block.scheduled); else -> Unit }
                }
            }
        }
    }
}

/** Compact, tappable gap: it says how long it is and offers to fill it, without eating the screen. */
@Composable
private fun FreeGap(block: ScheduleBlock.Free, appearance: Appearance, fill: () -> Unit) {
    val outline = MaterialTheme.colorScheme.outline.copy(alpha = .45f)
    Surface(onClick = fill, shape = RoundedCornerShape(18.dp), color = Color.Transparent,
        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).semantics { contentDescription = "${durationLabel(block.time.minutes)} free, ${minuteLabel(block.time.start)} to ${minuteLabel(block.time.end)}. Fill this gap" }
            .drawWithContent {
                drawContent()
                val inset = 1.dp.toPx()
                drawRoundRect(outline, Offset(inset, inset), Size(size.width - inset * 2, size.height - inset * 2), CornerRadius(18.dp.toPx()),
                    style = Stroke(1.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 5.dp.toPx()))))
            }) {
        Row(Modifier.padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            if (appearance.showIcons) Icon(Icons.Outlined.Add, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("${durationLabel(block.time.minutes)} free", Modifier.weight(1f), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("Fill", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
        }
    }
}

/** Colour used for a scheduled break of this kind. */
fun breakColour(kind: BreakKind): Color = when (kind) {
    BreakKind.LUNCH -> Color(0xFFFFD59B); BreakKind.CHANGEOVER -> Color(0xFFACE0D3)
    BreakKind.BREAK -> Color(0xFFFFB6C6); BreakKind.CUSTOM -> Color(0xFFD3BCFF)
}

@Composable
private fun BlockCard(block: ScheduleBlock, appearance: Appearance, current: Boolean, progress: Float, onClick: () -> Unit) {
    val outlined = (if (block is ScheduleBlock.Break) appearance.effectiveBreakStyle else appearance.blockStyle) == BlockStyle.OUTLINED
    val colour = when (block) { is ScheduleBlock.Lesson -> Color(block.subject.colour); is ScheduleBlock.Break -> breakColour(block.scheduled.kind); else -> Color.Transparent }
    val surface = MaterialTheme.colorScheme.surface
    val accent = subjectOutline(colour, surface)
    val fill = colour.compositeOver(surface)
    val foreground = if (outlined) MaterialTheme.colorScheme.onSurface else subjectForeground(fill)
    // Proportional but clamped, so a short break stays readable and a long lesson doesn't dominate.
    val height = (block.time.minutes * 1.1f).coerceIn(if (block is ScheduleBlock.Break) 48f else 64f, 150f).dp
    val compact = block.time.minutes < 40
    val name = when (block) { is ScheduleBlock.Lesson -> block.subject.name; is ScheduleBlock.Break -> block.scheduled.name; else -> "" }
    val elapsed = if (outlined) accent.copy(alpha = .14f) else Color.Black.copy(alpha = .10f)
    Surface(onClick = onClick, color = if (outlined) Color.Transparent else fill, contentColor = foreground,
        shape = RoundedCornerShape(if (compact) 18.dp else 24.dp), border = if (outlined) BorderStroke(if (current) 2.dp else 1.5.dp, accent) else null,
        modifier = Modifier.fillMaxWidth().heightIn(min = height).semantics {
            contentDescription = "$name, ${minuteLabel(block.time.start)} to ${minuteLabel(block.time.end)}" + (if (current) ", happening now" else "") + ". Edit"
        }) {
        Box {
        // Elapsed part of the current block fills from the top, following the vertical time axis,
        // and is clipped by the block's shape.
        if (current) Box(Modifier.matchParentSize()) {
            Box(Modifier.fillMaxWidth().fillMaxHeight(progress.coerceIn(0f, 1f)).background(elapsed))
        }
        Row(Modifier.heightIn(min = height).padding(horizontal = if (compact) 14.dp else 18.dp, vertical = if (compact) 8.dp else 14.dp),
            verticalAlignment = if (compact) Alignment.CenterVertically else Alignment.Top, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            val icon = when (block) { is ScheduleBlock.Lesson -> block.subject.icon; is ScheduleBlock.Break -> block.scheduled.kind.icon(); else -> SubjectIcon.NONE }
            if (appearance.showIcons) icon.vector()?.let { Icon(it, null, Modifier.size(if (compact) 20.dp else 24.dp), tint = if (outlined) accent else LocalContentColor.current) }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(name, style = if (compact) MaterialTheme.typography.labelLarge else MaterialTheme.typography.titleMedium,
                    maxLines = if (block.time.minutes < 75) 1 else 2, overflow = TextOverflow.Ellipsis)
                Text("${minuteLabel(block.time.start)}–${minuteLabel(block.time.end)} · ${durationLabel(block.time.minutes)}",
                    style = if (compact) MaterialTheme.typography.labelSmall else MaterialTheme.typography.labelLarge)
                if (!compact && appearance.showLocations && block is ScheduleBlock.Lesson && block.location.isNotBlank())
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Icon(Icons.Outlined.Place, null, Modifier.size(14.dp)); Text(block.location, style = MaterialTheme.typography.bodySmall)
                    }
            }
            if (current) Surface(shape = CircleShape, color = foreground.copy(alpha = .14f), contentColor = foreground) {
                Text("Now", Modifier.padding(horizontal = 10.dp, vertical = 3.dp), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
            }
        }
        }
    }
}

private sealed interface NowState {
    data class During(val block: ScheduleBlock, val next: ScheduleBlock.Lesson?) : NowState
    data class Upcoming(val next: ScheduleBlock.Lesson) : NowState
    data class Done(val date: LocalDate, val finishedAt: Int, val nextDay: Pair<LocalDate, ScheduleBlock.Lesson>?) : NowState
    /** Nothing today: the card only appears to point at the next session on a later day. */
    data class Nothing(val date: LocalDate, val nextDay: Pair<LocalDate, ScheduleBlock.Lesson>) : NowState
}

/** First lesson on a later date within the next fortnight, for "what's next" after today. */
private fun firstLessonAfter(timetable: Timetable, date: LocalDate): Pair<LocalDate, ScheduleBlock.Lesson>? =
    (1..14L).asSequence().map { date.plusDays(it) }.firstNotNullOfOrNull { day ->
        ScheduleResolver.resolve(timetable, day).filterIsInstance<ScheduleBlock.Lesson>().firstOrNull()?.let { day to it }
    }

/**
 * Live card for today. It ticks every second while visible, shows a progress ring for the
 * current block and opens whatever it is about when tapped.
 */
@Composable
private fun NowCard(timetable: Timetable, blocks: List<ScheduleBlock>, date: LocalDate,
    edit: (Session) -> Unit, editFree: (TimeSpan) -> Unit, editBreak: (ScheduledBreak) -> Unit, selectDate: (LocalDate) -> Unit) {
    val clock by produceState(LocalDateTime.now()) {
        while (true) { value = LocalDateTime.now(); delay(1_000L - System.currentTimeMillis() % 1_000L) }
    }
    if (clock.toLocalDate() != date) return
    val seconds = clock.toLocalTime().toSecondOfDay()
    val minute = seconds / 60
    val lessons = blocks.filterIsInstance<ScheduleBlock.Lesson>()
    val currentBlock = ScheduleResolver.current(blocks, minute)
    val next = ScheduleResolver.nextLesson(blocks, minute)
    val nextDay = remember(timetable, date) { firstLessonAfter(timetable, date) }
    val state: NowState = when {
        // No sessions at all: point at the next one instead of describing an empty day as "free time".
        lessons.isEmpty() -> NowState.Nothing(date, nextDay ?: return)
        // Free time or a break after the last session means the day is done, not "free".
        currentBlock is ScheduleBlock.Lesson || (currentBlock != null && next != null) -> NowState.During(currentBlock, next)
        next != null -> NowState.Upcoming(next)
        else -> NowState.Done(date, lessons.maxOf { it.time.end }, nextDay)
    }
    val colour = when (state) {
        is NowState.During -> when (val b = state.block) { is ScheduleBlock.Lesson -> Color(b.subject.colour); is ScheduleBlock.Break -> breakColour(b.scheduled.kind); else -> null }
        is NowState.Upcoming -> Color(state.next.subject.colour)
        is NowState.Nothing -> Color(state.nextDay.second.subject.colour)
        else -> null
    }
    val container = colour?.compositeOver(MaterialTheme.colorScheme.surface) ?: MaterialTheme.colorScheme.primaryContainer
    val animatedContainer by androidx.compose.animation.animateColorAsState(container, TempoMotion.effects(), label = "now colour")
    val content = if (colour != null) subjectForeground(container) else MaterialTheme.colorScheme.onPrimaryContainer
    val onClick: (() -> Unit)? = when (state) {
        is NowState.During -> when (val b = state.block) {
            is ScheduleBlock.Lesson -> ({ edit(b.session) }); is ScheduleBlock.Break -> ({ editBreak(b.scheduled) }); is ScheduleBlock.Free -> ({ editFree(b.time) })
        }
        is NowState.Upcoming -> ({ edit(state.next.session) })
        is NowState.Done -> state.nextDay?.let { (day, _) -> { selectDate(day) } }
        is NowState.Nothing -> ({ selectDate(state.nextDay.first) })
    }
    val body: @Composable () -> Unit = {
        Row(Modifier.fillMaxWidth().padding(20.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                AnimatedContent(state::class to nowTitle(state), label = "now title", transitionSpec = {
                    (fadeIn(TempoMotion.effects()) + scaleIn(TempoMotion.spatial(), initialScale = .96f)) togetherWith fadeOut(TempoMotion.exit())
                }) { (_, title) ->
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        nowEyebrow(state)?.let { Text(it, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = content.copy(alpha = .8f)) }
                        Text(title, style = MaterialTheme.typography.headlineSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    }
                }
                Text(nowDetail(state, timetable.appearance.showLocations, minute), style = MaterialTheme.typography.bodyMedium)
                nowFollowing(state)?.let {
                    HorizontalDivider(Modifier.padding(vertical = 8.dp), color = content.copy(alpha = .2f))
                    Text(it, style = MaterialTheme.typography.titleSmall)
                }
            }
            NowDial(state, seconds, content)
        }
    }
    val modifier = Modifier.fillMaxWidth().semantics(mergeDescendants = true) {}
    if (onClick != null) Surface(onClick = onClick, color = animatedContainer, contentColor = content, shape = RoundedCornerShape(28.dp), modifier = modifier) { body() }
    else Surface(color = animatedContainer, contentColor = content, shape = RoundedCornerShape(28.dp), modifier = modifier) { body() }
}

/**
 * The eyebrow names the moment and the title names the thing, so they never repeat each other.
 * "Done for today" needs no eyebrow at all.
 */
private fun nowEyebrow(state: NowState): String? = when (state) {
    is NowState.During -> "NOW"
    is NowState.Upcoming -> "UP NEXT"
    is NowState.Nothing -> "NEXT SESSION"
    is NowState.Done -> null
}

private fun nowTitle(state: NowState) = when (state) {
    is NowState.During -> when (val b = state.block) { is ScheduleBlock.Lesson -> b.subject.name; is ScheduleBlock.Break -> b.scheduled.name; else -> "Free time" }
    is NowState.Upcoming -> state.next.subject.name
    is NowState.Done -> "Done for today"
    is NowState.Nothing -> state.nextDay.second.subject.name
}

/** "tomorrow at 09:00" or "Friday at 09:00". */
private fun whenLabel(from: LocalDate, day: LocalDate, minute: Int) =
    (if (day == from.plusDays(1)) "tomorrow" else weekdayNames[day.dayOfWeek.value - 1]) + " at ${minuteLabel(minute)}"

private fun nowDetail(state: NowState, showLocations: Boolean, minute: Int) = when (state) {
    is NowState.During -> {
        val b = state.block
        val place = if (b is ScheduleBlock.Lesson && showLocations && b.location.isNotBlank()) "${b.location} · " else ""
        "${place}until ${minuteLabel(b.time.end)}"
    }
    is NowState.Upcoming -> {
        val place = if (showLocations && state.next.location.isNotBlank()) " · ${state.next.location}" else ""
        "Starts at ${minuteLabel(state.next.time.start)}$place"
    }
    is NowState.Done -> "Finished at ${minuteLabel(state.finishedAt)}"
    is NowState.Nothing -> {
        val (day, lesson) = state.nextDay
        val place = if (showLocations && lesson.location.isNotBlank()) " · ${lesson.location}" else ""
        whenLabel(state.date, day, lesson.time.start).replaceFirstChar { it.titlecase() } + place
    }
}

private fun nowFollowing(state: NowState): String? = when (state) {
    is NowState.During -> state.next?.let { "Then ${it.subject.name} at ${minuteLabel(it.time.start)}" }
    is NowState.Done -> state.nextDay?.let { (day, lesson) -> "Next: ${lesson.subject.name}, ${whenLabel(state.date, day, lesson.time.start)}" }
    is NowState.Nothing, is NowState.Upcoming -> null
}

/** Countdown ring for the current block, a countdown for what's next, or a check when done. */
@Composable
private fun NowDial(state: NowState, seconds: Int, content: Color) {
    Box(Modifier.size(84.dp), contentAlignment = Alignment.Center) {
        when (state) {
            is NowState.During -> {
                val span = state.block.time
                val total = span.minutes * 60f
                val left = (span.end * 60 - seconds).coerceAtLeast(0)
                val progress by animateFloatAsState(((total - left) / total).coerceIn(0f, 1f), TempoMotion.effects(), label = "ring")
                CircularProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxSize(), color = content,
                    trackColor = content.copy(alpha = .18f), strokeWidth = 6.dp, gapSize = 0.dp)
                CountdownText(left, "left", content)
            }
            is NowState.Upcoming -> Box(Modifier.fillMaxSize().border(2.dp, content.copy(alpha = .3f), CircleShape), contentAlignment = Alignment.Center) {
                CountdownText((state.next.time.start * 60 - seconds).coerceAtLeast(0), "to go", content)
            }
            is NowState.Done -> DoneBadge(content)
            is NowState.Nothing -> Icon(Icons.Outlined.EventNote, null, Modifier.size(40.dp), tint = content)
        }
    }
}

@Composable
private fun CountdownText(secondsLeft: Int, suffix: String, content: Color) {
    val minutes = secondsLeft / 60
    val headline = when {
        secondsLeft < 60 -> "${secondsLeft}s"
        minutes >= 60 -> "${minutes / 60}h ${"%02d".format(minutes % 60)}m"
        else -> "${minutes + 1}m"
    }
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(headline, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = content, maxLines = 1)
        Text(suffix, style = MaterialTheme.typography.labelSmall, color = content.copy(alpha = .75f))
    }
}

/** A scalloped badge with a check that springs in once when the day completes. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun DoneBadge(content: Color) {
    var shown by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { shown = true }
    val scale by animateFloatAsState(if (shown) 1f else .4f, TempoMotion.fastSpatial(), label = "done badge")
    val rotation by animateFloatAsState(if (shown) 0f else -60f, TempoMotion.spatial(), label = "done rotation")
    Box(Modifier.size(68.dp).graphicsLayer { scaleX = scale; scaleY = scale; rotationZ = rotation }
        .clip(MaterialShapes.Cookie9Sided.toShape()).background(content.copy(alpha = .18f)), contentAlignment = Alignment.Center) {
        Icon(Icons.Outlined.Check, "Day complete", Modifier.size(34.dp), tint = content)
    }
}
