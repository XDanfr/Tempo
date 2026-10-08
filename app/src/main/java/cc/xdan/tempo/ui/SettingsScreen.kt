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
    var selectedPeriod by remember { mutableStateOf<PeriodTemplate?>(null) }
    var showPeriod by remember { mutableStateOf(false) }
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
        item {
            Text("Usual periods", style = MaterialTheme.typography.titleLarge)
            Text("Time shortcuts for new sessions. Editing a shortcut keeps existing sessions at their saved times.", style = MaterialTheme.typography.bodySmall)
            TextButton(onClick = { selectedPeriod = null; showPeriod = true }) { Text("Add period") }
        }
        items(timetable.periods, key = { it.id }) { period ->
            ElevatedCard(onClick = { selectedPeriod = period; showPeriod = true }, modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) { Text(period.name, style = MaterialTheme.typography.titleMedium); Text("${minuteLabel(period.time.start)}–${minuteLabel(period.time.end)}") }
            }
        }
        item { Text("Tempo 0.1 · Early development", style = MaterialTheme.typography.labelMedium) }
    }
    editingDay?.let { hours ->
        HoursEditor(hours, { editingDay = null }) { new -> update { t -> t.copy(days = t.days.filterNot { it.day == new.day } + new) }; editingDay = null }
    }
    if (showPeriod) PeriodEditor(selectedPeriod, { showPeriod = false },
        { period -> update { it.copy(periods = it.periods.filterNot { p -> p.id == period.id } + period) }; showPeriod = false },
        { id -> update { it.copy(periods = it.periods.filterNot { p -> p.id == id }) }; showPeriod = false })
}

@Composable
fun AppearanceControls(appearance: Appearance, change: (Appearance) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Appearance", style = MaterialTheme.typography.titleLarge)
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ThemePreset.entries.forEach { preset -> FilterChip(appearance.preset == preset, { change(appearance.copy(preset = preset, dynamicColour = false)) }, label = { Text(preset.name.lowercase().replaceFirstChar { it.titlecase() }) }) }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ThemeMode.entries.forEach { mode -> FilterChip(appearance.mode == mode, { change(appearance.copy(mode = mode)) }, label = { Text(mode.name.lowercase().replaceFirstChar { it.titlecase() }) }) }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) { Text("Material You"); Text(if (Build.VERSION.SDK_INT >= 31) "Use your wallpaper colours" else "Available on Android 12 or newer", style = MaterialTheme.typography.bodySmall) }
            Switch(checked = appearance.dynamicColour, enabled = Build.VERSION.SDK_INT >= 31, onCheckedChange = { change(appearance.copy(dynamicColour = it)) })
        }
        Row(verticalAlignment = Alignment.CenterVertically) { Text("Show subject icons", Modifier.weight(1f)); Switch(appearance.showIcons, { change(appearance.copy(showIcons = it)) }) }
    }
}

@Composable
private fun HoursEditor(hours: DayHours, dismiss: () -> Unit, save: (DayHours) -> Unit) {
    var start by rememberSaveable { mutableStateOf(minuteLabel(hours.time.start)) }
    var end by rememberSaveable { mutableStateOf(minuteLabel(hours.time.end)) }
    val time = validSpan(start, end)
    AlertDialog(onDismissRequest = dismiss, title = { Text("${weekdayNames[hours.day - 1]} hours") }, text = { Column { TimeFields(start, end, { start = it }, { end = it }) } },
        confirmButton = { TextButton(enabled = time != null, onClick = { time?.let { save(hours.copy(time = it)) } }) { Text("Save") } },
        dismissButton = { TextButton(onClick = dismiss) { Text("Cancel") } })
}

@Composable
private fun PeriodEditor(period: PeriodTemplate?, dismiss: () -> Unit, save: (PeriodTemplate) -> Unit, delete: (String) -> Unit) {
    val id = rememberSaveable { period?.id ?: UUID.randomUUID().toString() }
    var name by rememberSaveable { mutableStateOf(period?.name ?: "") }
    var start by rememberSaveable { mutableStateOf(minuteLabel(period?.time?.start ?: 540)) }
    var end by rememberSaveable { mutableStateOf(minuteLabel(period?.time?.end ?: 630)) }
    val time = validSpan(start, end)
    AlertDialog(onDismissRequest = dismiss, title = { Text(if (period == null) "New usual period" else "Edit usual period") },
        text = { Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(name, { name = it }, label = { Text("Name") }, singleLine = true)
            TimeFields(start, end, { start = it }, { end = it })
            if (period != null) TextButton(onClick = { delete(id) }) { Text("Remove shortcut") }
        } }, confirmButton = { TextButton(enabled = time != null && name.isNotBlank(), onClick = { time?.let { save(PeriodTemplate(id, name.trim(), it)) } }) { Text("Save") } },
        dismissButton = { TextButton(onClick = dismiss) { Text("Cancel") } })
}
