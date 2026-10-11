package cc.xdan.tempo.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import cc.xdan.tempo.model.*
import cc.xdan.tempo.schedule.*

@Composable
fun BatchSessionEditor(table: Timetable, update: ((Timetable) -> Timetable) -> Unit, dismiss: () -> Unit) {
    var selected by rememberSaveable { mutableStateOf(emptyList<String>()) }
    var shift by rememberSaveable { mutableStateOf("0") }
    var day by rememberSaveable { mutableIntStateOf(0) }
    var week by rememberSaveable { mutableIntStateOf(-1) }
    var changeLocation by rememberSaveable { mutableStateOf(false) }
    var location by rememberSaveable { mutableStateOf("") }
    val minutes = shift.toIntOrNull()
    val change = minutes?.let { SessionBatchEdit(it, day.takeIf { d -> d != 0 }, week != -1, week.takeIf { w -> w > 0 }, if (changeLocation) location.trim() else null) }
    val preview = change?.let { runCatching { it.apply(table, selected.toSet()) }.getOrNull() }
    val hasChange = preview != null && preview != table
    EditorSheet("Edit selected sessions", dismiss, confirm = {
        update { change!!.apply(it, selected.toSet()) }; dismiss()
    }, confirmLabel = "Apply", enabled = hasChange) {
        Text("Choose the recurring sessions to change. Dated replacements remain independent.")
        TextButton(onClick = { selected = if (selected.size == table.sessions.size) emptyList() else table.sessions.map { it.id } }) {
            Text(if (selected.size == table.sessions.size) "Clear selection" else "Select all")
        }
        table.sessions.sortedWith(compareBy<Session> { it.day }.thenBy { it.time.start }).forEach { session ->
            Row(Modifier.fillMaxWidth().toggleable(session.id in selected) { checked -> selected = if (checked) selected + session.id else selected - session.id }, verticalAlignment = Alignment.CenterVertically) {
                Checkbox(session.id in selected, null)
                Text("${table.subjects.first { it.id == session.subjectId }.name} · ${weekdayNames[session.day - 1].take(3)} ${minuteLabel(session.time.start)} · ${session.cycleWeek?.let { "week $it" } ?: "every week"}")
            }
        }
        Text("${selected.size} selected", style = MaterialTheme.typography.titleMedium)
        OutlinedTextField(shift, { shift = it }, label = { Text("Shift by minutes (negative moves earlier)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        ChoiceField("Move to day", day, (0..7).toList(), { if (it == 0) "Keep current days" else weekdayNames[it - 1] }, { day = it })
        if (table.cycle.weeks > 1) ChoiceField("Cycle week", week, (-1..table.cycle.weeks).toList(), {
            when (it) { -1 -> "Keep current weeks"; 0 -> "Every week"; else -> "Week $it" }
        }, { week = it })
        Row(verticalAlignment = Alignment.CenterVertically) { Text("Replace location overrides", Modifier.weight(1f)); Switch(changeLocation, { changeLocation = it }) }
        if (changeLocation) OutlinedTextField(location, { location = it }, label = { Text("New location (empty uses subject default)") }, modifier = Modifier.fillMaxWidth())
        if (selected.isNotEmpty() && preview == null) Text("The change would create invalid times. All sessions must stay within 00:00–24:00.", color = MaterialTheme.colorScheme.error)
        if (preview != null && ScheduleResolver.conflicts(preview.sessions).any { it.first.id in selected || it.second.id in selected }) Text("The selected change creates overlapping sessions. Keep it if intentional.", color = MaterialTheme.colorScheme.error)
    }
}
