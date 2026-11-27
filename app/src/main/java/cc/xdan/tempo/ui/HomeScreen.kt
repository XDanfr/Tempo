package cc.xdan.tempo.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import cc.xdan.tempo.design.TempoMotion
import cc.xdan.tempo.design.subjectForeground
import cc.xdan.tempo.model.*
import cc.xdan.tempo.schedule.*
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * Home: what's next today, who's free with you, your progress and achievements. The full
 * timetable is one tap away in Schedule.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun HomeScreen(timetable: Timetable, now: LocalDateTime, profileName: String, people: List<Pair<String, Timetable>>,
    openSchedule: (LocalDate?) -> Unit, openWeek: () -> Unit, openProfile: () -> Unit, openTimetables: () -> Unit,
    edit: (Session) -> Unit, editFree: (TimeSpan) -> Unit, editBreak: (ScheduledBreak) -> Unit,
    update: ((Timetable) -> Timetable) -> Unit = {}) {
    val context = LocalContext.current
    val today = now.toLocalDate()
    val minute = now.hour * 60 + now.minute
    val blocks = remember(timetable, today) { ScheduleResolver.resolve(timetable, today) }
    val shared = remember(timetable, people, today) { if (people.isEmpty()) emptyList() else sharedFrees(timetable, people, today) }
    // Only the first day that has shared frees: today if any are left, otherwise the next one.
    val upcomingFrees = remember(timetable, people, today, minute) {
        if (people.isEmpty()) emptyList() else (0..6L).asSequence().map { offset ->
            val day = today.plusDays(offset)
            sharedFrees(timetable, people, day).filter { offset > 0 || it.time.end > minute }.map { day to it }
        }.firstOrNull { it.isNotEmpty() }.orEmpty()
    }
    val achievementsUnlocked = remember(timetable) { Achievements.unlocked(context) }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item(key = "greeting") {
            Column(Modifier.padding(horizontal = 4.dp, vertical = 4.dp)) {
                Text(greeting(now) + if (profileName.isNotBlank()) ", ${profileName.substringBefore(' ')}" else "",
                    style = MaterialTheme.typography.headlineMedium, modifier = Modifier.semantics { heading() })
                Text(friendlyDate(today), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        if (timetable.sessions.isEmpty() && timetable.person == null) item(key = "build") {
            var building by remember { mutableStateOf(false) }
            Surface(onClick = { building = true }, shape = RoundedCornerShape(28.dp), color = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer) {
                Row(Modifier.fillMaxWidth().padding(20.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    SpinningShapeBadge(Icons.Outlined.EditCalendar)
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Build your timetable", style = MaterialTheme.typography.titleLarge)
                        Text("Add your ${timetable.sessionWord}s to see what's next, your week and who's free with you.", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
            if (building) EditorSheet("Build your timetable", { building = false }) { WeekBuilder(timetable, update) }
        }
        item(key = "now") { NowCard(timetable, blocks, today, edit, editFree, editBreak, { openSchedule(it) }, shared) }
        item(key = "today") { RestOfToday(timetable, blocks, minute) { openSchedule(today) } }
        item(key = "frees") { FreesCard(timetable, upcomingFrees, people.isNotEmpty(), today, openWeek, openTimetables) }
        if (timetable.progress == ProgressMode.STREAKS) item(key = "progress") { ProgressCard(timetable, today, openProfile) }
        item(key = "achievements") { AchievementsCard(achievementsUnlocked, openProfile) }
    }
}

private fun greeting(now: LocalDateTime) = when (now.hour) { in 5..11 -> "Good morning"; in 12..17 -> "Good afternoon"; else -> "Good evening" }

@Composable
private fun HomeCard(title: String, action: String?, onAction: (() -> Unit)?, content: @Composable ColumnScope.() -> Unit) {
    Surface(shape = RoundedCornerShape(28.dp), color = MaterialTheme.colorScheme.surfaceContainer) {
        Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                if (action != null && onAction != null) TextButton(onClick = onAction) {
                    Text(action); Spacer(Modifier.width(4.dp)); Icon(Icons.AutoMirrored.Outlined.ArrowForward, null, Modifier.size(18.dp))
                }
            }
            content()
        }
    }
}

@Composable
private fun RestOfToday(timetable: Timetable, blocks: List<ScheduleBlock>, minute: Int, open: () -> Unit) {
    // The Now card already shows what's on; this lists only what's still to come.
    val rest = blocks.filterIsInstance<ScheduleBlock.Lesson>().filter { it.time.start > minute }
    HomeCard("Later today", "Timetable", open) {
        if (rest.isEmpty()) Text(if (blocks.any { it is ScheduleBlock.Lesson }) "Nothing else today." else "Nothing scheduled today.",
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        rest.take(4).forEach { lesson ->
            val fill = Color(lesson.subject.colour).compositeOver(MaterialTheme.colorScheme.surface)
            Surface(onClick = open, shape = RoundedCornerShape(18.dp), color = fill, contentColor = subjectForeground(fill)) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (timetable.appearance.showIcons) lesson.subject.icon.vector()?.let { Icon(it, null, Modifier.size(20.dp)) }
                    Text(lesson.subject.name, Modifier.weight(1f), style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text("${minuteLabel(lesson.time.start)}–${minuteLabel(lesson.time.end)}", style = MaterialTheme.typography.labelLarge)
                }
            }
        }
        if (rest.size > 4) Text("and ${rest.size - 4} more", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun FreesCard(timetable: Timetable, frees: List<Pair<LocalDate, SharedFree>>, hasPeople: Boolean, today: LocalDate, openWeek: () -> Unit, openTimetables: () -> Unit) {
    val day = frees.firstOrNull()?.first
    val dayName = when (day) { null -> null; today -> "today"; today.plusDays(1) -> "tomorrow"; else -> weekdayNames[day.dayOfWeek.value - 1] }
    HomeCard(if (dayName != null) "Who's free $dayName" else "Who's free when", if (hasPeople) "Week" else null, if (hasPeople) openWeek else null) {
        when {
            !hasPeople -> {
                Text("Share your timetable or add a friend's link to see when you're both free.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                FilledTonalButton(onClick = openTimetables) { Icon(Icons.Outlined.GroupAdd, null); Spacer(Modifier.width(8.dp)); Text("Share or add a friend") }
            }
            frees.isEmpty() -> Text("No shared frees in the next week.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            else -> frees.forEach { (_, free) ->
                Surface(onClick = openWeek, shape = RoundedCornerShape(18.dp), color = MaterialTheme.colorScheme.surfaceContainerHigh) {
                    Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        PeopleFree(free.people, showText = false)
                        Column(Modifier.weight(1f)) {
                            Text(peopleList(free.people), style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(freeTimeLabel(timetable, free.time) + " · ${durationLabel(free.time.minutes)}", style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ProgressCard(timetable: Timetable, today: LocalDate, openProfile: () -> Unit) {
    val summary = remember(timetable, today) { Progress.summary(timetable, today) }
    HomeCard("Your progress", "Profile", openProfile) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(Icons.Outlined.LocalFireDepartment to "${summary.streak}-day streak", Icons.Outlined.MilitaryTech to "Level ${summary.level}",
                Icons.Outlined.Bolt to "+${summary.xpToday} today").forEach { (icon, label) ->
                Surface(Modifier.weight(1f), shape = RoundedCornerShape(18.dp), color = MaterialTheme.colorScheme.tertiaryContainer,
                    contentColor = MaterialTheme.colorScheme.onTertiaryContainer) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(icon, null, Modifier.size(20.dp)); Spacer(Modifier.weight(1f))
                            if (icon == Icons.Outlined.LocalFireDepartment) StreakBonusTag(summary.multiplier, showIcon = false)
                        }
                        Text(label, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, maxLines = 2, minLines = 2)
                    }
                }
            }
        }
        val fraction by androidx.compose.animation.core.animateFloatAsState(summary.levelXp.toFloat() / summary.levelGoal, TempoMotion.spatial(), label = "level")
        LinearWavyProgressIndicator(progress = { fraction }, modifier = Modifier.fillMaxWidth(), waveSpeed = 0.dp)
        Text("${summary.levelGoal - summary.levelXp} XP to level ${summary.level + 1}", style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun AchievementsCard(unlocked: Map<Achievement, Long>, openProfile: () -> Unit) {
    val latest = unlocked.maxByOrNull { it.value }?.key
    val next = Achievement.entries.firstOrNull { it !in unlocked }
    HomeCard("Achievements · ${unlocked.size} of ${Achievement.entries.size}", "All", openProfile) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            listOfNotNull(latest?.let { it to true }, next?.let { it to false }).forEach { (achievement, done) ->
                Surface(onClick = openProfile, modifier = Modifier.weight(1f), shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surfaceContainerHigh) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        AchievementBadge(achievement, done, size = 44.dp)
                        Text(if (done) "Latest" else "Up next", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(achievement.title, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        if (!done) Text(achievement.detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2)
                    }
                }
            }
        }
    }
}
