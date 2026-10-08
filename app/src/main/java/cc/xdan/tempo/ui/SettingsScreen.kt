package cc.xdan.tempo.ui

import android.os.Build
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import cc.xdan.tempo.model.*
import java.util.UUID

@Composable
fun SettingsScreen(timetable: Timetable, update: ((Timetable) -> Timetable) -> Unit) {
    var editingDay by remember { mutableStateOf<DayHours?>(null) }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            Text("Your Tempo", style = MaterialTheme.typography.headlineMedium)
            Text("Part of Axis", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        item { AppearanceControls(timetable.appearance) { appearance -> update { it.copy(appearance = appearance) } } }
        item { Text("Day lengths", style = MaterialTheme.typography.titleLarge) }
        items((1..7).toList()) { day ->
            val hours = timetable.days.firstOrNull { it.day == day }
            ElevatedCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(weekdayNames[day - 1], Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
                        Switch(checked = hours != null, onCheckedChange = { enabled ->
                            update { t -> t.copy(days = t.days.filterNot { it.day == day } + if (enabled) listOf(DayHours(day, TimeSpan(540, 990))) else emptyList()) }
                        })
                    }
                    if (hours != null) TextButton(onClick = { editingDay = hours }) { Text("${minuteLabel(hours.time.start)}–${minuteLabel(hours.time.end)} · Edit hours") }
                    else Text("No automatic frees. Existing sessions stay visible.", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        item { PeriodSetup(timetable, update) }
        item { Text("Tempo 0.1 · Early development", style = MaterialTheme.typography.labelMedium) }
    }
    editingDay?.let { hours ->
        HoursEditor(hours, { editingDay = null }) { new -> update { t -> t.copy(days = t.days.filterNot { it.day == new.day } + new) }; editingDay = null }
    }
}

@Composable
private fun HoursEditor(hours: DayHours, dismiss: () -> Unit, save: (DayHours) -> Unit) {
    var start by rememberSaveable { mutableStateOf(minuteLabel(hours.time.start)) }
    var end by rememberSaveable { mutableStateOf(minuteLabel(hours.time.end)) }
    val time = validSpan(start, end)
    EditorSheet("${weekdayNames[hours.day - 1]} hours", dismiss, confirm = { time?.let { save(hours.copy(time = it)) } }, enabled = time != null) {
        TimeFields(start, end, { start = it }, { end = it })
    }
}
