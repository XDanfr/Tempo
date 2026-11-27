package cc.xdan.tempo.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cc.xdan.tempo.design.subjectForeground
import cc.xdan.tempo.model.*
import java.util.UUID

/**
 * Quick way to fill in a week: pick a day, then choose a subject for each period (or add sessions by
 * time when there are no periods). Used in setup and from Home while a timetable is empty.
 */
@Composable
fun WeekBuilder(timetable: Timetable, update: ((Timetable) -> Timetable) -> Unit) {
    val days = timetable.days.map { it.day }.sorted().ifEmpty { (1..5).toList() }
    var day by rememberSaveable { mutableIntStateOf(days.first()) }
    var adding by remember { mutableStateOf(false) }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        days.forEach { d ->
            val selected = d == day
            val count = timetable.sessions.count { it.day == d }
            Surface(onClick = { day = d }, shape = RoundedCornerShape(18.dp), modifier = Modifier.weight(1f),
                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHigh,
                contentColor = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface) {
                Column(Modifier.padding(vertical = 10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(weekdayNames[d - 1].take(3), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                    // A small dot per session added, so progress through the week is visible.
                    Row(Modifier.height(8.dp).padding(top = 3.dp), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                        repeat(count.coerceAtMost(4)) { Box(Modifier.size(4.dp).clip(CircleShape).background(LocalContentColor.current)) }
                    }
                }
            }
        }
    }
    if (timetable.subjects.isEmpty()) {
        Text("Add a ${timetable.subjectWord} first, then come back to place it in your week.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        return
    }
    val periods = timetable.periods.sortedBy { it.time.start }
    if (periods.isNotEmpty()) {
        periods.forEach { period ->
            val existing = timetable.sessions.firstOrNull { it.day == day && it.time == period.time }
            val subject = timetable.subjects.firstOrNull { it.id == existing?.subjectId }
            ChoiceField("${period.name} · ${minuteLabel(period.time.start)}–${minuteLabel(period.time.end)}", subject?.id,
                listOf<String?>(null) + timetable.subjects.map { it.id },
                { id -> timetable.subjects.firstOrNull { it.id == id }?.name ?: "Free" },
                { id -> update { t ->
                    val others = t.sessions.filterNot { it.day == day && it.time == period.time }
                    t.copy(sessions = if (id == null) others else others + Session(existing?.id ?: UUID.randomUUID().toString(), id, day, period.time, location = period.location))
                } },
                leading = { id -> SubjectDot(timetable.subjects.firstOrNull { it.id == id }) })
        }
    } else {
        timetable.sessions.filter { it.day == day }.sortedBy { it.time.start }.forEach { session ->
            val subject = timetable.subjects.firstOrNull { it.id == session.subjectId }
            val fill = Color(subject?.colour ?: 0xFFB7F397).compositeOver(MaterialTheme.colorScheme.surface)
            Surface(shape = RoundedCornerShape(18.dp), color = fill, contentColor = subjectForeground(fill)) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(subject?.name ?: timetable.sessionWord, Modifier.weight(1f), style = MaterialTheme.typography.titleSmall)
                    Text("${minuteLabel(session.time.start)}–${minuteLabel(session.time.end)}", style = MaterialTheme.typography.labelLarge)
                }
            }
        }
        OutlinedButton(onClick = { adding = true }, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Outlined.Add, null); Spacer(Modifier.width(8.dp)); Text("Add a ${timetable.sessionWord} on ${weekdayNames[day - 1]}")
        }
    }
    if (adding) SessionEditor(timetable, null, day, onDismiss = { adding = false },
        onSave = { session -> update { it.copy(sessions = it.sessions.filterNot { s -> s.id == session.id } + session) }; adding = false },
        onDelete = { adding = false }, onAddSubject = { subject -> update { it.copy(subjects = it.subjects + subject) } })
}

@Composable
private fun SubjectDot(subject: Subject?) {
    Box(Modifier.size(18.dp).clip(CircleShape).background(subject?.let { Color(it.colour) } ?: MaterialTheme.colorScheme.outlineVariant))
}
