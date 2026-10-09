package cc.xdan.tempo.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cc.xdan.tempo.model.*
import cc.xdan.tempo.schedule.*
import java.time.LocalDate

@Composable
fun ComparisonScreen(collection: TimetableCollection, dismiss: () -> Unit) {
    var selected by rememberSaveable { mutableStateOf(collection.timetables.take(2).map { it.id }) }
    var day by rememberSaveable { mutableIntStateOf(LocalDate.now().dayOfWeek.value) }
    val tables = collection.timetables.filter { it.id in selected }
    val slices = remember(tables, day) { if (tables.size >= 2) compareTimetables(tables.map { it.timetable }, day) else emptyList() }
    EditorSheet("Compare timetables", dismiss, scrollContent = false) {
        ChoiceField("Day", day, (1..7).toList(), { weekdayNames[it - 1] }, { day = it })
        LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(collection.timetables, key = { it.id }) { entry ->
                Row {
                    Checkbox(entry.id in selected, { checked -> selected = if (checked) selected + entry.id else selected - entry.id })
                    Text(entry.timetable.name, Modifier.padding(top = 12.dp), style = MaterialTheme.typography.titleSmall)
                }
            }
            item {
                Text(if (tables.size < 2) "Select at least two timetables." else "${slices.filter { it.sharedFree }.sumOf { it.time.minutes }} minutes free together", style = MaterialTheme.typography.titleLarge)
                Text("Time outside a timetable’s day hours is not counted as shared free time.", style = MaterialTheme.typography.bodySmall)
            }
            items(slices) { slice ->
                Surface(color = if (slice.sharedFree) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainer, shape = MaterialTheme.shapes.medium) {
                    Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("${minuteLabel(slice.time.start)}–${minuteLabel(slice.time.end)}" + if (slice.sharedFree) " · Free together" else "", style = MaterialTheme.typography.titleMedium)
                        tables.forEachIndexed { index, table ->
                            Text(table.timetable.name + ": " + slice.blocks[index].joinToString(" / ") {
                                when (it) { is ScheduleBlock.Lesson -> it.subject.name; is ScheduleBlock.Break -> it.scheduled.name; is ScheduleBlock.Free -> "Free" }
                            }.ifBlank { "Outside day hours" })
                        }
                    }
                }
            }
        }
    }
}
