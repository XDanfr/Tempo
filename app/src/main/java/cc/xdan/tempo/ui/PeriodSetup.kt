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
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
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
                    Box(Modifier.size(12.dp, 36.dp).clip(RoundedCornerShape(6.dp)).background(period.colour?.let { androidx.compose.ui.graphics.Color(it) } ?: MaterialTheme.colorScheme.outlineVariant))
                    Column(Modifier.weight(1f)) { Text(period.name, style = MaterialTheme.typography.titleMedium); Text("${minuteLabel(period.time.start)}–${minuteLabel(period.time.end)}" + if (period.location.isNotBlank()) " · ${period.location}" else "", style = MaterialTheme.typography.bodySmall) }
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
        { id -> update { it.copy(periods = it.periods.filterNot { p -> p.id == id }) }; showPeriod = false }, nextNumber = timetable.periods.size + 1)
    if (showBreak) BreakEditor(timetable, timetable.breaks.firstOrNull { it.id == breakId }, dismiss = { showBreak = false },
        save = { value -> update { it.copy(breaks = it.breaks.filterNot { b -> b.id == value.id } + value) }; showBreak = false },
        delete = { id -> update { it.copy(breaks = it.breaks.filterNot { b -> b.id == id }) }; showBreak = false })
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PeriodEditor(period: PeriodTemplate?, dismiss: () -> Unit, save: (PeriodTemplate) -> Unit, delete: (String) -> Unit, nextNumber: Int = 1) {
    val id = rememberSaveable { period?.id ?: UUID.randomUUID().toString() }
    var name by rememberSaveable { mutableStateOf(period?.name ?: "Period $nextNumber") }
    var start by rememberSaveable { mutableStateOf(minuteLabel(period?.time?.start ?: 540)) }
    var end by rememberSaveable { mutableStateOf(minuteLabel(period?.time?.end ?: 630)) }
    var location by rememberSaveable { mutableStateOf(period?.location ?: "") }
    var colour by rememberSaveable { mutableStateOf(period?.colour) }
    val time = validSpan(start, end)
    val draft = listOf(name.trim(), time, location.trim(), colour)
    val initial = remember { draft }
    EditorSheet(if (period == null) "New period" else "Edit ${period.name}", dismiss,
        confirm = { time?.let { save(PeriodTemplate(id, name.trim(), it, location.trim(), colour)) } }, enabled = time != null && name.isNotBlank(),
        dirty = draft != initial) {
        OutlinedTextField(name, { name = it }, label = { Text("Name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        TimeFields(start, end, { start = it }, { end = it })
        OutlinedTextField(location, { location = it }, label = { Text("Room or location") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        Text("Colour", style = MaterialTheme.typography.labelLarge)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Surface(onClick = { colour = null }, shape = CircleShape, color = MaterialTheme.colorScheme.surfaceContainerHighest,
                border = androidx.compose.foundation.BorderStroke(if (colour == null) 3.dp else 1.dp, MaterialTheme.colorScheme.outline), modifier = Modifier.size(44.dp)) {
                Box(contentAlignment = Alignment.Center) { Icon(Icons.Outlined.FormatColorReset, "No colour", Modifier.size(20.dp)) }
            }
            cc.xdan.tempo.design.subjectColours.forEach { option ->
                Surface(onClick = { colour = option.argb }, shape = CircleShape, color = androidx.compose.ui.graphics.Color(option.argb),
                    border = if (colour == option.argb) androidx.compose.foundation.BorderStroke(3.dp, MaterialTheme.colorScheme.onSurface) else null, modifier = Modifier.size(44.dp)) {
                    Box(contentAlignment = Alignment.Center) { if (colour == option.argb) Icon(Icons.Outlined.Check, option.name, tint = cc.xdan.tempo.design.subjectForeground(androidx.compose.ui.graphics.Color(option.argb))) }
                }
            }
        }
        Text("Existing sessions keep their saved times when a period changes. New ones in this period use its room.", style = MaterialTheme.typography.bodySmall)
        if (period != null) DeleteButton("Delete period", { delete(id) })
    }
}
