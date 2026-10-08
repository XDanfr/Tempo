package cc.xdan.tempo.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cc.xdan.tempo.model.*
import java.util.UUID

@Composable
fun PeriodSetup(timetable: Timetable, update: ((Timetable) -> Timetable) -> Unit) {
    var periodId by rememberSaveable { mutableStateOf<String?>(null) }
    var showPeriod by rememberSaveable { mutableStateOf(false) }
    var breakId by rememberSaveable { mutableStateOf<String?>(null) }
    var showBreak by rememberSaveable { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Usual periods", style = MaterialTheme.typography.titleLarge)
        Text("Shortcuts for lesson times. Each session can still use custom times.", style = MaterialTheme.typography.bodySmall)
        timetable.periods.sortedBy { it.time.start }.forEach { period ->
            Surface(onClick = { periodId = period.id; showPeriod = true }, modifier = Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.surfaceContainerHigh, shape = MaterialTheme.shapes.medium) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(Icons.Outlined.Schedule, null)
                    Column(Modifier.weight(1f)) { Text(period.name, style = MaterialTheme.typography.titleMedium); Text("${minuteLabel(period.time.start)}–${minuteLabel(period.time.end)}", style = MaterialTheme.typography.bodySmall) }
                    Icon(Icons.Outlined.Edit, "Edit ${period.name}")
                }
            }
        }
        OutlinedButton(onClick = { periodId = null; showPeriod = true }) { Icon(Icons.Outlined.Add, null); Text("Add usual period", Modifier.padding(start = 8.dp)) }
        Text("Break times", Modifier.padding(top = 12.dp), style = MaterialTheme.typography.titleLarge)
        Text("Break, lunch and changeover are separate from free time.", style = MaterialTheme.typography.bodySmall)
        timetable.breaks.sortedBy { it.time.start }.forEach { pause ->
            Surface(onClick = { breakId = pause.id; showBreak = true }, modifier = Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.secondaryContainer, shape = MaterialTheme.shapes.medium) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(pause.kind.icon().vector()!!, null)
                    Column(Modifier.weight(1f)) { Text(pause.name, style = MaterialTheme.typography.titleMedium); Text("${minuteLabel(pause.time.start)}–${minuteLabel(pause.time.end)} · ${pause.days.joinToString { weekdayNames[it - 1].take(3) }}", style = MaterialTheme.typography.bodySmall) }
                    Icon(Icons.Outlined.Edit, "Edit ${pause.name}")
                }
            }
        }
        OutlinedButton(onClick = { breakId = null; showBreak = true }) { Icon(Icons.Outlined.Add, null); Text("Add break time", Modifier.padding(start = 8.dp)) }
    }
    if (showPeriod) PeriodEditor(timetable.periods.firstOrNull { it.id == periodId }, { showPeriod = false },
        { value -> update { it.copy(periods = it.periods.filterNot { p -> p.id == value.id } + value) }; showPeriod = false },
        { id -> update { it.copy(periods = it.periods.filterNot { p -> p.id == id }) }; showPeriod = false })
    if (showBreak) BreakEditor(timetable, timetable.breaks.firstOrNull { it.id == breakId }, dismiss = { showBreak = false },
        save = { value -> update { it.copy(breaks = it.breaks.filterNot { b -> b.id == value.id } + value) }; showBreak = false },
        delete = { id -> update { it.copy(breaks = it.breaks.filterNot { b -> b.id == id }) }; showBreak = false })
}

@Composable
private fun PeriodEditor(period: PeriodTemplate?, dismiss: () -> Unit, save: (PeriodTemplate) -> Unit, delete: (String) -> Unit) {
    val id = rememberSaveable { period?.id ?: UUID.randomUUID().toString() }
    var name by rememberSaveable { mutableStateOf(period?.name ?: "") }
    var start by rememberSaveable { mutableStateOf(minuteLabel(period?.time?.start ?: 540)) }
    var end by rememberSaveable { mutableStateOf(minuteLabel(period?.time?.end ?: 630)) }
    val time = validSpan(start, end)
    EditorSheet(if (period == null) "New usual period" else "Edit usual period", dismiss,
        confirm = { time?.let { save(PeriodTemplate(id, name.trim(), it)) } }, enabled = time != null && name.isNotBlank()) {
        OutlinedTextField(name, { name = it }, label = { Text("Name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        TimeFields(start, end, { start = it }, { end = it })
        Text("Existing sessions keep their saved times when this shortcut changes.", style = MaterialTheme.typography.bodySmall)
        if (period != null) TextButton(onClick = { delete(id) }) { Text("Remove shortcut") }
    }
}
