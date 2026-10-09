package cc.xdan.tempo.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cc.xdan.tempo.model.*
import java.util.UUID

fun BreakKind.label(): String = when (this) { BreakKind.BREAK -> "Break"; BreakKind.LUNCH -> "Lunch"; BreakKind.CHANGEOVER -> "Changeover"; BreakKind.CUSTOM -> "Custom" }
fun BreakKind.icon(): SubjectIcon = when (this) { BreakKind.BREAK -> SubjectIcon.BREAK; BreakKind.LUNCH -> SubjectIcon.LUNCH; BreakKind.CHANGEOVER -> SubjectIcon.CHANGEOVER; BreakKind.CUSTOM -> SubjectIcon.TIME }

@Composable
fun BreakEditor(timetable: Timetable, existing: ScheduledBreak? = null, initialDay: Int? = null, initialTime: TimeSpan? = null,
    dismiss: () -> Unit, save: (ScheduledBreak) -> Unit, delete: (String) -> Unit) {
    val id = rememberSaveable { existing?.id ?: UUID.randomUUID().toString() }
    var kind by rememberSaveable { mutableStateOf(existing?.kind ?: BreakKind.BREAK) }
    var name by rememberSaveable { mutableStateOf(existing?.name ?: "Break") }
    var days by rememberSaveable { mutableStateOf((existing?.days ?: initialDay?.let { listOf(it) } ?: timetable.days.map { it.day }).joinToString(",")) }
    val selectedDays = days.split(",").mapNotNull { it.toIntOrNull() }
    val time = existing?.time ?: initialTime ?: TimeSpan(630, 645)
    var start by rememberSaveable { mutableStateOf(minuteLabel(time.start)) }
    var end by rememberSaveable { mutableStateOf(minuteLabel(time.end)) }
    var confirmDelete by remember { mutableStateOf(false) }
    val span = validSpan(start, end)
    val overlaps = span != null && (timetable.sessions.any { it.day in selectedDays && it.time.start < span.end && span.start < it.time.end } ||
        timetable.breaks.any { it.id != id && it.days.any { d -> d in selectedDays } && it.time.start < span.end && span.start < it.time.end })
    EditorSheet(if (existing == null) "Add break time" else "Edit break time", dismiss,
        confirm = { span?.let { save(ScheduledBreak(id, name.trim(), selectedDays, it, kind)) } }, enabled = span != null && name.isNotBlank() && selectedDays.isNotEmpty()) {
        ChoiceField("Type", kind, BreakKind.entries, { it.label() }, { chosen -> if (name == kind.label()) name = chosen.label(); kind = chosen })
        OutlinedTextField(name, { name = it }, label = { Text("Name") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
        TimeFields(start, end, { start = it }, { end = it })
        Text("Repeats on", style = MaterialTheme.typography.labelLarge)
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            weekdayNames.forEachIndexed { i, label -> FilterChip(i + 1 in selectedDays, {
                days = (if (i + 1 in selectedDays) selectedDays - (i + 1) else selectedDays + (i + 1)).sorted().joinToString(",")
            }, label = { Text(label.take(3)) }) }
        }
        if (overlaps) Text("Overlaps a session or another break. Keep it if intentional.", color = MaterialTheme.colorScheme.error)
        Text("Breaks are separate from frees and appear in your day timeline.", style = MaterialTheme.typography.bodySmall)
        if (existing != null) TextButton(onClick = { confirmDelete = true }) { Text("Delete break", color = MaterialTheme.colorScheme.error) }
    }
    if (confirmDelete) AlertDialog(onDismissRequest = { confirmDelete = false }, title = { Text("Delete this break?") },
        text = { Text("This removes it from every selected day.") }, confirmButton = { TextButton(onClick = { delete(id) }) { Text("Delete") } },
        dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Keep") } })
}
