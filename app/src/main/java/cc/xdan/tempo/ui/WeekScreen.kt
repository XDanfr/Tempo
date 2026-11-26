package cc.xdan.tempo.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChevronLeft
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Today
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import cc.xdan.tempo.design.TempoMotion
import cc.xdan.tempo.design.subjectForeground
import cc.xdan.tempo.model.*
import cc.xdan.tempo.schedule.*
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.TextStyle
import java.util.Locale

/** One resolved day of the shown week. */
private data class WeekDay(val date: LocalDate, val blocks: List<ScheduleBlock>)

/** Totals for a set of blocks; [cutoff] counts only time after that moment (for "still ahead"). */
private data class WeekTotals(val sessions: Int, val sessionMinutes: Int, val freeMinutes: Int, val breakMinutes: Int,
    val bySubject: List<Triple<Subject, Int, Int>>)

private fun totals(days: List<WeekDay>, cutoff: LocalDateTime?): WeekTotals {
    var sessions = 0; var sessionMinutes = 0; var free = 0; var breaks = 0
    val subjects = linkedMapOf<String, Triple<Subject, Int, Int>>()
    days.forEach { day ->
        day.blocks.forEach { block ->
            // Only the part of a block after the cutoff counts as still ahead.
            val start = if (cutoff != null && day.date == cutoff.toLocalDate()) maxOf(block.time.start, cutoff.hour * 60 + cutoff.minute) else block.time.start
            val skipped = cutoff != null && day.date < cutoff.toLocalDate()
            val minutes = if (skipped) 0 else (block.time.end - start).coerceAtLeast(0)
            if (minutes == 0) return@forEach
            when (block) {
                is ScheduleBlock.Lesson -> {
                    sessions++; sessionMinutes += minutes
                    val (s, c, m) = subjects[block.subject.id] ?: Triple(block.subject, 0, 0)
                    subjects[block.subject.id] = Triple(s, c + 1, m + minutes)
                }
                is ScheduleBlock.Free -> free += minutes
                is ScheduleBlock.Break -> breaks += minutes
            }
        }
    }
    return WeekTotals(sessions, sessionMinutes, free, breaks, subjects.values.sortedByDescending { it.third })
}

/**
 * Week overview: a summary of the resolved week (whole week or what's still ahead) and a
 * time-aligned grid. Tapping a block opens that day in Schedule.
 */
@Composable
fun WeekScreen(timetable: Timetable, now: LocalDateTime, date: LocalDate, selectDate: (LocalDate) -> Unit, openDay: (LocalDate) -> Unit,
    people: List<Pair<String, Timetable>> = emptyList()) {
    val monday = date.with(DayOfWeek.MONDAY)
    val thisMonday = now.toLocalDate().with(DayOfWeek.MONDAY)
    Column(Modifier.fillMaxSize()) {
        WeekHeader(monday, thisMonday, { selectDate(date.plusWeeks(it.toLong())) }, { selectDate(now.toLocalDate()) })
        AnimatedContent(monday, label = "week", transitionSpec = {
            val direction = if (targetState > initialState) 1 else -1
            (slideInHorizontally(TempoMotion.slide) { it * direction / 4 } + fadeIn(TempoMotion.effects())) togetherWith
                (slideOutHorizontally(TempoMotion.slide) { -it * direction / 6 } + fadeOut(TempoMotion.exit()))
        }) { shownMonday ->
            val days = remember(timetable, shownMonday) { (0 until 7).map { shownMonday.plusDays(it.toLong()) }.map { WeekDay(it, ScheduleResolver.resolve(timetable, it)) } }
            WeekBody(timetable, days, now, shownMonday == thisMonday, shownMonday < thisMonday, openDay, people)
        }
    }
}

@Composable
private fun WeekHeader(monday: LocalDate, thisMonday: LocalDate, move: (Int) -> Unit, thisWeek: () -> Unit) {
    val sunday = monday.plusDays(6)
    val locale = Locale.getDefault()
    val range = if (monday.month == sunday.month) "${monday.dayOfMonth}–${sunday.dayOfMonth} ${sunday.month.getDisplayName(TextStyle.FULL, locale)}"
        else "${monday.dayOfMonth} ${monday.month.getDisplayName(TextStyle.SHORT, locale)} – ${sunday.dayOfMonth} ${sunday.month.getDisplayName(TextStyle.SHORT, locale)}"
    val title = when (monday) { thisMonday -> "This week"; thisMonday.plusWeeks(1) -> "Next week"; thisMonday.minusWeeks(1) -> "Last week"; else -> "Week" }
    Row(Modifier.fillMaxWidth().padding(start = 8.dp, end = 16.dp, top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = { move(-1) }) { Icon(Icons.Outlined.ChevronLeft, "Previous week") }
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.headlineMedium, maxLines = 1, modifier = Modifier.semantics { heading() })
            Text(range, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (monday != thisMonday) FilledTonalButton(onClick = thisWeek) {
            Icon(Icons.Outlined.Today, null, Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text("This week")
        }
        IconButton(onClick = { move(1) }) { Icon(Icons.Outlined.ChevronRight, "Next week") }
    }
}

@Composable
private fun WeekBody(timetable: Timetable, days: List<WeekDay>, now: LocalDateTime, current: Boolean, past: Boolean, openDay: (LocalDate) -> Unit,
    people: List<Pair<String, Timetable>>) {
    // "Still ahead" only means something for the week you're in.
    var ahead by rememberSaveable { mutableStateOf(false) }
    val showAhead = current && ahead
    val stats = remember(days, showAhead, now) { totals(days, if (showAhead) now else null) }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)) {
        if (current) Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(false to "Whole week", true to "Still ahead").forEach { (value, label) ->
                val press = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
                FilterChip(ahead == value, { ahead = value }, label = { Text(label) }, modifier = squishWeight(press), interactionSource = press)
            }
        } else Text(if (past) "This week has passed, so these are its full totals." else "Everything here is still ahead.",
            style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (timetable.progress == ProgressMode.STREAKS) WeekXp(timetable, days, now.toLocalDate())
        StatTiles(stats, timetable.sessionWord)
        var hidden by rememberSaveable { mutableStateOf(emptyList<String>()) }
        val shown = people.filter { it.first !in hidden }
        val shared = remember(timetable, shown, days) { days.associate { it.date to sharedFrees(timetable, shown, it.date) } }
        if (people.isNotEmpty()) SharedFreesCard(people, hidden, { name -> hidden = if (name in hidden) hidden - name else hidden + name }, shared, openDay)
        if (stats.bySubject.isNotEmpty()) SubjectBreakdown(stats)
        Text("Week at a glance", style = MaterialTheme.typography.titleMedium)
        WeekGrid(timetable, days, now, openDay, if (people.isEmpty()) emptyMap() else shared)
    }
}

@Composable
private fun StatTiles(stats: WeekTotals, word: String) {
    val tiles = listOf(
        "${word.replaceFirstChar(Char::titlecase)}s" to stats.sessions.toString(),
        "Scheduled" to durationLabel(stats.sessionMinutes),
        "Free time" to durationLabel(stats.freeMinutes),
        "Breaks" to durationLabel(stats.breakMinutes),
    )
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        tiles.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { (label, value) ->
                    Surface(Modifier.weight(1f), shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surfaceContainerHigh) {
                        Column(Modifier.padding(16.dp).semantics(mergeDescendants = true) {}) {
                            AnimatedContent(value, label = label, transitionSpec = { fadeIn(TempoMotion.effects()) togetherWith fadeOut(TempoMotion.exit()) }) {
                                Text(it, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                            }
                            Text(label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SubjectBreakdown(stats: WeekTotals) {
    val most = stats.bySubject.maxOf { it.third }.coerceAtLeast(1)
    Surface(shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surfaceContainer) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("By subject", style = MaterialTheme.typography.titleMedium)
            stats.bySubject.forEach { (subject, count, minutes) ->
                val share by animateFloatAsState(minutes.toFloat() / most, TempoMotion.spatial(), label = "share")
                Column(Modifier.semantics(mergeDescendants = true) {}, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        subject.icon.vector()?.let { Icon(it, null, Modifier.size(18.dp)) }
                        Text(subject.name, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text("${countLabel(count, "session")} · ${durationLabel(minutes)}", style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Box(Modifier.fillMaxWidth().height(8.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceContainerHighest)) {
                        Box(Modifier.fillMaxWidth(share).fillMaxHeight().clip(CircleShape).background(Color(subject.colour)))
                    }
                }
            }
        }
    }
}

private const val MinuteHeight = 0.9f

/** Time-aligned week grid. Overlapping blocks share their column side by side. */
@Composable
private fun WeekGrid(timetable: Timetable, days: List<WeekDay>, now: LocalDateTime, openDay: (LocalDate) -> Unit,
    shared: Map<LocalDate, List<SharedFree>> = emptyMap()) {
    // Weekend columns appear only when something happens on them.
    val shown = days.filter { it.date.dayOfWeek.value <= 5 || it.blocks.any { b -> b !is ScheduleBlock.Free } }
    val occupied = shown.flatMap { it.blocks }
    if (occupied.isEmpty()) {
        Text("Nothing scheduled this week.", color = MaterialTheme.colorScheme.onSurfaceVariant); return
    }
    val start = (occupied.minOf { it.time.start } / 60) * 60
    val end = ((occupied.maxOf { it.time.end } + 59) / 60) * 60
    val height = ((end - start) * MinuteHeight).dp
    val today = now.toLocalDate()
    val surface = MaterialTheme.colorScheme.surface
    Row(Modifier.fillMaxWidth()) {
        // Hour labels.
        Column(Modifier.width(36.dp)) {
            Spacer(Modifier.height(28.dp))
            Box(Modifier.height(height)) {
                (start until end step 60).forEach { minute ->
                    Text(minuteLabel(minute).take(2), Modifier.offset(y = ((minute - start) * MinuteHeight).dp - 6.dp),
                        style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        shown.forEach { day ->
            val isToday = day.date == today
            Column(Modifier.weight(1f).padding(horizontal = 1.5.dp)) {
                Surface(onClick = { openDay(day.date) }, shape = CircleShape, color = if (isToday) MaterialTheme.colorScheme.primary else Color.Transparent,
                    contentColor = if (isToday) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.fillMaxWidth().height(26.dp).semantics { contentDescription = "Open ${weekdayNames[day.date.dayOfWeek.value - 1]} ${day.date.dayOfMonth}" }) {
                    Box(contentAlignment = Alignment.Center) {
                        Text("${weekdayNames[day.date.dayOfWeek.value - 1].take(1)} ${day.date.dayOfMonth}", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(Modifier.height(2.dp))
                BoxWithConstraints(Modifier.fillMaxWidth().height(height).clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerLow)) {
                    // Shared frees sit behind the blocks as soft bands.
                    shared[day.date].orEmpty().forEach { free ->
                        Box(Modifier.offset(y = ((free.time.start - start) * MinuteHeight).dp).fillMaxWidth().height((free.time.minutes * MinuteHeight).dp)
                            .background(MaterialTheme.colorScheme.tertiary.copy(alpha = .28f)))
                    }
                    val lanes = laneOf(day.blocks.filterNot { it is ScheduleBlock.Free })
                    lanes.forEach { (block, lane, laneCount) ->
                        val colour = when (block) { is ScheduleBlock.Lesson -> Color(block.subject.colour); is ScheduleBlock.Break -> breakColour(block.scheduled.kind); else -> Color.Transparent }
                        val fill = colour.compositeOver(surface)
                        val past = day.date < today || (isToday && block.time.end <= now.hour * 60 + now.minute)
                        val name = when (block) { is ScheduleBlock.Lesson -> block.subject.name; is ScheduleBlock.Break -> block.scheduled.name; else -> "" }
                        val laneWidth = maxWidth / laneCount
                        Surface(onClick = { openDay(day.date) }, color = fill, contentColor = subjectForeground(fill), shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.offset(x = laneWidth * lane, y = ((block.time.start - start) * MinuteHeight).dp)
                                .width(laneWidth).height(((block.time.minutes * MinuteHeight).dp - 2.dp).coerceAtLeast(6.dp)).padding(1.dp)
                                .alpha(if (past) .5f else 1f)
                                .semantics { contentDescription = "$name, ${weekdayNames[day.date.dayOfWeek.value - 1]} ${minuteLabel(block.time.start)} to ${minuteLabel(block.time.end)}. Open day" }) {
                            if (block is ScheduleBlock.Lesson && block.time.minutes >= 30) Text(name, Modifier.padding(4.dp), style = MaterialTheme.typography.labelSmall,
                                maxLines = (block.time.minutes / 20).coerceIn(1, 4), overflow = TextOverflow.Ellipsis)
                        }
                    }
                    if (isToday) {
                        val minute = now.hour * 60 + now.minute
                        if (minute in start..end) Box(Modifier.offset(y = ((minute - start) * MinuteHeight).dp).fillMaxWidth().height(2.dp).background(MaterialTheme.colorScheme.primary))
                    }
                }
            }
        }
    }
}


/** Greedy lanes for overlapping blocks: returns each block with its lane and the lane count of its cluster. */
private fun laneOf(blocks: List<ScheduleBlock>): List<Triple<ScheduleBlock, Int, Int>> {
    val sorted = blocks.sortedBy { it.time.start }
    val result = mutableListOf<Triple<ScheduleBlock, Int, Int>>()
    var cluster = mutableListOf<Pair<ScheduleBlock, Int>>()
    var clusterEnd = -1
    fun flush() { val count = (cluster.maxOfOrNull { it.second } ?: 0) + 1; cluster.forEach { result += Triple(it.first, it.second, count) }; cluster = mutableListOf() }
    val laneEnds = mutableListOf<Int>()
    sorted.forEach { block ->
        if (block.time.start >= clusterEnd) { flush(); laneEnds.clear() }
        val lane = laneEnds.indexOfFirst { it <= block.time.start }.let { if (it == -1) { laneEnds += 0; laneEnds.lastIndex } else it }
        laneEnds[lane] = block.time.end
        cluster += block to lane
        clusterEnd = maxOf(clusterEnd, block.time.end)
    }
    flush()
    return result
}

/** XP earned this week so far and how far it is to the next level. */
@Composable
private fun WeekXp(timetable: Timetable, days: List<WeekDay>, today: LocalDate) {
    val weekXp = remember(timetable, days, today) { days.filter { !it.date.isAfter(today) }.sumOf { Progress.xpForDay(timetable, it.date) } }
    val summary = remember(timetable, today) { Progress.summary(timetable, today) }
    val fraction by animateFloatAsState(summary.levelXp.toFloat() / summary.levelGoal, TempoMotion.spatial(), label = "week xp")
    Surface(shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.tertiaryContainer, contentColor = MaterialTheme.colorScheme.onTertiaryContainer) {
        Column(Modifier.fillMaxWidth().padding(16.dp).semantics(mergeDescendants = true) {}, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text("+$weekXp XP", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Text("Level ${summary.level}", style = MaterialTheme.typography.labelLarge)
            }
            LinearProgressIndicator(progress = { fraction }, modifier = Modifier.fillMaxWidth().height(8.dp),
                color = MaterialTheme.colorScheme.onTertiaryContainer, trackColor = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = .2f))
            Text("Earned this week · ${summary.levelGoal - summary.levelXp} XP to level ${summary.level + 1}", style = MaterialTheme.typography.bodyMedium)
        }
    }
}

/** Who's free when you are this week, with people you can toggle in and out. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SharedFreesCard(people: List<Pair<String, Timetable>>, hidden: List<String>, toggle: (String) -> Unit,
    shared: Map<LocalDate, List<SharedFree>>, openDay: (LocalDate) -> Unit) {
    Surface(shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surfaceContainer) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Frees you share", style = MaterialTheme.typography.titleMedium)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                people.map { it.first }.distinct().forEach { name ->
                    FilterChip(name !in hidden, { toggle(name) }, label = { Text(name) }, leadingIcon = {
                        Box(Modifier.size(18.dp).clip(CircleShape).background(personColour(name)), contentAlignment = Alignment.Center) {
                            Text(name.take(1).uppercase(), style = MaterialTheme.typography.labelSmall, color = subjectForeground(personColour(name)))
                        }
                    })
                }
            }
            val rows = shared.entries.sortedBy { it.key }.flatMap { (day, frees) -> frees.map { day to it } }
            if (rows.isEmpty()) Text("No shared frees this week.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            rows.forEach { (day, free) ->
                Surface(onClick = { openDay(day) }, shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surfaceContainerHigh) {
                    Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Column(Modifier.weight(1f)) {
                            Text("${weekdayNames[day.dayOfWeek.value - 1]} · ${minuteLabel(free.time.start)}–${minuteLabel(free.time.end)}", style = MaterialTheme.typography.titleSmall)
                            Text("${durationLabel(free.time.minutes)} with ${peopleList(free.people)}", style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        PeopleFree(free.people, showText = false)
                    }
                }
            }
        }
    }
}
