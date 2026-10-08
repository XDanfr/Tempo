package cc.xdan.tempo.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cc.xdan.tempo.model.*
import cc.xdan.tempo.schedule.ScheduleResolver
import java.util.UUID

@Composable
fun SessionEditor(timetable: Timetable, existing: Session?, initialDay: Int, onDismiss: () -> Unit, onSave: (Session) -> Unit, onDelete: (String) -> Unit) {
    if (timetable.subjects.isEmpty()) {
        AlertDialog(onDismissRequest = onDismiss, title = { Text("Add a subject first") }, text = { Text("Open Library to add a subject or work activity, then return to schedule it.") },
            confirmButton = { TextButton(onClick = onDismiss) { Text("Got it") } })
        return
    }
    val id = rememberSaveable { existing?.id ?: UUID.randomUUID().toString() }
    var subject by rememberSaveable { mutableStateOf(existing?.subjectId ?: timetable.subjects.first().id) }
    var day by rememberSaveable { mutableIntStateOf(existing?.day ?: initialDay) }
    var start by rememberSaveable { mutableStateOf(minuteLabel(existing?.time?.start ?: timetable.periods.firstOrNull()?.time?.start ?: 540)) }
    var end by rememberSaveable { mutableStateOf(minuteLabel(existing?.time?.end ?: timetable.periods.firstOrNull()?.time?.end ?: 630)) }
    var location by rememberSaveable { mutableStateOf(existing?.location ?: "") }
    var notes by rememberSaveable { mutableStateOf(existing?.notes ?: "") }
    var deleteConfirmation by remember { mutableStateOf(false) }
    val time = validSpan(start, end)
    val candidate = time?.let { Session(id, subject, day, it, location.trim(), notes.trim()) }
    val overlap = candidate != null && ScheduleResolver.conflicts(timetable.sessions.filterNot { it.id == id } + candidate).any { it.first.id == id || it.second.id == id }
    AlertDialog(onDismissRequest = onDismiss,
        title = { Text(if (existing == null) "Add session" else "Edit session") },
        text = { Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Subject / activity", style = MaterialTheme.typography.labelLarge)
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                timetable.subjects.forEach { s -> FilterChip(subject == s.id, { subject = s.id }, label = { Text(s.name) }) }
            }
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                weekdayNames.forEachIndexed { i, name -> FilterChip(day == i + 1, { day = i + 1 }, label = { Text(name.take(3)) }) }
            }
            if (timetable.periods.isNotEmpty()) {
                Text("Use usual times, then adjust if needed", style = MaterialTheme.typography.labelLarge)
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    timetable.periods.forEach { p -> SuggestionChip(onClick = { start = minuteLabel(p.time.start); end = minuteLabel(p.time.end) }, label = { Text(p.name) }) }
                }
            }
            TimeFields(start, end, { start = it }, { end = it })
            if (overlap) Text("Overlaps another session. You can keep it if intentional.", color = MaterialTheme.colorScheme.error)
            val hours = timetable.days.firstOrNull { it.day == day }?.time
            if (time != null && (hours == null || time.start < hours.start || time.end > hours.end)) Text("This session is outside the configured day hours. It will still appear.", style = MaterialTheme.typography.bodySmall)
            OutlinedTextField(location, { location = it }, label = { Text("Room / location override") }, placeholder = { Text(timetable.subjects.first { it.id == subject }.location) }, singleLine = true)
            OutlinedTextField(notes, { notes = it }, label = { Text("Notes") }, minLines = 2)
            if (existing != null) TextButton(onClick = { deleteConfirmation = true }) { Text("Delete session", color = MaterialTheme.colorScheme.error) }
        } },
        confirmButton = { TextButton(enabled = candidate != null, onClick = { candidate?.let(onSave) }) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } })
    if (deleteConfirmation) AlertDialog(onDismissRequest = { deleteConfirmation = false }, title = { Text("Delete this session?") },
        confirmButton = { TextButton(onClick = { onDelete(id) }) { Text("Delete") } }, dismissButton = { TextButton(onClick = { deleteConfirmation = false }) { Text("Keep") } })
}
