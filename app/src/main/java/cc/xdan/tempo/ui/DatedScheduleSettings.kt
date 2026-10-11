package cc.xdan.tempo.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.semantics.Role
import cc.xdan.tempo.model.*
import cc.xdan.tempo.schedule.replacementDay
import java.time.LocalDate
import java.util.UUID

@Composable
fun DatedScheduleSettings(timetable: Timetable, update: ((Timetable) -> Timetable) -> Unit) {
    var cycleEditor by remember { mutableStateOf(false) }
    var holidayEditor by remember { mutableStateOf(false) }
    var holiday by remember { mutableStateOf<Holiday?>(null) }
    var exceptionEditor by remember { mutableStateOf(false) }
    var exception by remember { mutableStateOf<DatedException?>(null) }
    var batchEditor by remember { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Dates and repeating weeks", style = MaterialTheme.typography.titleLarge)
        Text("${timetable.cycle.weeks}-week cycle · week 1 starts in the week of ${timetable.cycle.anchorDate}")
        OutlinedButton(onClick = { cycleEditor = true }, modifier = Modifier.fillMaxWidth()) { Text("Edit week cycle") }
        Text("Holidays close every block, including automatic frees. Dated replacements take priority; the cycle keeps counting through days off.", style = MaterialTheme.typography.bodySmall)
        timetable.holidays.forEach { item ->
            TextButton(onClick = { holiday = item; holidayEditor = true }) { Text("${item.name} · ${item.startDate} to ${item.endDate}") }
        }
        OutlinedButton(onClick = { holiday = null; holidayEditor = true }, modifier = Modifier.fillMaxWidth()) { Text("Add holiday") }
        timetable.exceptions.sortedBy { it.date }.forEach { item ->
            TextButton(onClick = { exception = item; exceptionEditor = true }) { Text("${item.date} · ${item.name.ifBlank { if (item.closed) "Day off" else "Replacement schedule" }}") }
        }
        OutlinedButton(onClick = { exception = null; exceptionEditor = true }, modifier = Modifier.fillMaxWidth()) { Text("Add dated exception") }
        OutlinedButton(onClick = { batchEditor = true }, enabled = timetable.sessions.isNotEmpty(), modifier = Modifier.fillMaxWidth()) { Text("Edit selected recurring sessions") }
    }
    if (cycleEditor) {
        var weeks by rememberSaveable { mutableStateOf(timetable.cycle.weeks.toString()) }
        var anchor by rememberSaveable { mutableStateOf(timetable.cycle.anchorDate) }
        val count = weeks.toIntOrNull()
        val highest = (timetable.sessions.mapNotNull { it.cycleWeek } + timetable.breaks.mapNotNull { it.cycleWeek }).maxOrNull() ?: 1
        val valid = count != null && count in highest..52 && validDate(anchor) != null
        EditorSheet("Repeating weeks", { cycleEditor = false }, confirm = {
            update { it.copy(schemaVersion = 3, cycle = WeekCycle(count!!, anchor)) }; cycleEditor = false
        }, enabled = valid) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                listOf(1, 2, 4).forEach { suggestion ->
                    FilterChip(weeks == suggestion.toString(), { weeks = suggestion.toString() }, label = { Text(if (suggestion == 1) "1 week" else "$suggestion weeks") })
                }
            }
            OutlinedTextField(weeks, { weeks = it }, label = { Text("Number of weeks") }, supportingText = { Text("Choose a suggestion or enter 1–52") },
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                singleLine = true, modifier = Modifier.fillMaxWidth())
            DateField("Date in week 1", anchor) { anchor = it }
            Text("Weeks run Monday to Sunday. Existing sessions repeat every week until you assign them to a cycle week.")
            if (count != null && count < highest) Text("Move sessions and breaks out of week $highest before shortening this cycle.", color = MaterialTheme.colorScheme.error)
        }
    }
    if (holidayEditor) {
        val id = rememberSaveable { holiday?.id ?: UUID.randomUUID().toString() }
        var name by rememberSaveable { mutableStateOf(holiday?.name ?: "") }
        var start by rememberSaveable { mutableStateOf(holiday?.startDate ?: LocalDate.now().toString()) }
        var end by rememberSaveable { mutableStateOf(holiday?.endDate ?: start) }
        val a = validDate(start); val b = validDate(end)
        EditorSheet("Holiday", { holidayEditor = false }, confirm = {
            val item = Holiday(id, name.trim(), start, end)
            update { it.copy(holidays = it.holidays.filterNot { h -> h.id == id } + item) }; holidayEditor = false
        }, enabled = name.isNotBlank() && a != null && b != null && !b.isBefore(a)) {
            OutlinedTextField(name, { name = it }, label = { Text("Holiday name") }, modifier = Modifier.fillMaxWidth())
            DateField("First day (included)", start) { start = it }
            DateField("Last day (included)", end) { end = it }
            if (a != null && b != null && b.isBefore(a)) Text("The last day must follow the first day.", color = MaterialTheme.colorScheme.error)
            if (holiday != null) TextButton(onClick = { update { it.copy(holidays = it.holidays.filterNot { h -> h.id == id }) }; holidayEditor = false }) { Text("Remove holiday") }
        }
    }
    if (exceptionEditor) {
        var date by rememberSaveable { mutableStateOf(exception?.date ?: LocalDate.now().toString()) }
        var name by rememberSaveable { mutableStateOf(exception?.name ?: "") }
        var closed by rememberSaveable { mutableStateOf(exception?.closed ?: true) }
        val parsed = validDate(date)
        val duplicate = timetable.exceptions.any { it.date == date && it.date != exception?.date }
        EditorSheet("Dated exception", { exceptionEditor = false }, confirm = {
            val original = exception
            val chosenDate = date
            val chosenName = name.trim()
            val chosenClosed = closed
            update { table ->
                val replacement = if (chosenClosed) DatedException(chosenDate, chosenName, closed = true)
                else if (original != null && !original.closed) original.copy(name = chosenName)
                else table.copy(exceptions = table.exceptions.filterNot { it.date == chosenDate }, holidays = emptyList()).replacementDay(LocalDate.parse(chosenDate)).copy(name = chosenName)
                table.copy(exceptions = table.exceptions.filterNot { it.date == original?.date || it.date == chosenDate } + replacement)
            }; exceptionEditor = false
        }, enabled = parsed != null && !duplicate) {
            if (exception == null) DateField("Date", date) { date = it } else Text(date)
            OutlinedTextField(name, { name = it }, label = { Text("Reason / label (optional)") }, modifier = Modifier.fillMaxWidth())
            Row(Modifier.fillMaxWidth().toggleable(closed, role = Role.Switch) { closed = it }, verticalAlignment = Alignment.CenterVertically) {
                Text("Day off", Modifier.weight(1f)); Switch(closed, null)
            }
            Text(if (closed) "This date will have no sessions, breaks or frees." else "Start with this date's recurring schedule. Open the date in Timetable to edit its sessions and breaks.")
            if (duplicate) Text("An exception already exists for this date. Edit it from the list.", color = MaterialTheme.colorScheme.error)
            if (exception != null) TextButton(onClick = {
                val originalDate = exception!!.date
                update { it.copy(exceptions = it.exceptions.filterNot { e -> e.date == originalDate }) }; exceptionEditor = false
            }) { Text("Remove exception and restore recurring schedule") }
        }
    }
    if (batchEditor) BatchSessionEditor(timetable, update) { batchEditor = false }
}
