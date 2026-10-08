package cc.xdan.tempo.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import cc.xdan.tempo.model.*
import cc.xdan.tempo.schedule.ScheduleResolver
import java.util.UUID

@Composable
fun SessionEditor(timetable: Timetable, existing: Session?, initialDay: Int, initialTime: TimeSpan? = null,
    onDismiss: () -> Unit, onSave: (Session) -> Unit, onDelete: (String) -> Unit, onAddSubject: (Subject) -> Unit) {
    var addingSubject by remember { mutableStateOf(false) }
    if (timetable.subjects.isEmpty()) {
        EditorSheet("Your first session", onDismiss) {
            Text("Add a subject or work activity, then schedule it here.")
            Button(onClick = { addingSubject = true }) { Text("Add subject / activity") }
        }
        if (addingSubject) SubjectEditor(null, 0, { addingSubject = false }, { onAddSubject(it); addingSubject = false }, {})
        return
    }
    val id = rememberSaveable { existing?.id ?: UUID.randomUUID().toString() }
    var subject by rememberSaveable { mutableStateOf(existing?.subjectId ?: timetable.subjects.first().id) }
    var day by rememberSaveable { mutableIntStateOf(existing?.day ?: initialDay) }
    val startingTime = existing?.time ?: initialTime ?: timetable.periods.firstOrNull()?.time ?: TimeSpan(540, 630)
    var start by rememberSaveable { mutableStateOf(minuteLabel(startingTime.start)) }
    var end by rememberSaveable { mutableStateOf(minuteLabel(startingTime.end)) }
    var location by rememberSaveable { mutableStateOf(existing?.location ?: "") }
    var notes by rememberSaveable { mutableStateOf(existing?.notes ?: "") }
    var deleteConfirmation by remember { mutableStateOf(false) }
    val time = validSpan(start, end)
    val candidate = time?.let { Session(id, subject, day, it, location.trim(), notes.trim()) }
    val overlapsSession = candidate != null && ScheduleResolver.conflicts(timetable.sessions.filterNot { it.id == id } + candidate).any { it.first.id == id || it.second.id == id }
    val overlapsBreak = time != null && timetable.breaks.any { day in it.days && time.start < it.time.end && it.time.start < time.end }
    EditorSheet(if (existing == null) "Add session" else "Edit session", onDismiss,
        confirm = { candidate?.let(onSave) }, enabled = candidate != null) {
        ChoiceField("Subject / activity", subject, timetable.subjects.map { it.id }, { key -> timetable.subjects.first { it.id == key }.name }, { subject = it })
        ChoiceField("Day", day, (1..7).toList(), { weekdayNames[it - 1] }, { day = it })
        if (timetable.periods.isNotEmpty()) {
            val selected = timetable.periods.firstOrNull { it.time == time }?.id ?: "custom"
            ChoiceField("Usual period", selected, listOf("custom") + timetable.periods.map { it.id }, { key ->
                timetable.periods.firstOrNull { it.id == key }?.let { "${it.name} · ${minuteLabel(it.time.start)}–${minuteLabel(it.time.end)}" } ?: "Custom times"
            }, { key -> timetable.periods.firstOrNull { it.id == key }?.let { start = minuteLabel(it.time.start); end = minuteLabel(it.time.end) } })
        }
        TimeFields(start, end, { start = it }, { end = it })
        if (overlapsSession || overlapsBreak) Text("Overlaps another session or break. Keep it if intentional.", color = MaterialTheme.colorScheme.error)
        val hours = timetable.days.firstOrNull { it.day == day }?.time
        if (time != null && (hours == null || time.start < hours.start || time.end > hours.end)) Text("Outside the configured day hours; this session will still appear.", style = MaterialTheme.typography.bodySmall)
        OutlinedTextField(location, { location = it }, label = { Text("Room / location override") }, placeholder = { Text(timetable.subjects.first { it.id == subject }.location) }, singleLine = true, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(notes, { notes = it }, label = { Text("Notes") }, minLines = 2, modifier = Modifier.fillMaxWidth())
        if (existing != null) TextButton(onClick = { deleteConfirmation = true }) { Text("Delete session", color = MaterialTheme.colorScheme.error) }
    }
    if (deleteConfirmation) AlertDialog(onDismissRequest = { deleteConfirmation = false }, title = { Text("Delete this session?") },
        confirmButton = { TextButton(onClick = { onDelete(id) }) { Text("Delete") } }, dismissButton = { TextButton(onClick = { deleteConfirmation = false }) { Text("Keep") } })
}
