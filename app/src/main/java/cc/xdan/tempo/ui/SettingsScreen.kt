package cc.xdan.tempo.ui

import android.os.Build
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.foundation.shape.CircleShape
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

private enum class SettingsPage(val label: String, val detail: String) {
    FILES("Timetables & files", "Manage, import, backup and export images"),
    APPEARANCE("Appearance", "Themes, colours and block styles"),
    SCHEDULE("Schedule", "Repeating weeks, holidays, day hours and usual periods"),
    UPDATES("Updates", "Release checks and downloads")
}

@Composable
fun SettingsScreen(timetable: Timetable, update: ((Timetable) -> Timetable) -> Unit, files: TimetableFileActions, manageTimetables: () -> Unit, fileBusy: Boolean, report: (String) -> Unit = {}) {
    var editingDay by remember { mutableStateOf<DayHours?>(null) }
    var pngExport by remember { mutableStateOf(false) }
    var page by rememberSaveable { mutableStateOf<SettingsPage?>(null) }
    PredictiveSettingsHost(page?.name, { page = null }) { shownPage ->
    val currentPage = shownPage?.let(SettingsPage::valueOf)
    key(currentPage) {
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        if (currentPage == null) {
        item {
            Text("Your Tempo", style = MaterialTheme.typography.headlineMedium)
            Text("Part of Axis", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        items(SettingsPage.entries) { section ->
            ElevatedCard(onClick = { page = section }, modifier = Modifier.fillMaxWidth()) {
                Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Icon(when (section) { SettingsPage.FILES -> Icons.Outlined.Folder; SettingsPage.APPEARANCE -> Icons.Outlined.Palette; SettingsPage.SCHEDULE -> Icons.Outlined.CalendarMonth; SettingsPage.UPDATES -> Icons.Outlined.SystemUpdate }, null)
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(section.label, style = MaterialTheme.typography.titleMedium)
                    Text(section.detail, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Icon(Icons.Outlined.ChevronRight, null)
                }
            }
        }
        item { HorizontalDivider(Modifier.padding(top = 12.dp)); AboutTempo() }
        } else item {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                FilledTonalIconButton(onClick = { page = null }, shape = CircleShape) { Icon(Icons.Outlined.ArrowBack, "Back to settings") }
                Text(currentPage.label, style = MaterialTheme.typography.headlineMedium)
            }
        }
        if (currentPage == SettingsPage.FILES) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Timetables", style = MaterialTheme.typography.titleLarge)
                OutlinedButton(onClick = manageTimetables, modifier = Modifier.fillMaxWidth()) { Text("Manage timetables") }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = files.import, enabled = !fileBusy, modifier = Modifier.weight(1f)) { Text("Import file") }
                    OutlinedButton(onClick = files.export, enabled = !fileBusy, modifier = Modifier.weight(1f)) { Text("Export file") }
                }
                Text("Tempo files include subjects, colours, times, breaks and settings.", style = MaterialTheme.typography.bodySmall)
                OutlinedButton(onClick = { pngExport = true }, enabled = !fileBusy, modifier = Modifier.fillMaxWidth()) { Text("Export day / week PNG") }
            }
        }
        }
        if (currentPage == SettingsPage.APPEARANCE) item { AppearanceControls(timetable.appearance) { appearance -> update { it.copy(appearance = appearance) } } }
        if (currentPage == SettingsPage.SCHEDULE) {
        item { DatedScheduleSettings(timetable, update) }
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
        }
        if (currentPage == SettingsPage.UPDATES) item { UpdateControls() }

    }
    }
    }
    if (pngExport) PngExportSheet(timetable, { pngExport = false }, report)
    editingDay?.let { hours ->
        HoursEditor(hours, { editingDay = null }) { new -> update { t -> t.copy(days = t.days.filterNot { it.day == new.day } + new) }; editingDay = null }
    }
}

@Composable
private fun HoursEditor(hours: DayHours, dismiss: () -> Unit, save: (DayHours) -> Unit) {
    var start by rememberSaveable { mutableStateOf(minuteLabel(hours.time.start)) }
    var end by rememberSaveable { mutableStateOf(minuteLabel(hours.time.end)) }
    val time = validSpan(start, end)
    EditorSheet("${weekdayNames[hours.day - 1]} hours", dismiss, confirm = { time?.let { save(hours.copy(time = it)) } }, enabled = time != null, compact = true) {
        TimeFields(start, end, { start = it }, { end = it })
    }
}
