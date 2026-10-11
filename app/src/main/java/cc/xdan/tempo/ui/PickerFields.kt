package cc.xdan.tempo.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.window.DialogProperties
import cc.xdan.tempo.model.minuteLabel
import cc.xdan.tempo.model.parseMinute
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

@Composable
private fun PickerField(label: String, value: String, icon: ImageVector, modifier: Modifier, open: () -> Unit) {
    Surface(onClick = open, modifier = modifier, shape = MaterialTheme.shapes.extraSmall,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline), color = MaterialTheme.colorScheme.surface) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(Modifier.weight(1f)) {
                Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(value, style = MaterialTheme.typography.bodyLarge)
            }
            Icon(icon, null)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateSelectionDialog(date: LocalDate, dismiss: () -> Unit, select: (LocalDate) -> Unit) {
    // Material calendar timestamps represent UTC dates, not local midnight instants.
    val state = rememberDatePickerState(initialSelectedDateMillis = date.atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli(), yearRange = 1..9999)
    DatePickerDialog(onDismissRequest = dismiss,
        confirmButton = { TextButton(onClick = { state.selectedDateMillis?.let { select(Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate()) }; dismiss() }, enabled = state.selectedDateMillis != null) { Text("Choose date") } },
        dismissButton = { TextButton(onClick = dismiss) { Text("Cancel") } }) {
        DatePicker(state, showModeToggle = false)
    }
}

@Composable
fun DateField(label: String, value: String, change: (String) -> Unit) {
    var choosing by remember { mutableStateOf(false) }
    PickerField(label, value, Icons.Outlined.CalendarMonth, Modifier.fillMaxWidth()) { choosing = true }
    if (choosing) DateSelectionDialog(validDate(value) ?: LocalDate.now(), { choosing = false }) { change(it.toString()) }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimeField(label: String, value: String, change: (String) -> Unit, modifier: Modifier = Modifier.fillMaxWidth(), allowEndOfDay: Boolean = false) {
    var choosing by remember { mutableStateOf(false) }
    PickerField(label, value, Icons.Outlined.AccessTime, modifier) { choosing = true }
    if (choosing) {
        val minute = (parseMinute(value) ?: 540) % 1440
        val state = rememberTimePickerState(initialHour = minute / 60, initialMinute = minute % 60, is24Hour = true)
        BasicAlertDialog(onDismissRequest = { choosing = false }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
            Surface(Modifier.widthIn(max = 420.dp).fillMaxWidth().padding(12.dp), shape = MaterialTheme.shapes.extraLarge) {
                Column(Modifier.heightIn(max = (LocalConfiguration.current.screenHeightDp * .9f).dp).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(label, style = MaterialTheme.typography.titleLarge)
                    Column(Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState()), horizontalAlignment = Alignment.CenterHorizontally) {
                        TimePicker(state)
                        if (allowEndOfDay) TextButton(onClick = { change("24:00"); choosing = false }) { Text("End of day (24:00)") }
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = { choosing = false }) { Text("Cancel") }
                        TextButton(onClick = {
                            val selected = state.hour * 60 + state.minute
                            change(minuteLabel(if (allowEndOfDay && selected == 0) 1440 else selected))
                            choosing = false
                        }) { Text("Choose time") }
                    }
                }
            }
        }
    }
}
