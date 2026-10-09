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
import cc.xdan.tempo.TempoViewModel
import cc.xdan.tempo.model.*

@Composable
fun TimetableManager(collection: TimetableCollection, model: TempoViewModel, files: TimetableFileActions, dismiss: () -> Unit) {
    var naming by rememberSaveable { mutableStateOf(false) }
    var renameId by rememberSaveable { mutableStateOf<String?>(null) }
    var name by rememberSaveable { mutableStateOf("") }
    var deleteId by rememberSaveable { mutableStateOf<String?>(null) }
    EditorSheet("Your timetables", dismiss) {
        Text("Choose the timetable shown in Tempo. Each has its own subjects, times and appearance.")
        collection.timetables.forEach { saved ->
            val active = saved.id == collection.activeId
            Surface(onClick = { model.select(saved.id); dismiss() }, shape = MaterialTheme.shapes.medium,
                color = if (active) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh) {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Icon(if (active) Icons.Outlined.CheckCircle else Icons.Outlined.CalendarViewWeek, null)
                        Column(Modifier.weight(1f)) {
                            Text(saved.timetable.name, style = MaterialTheme.typography.titleMedium)
                            Text(if (!saved.timetable.onboarded) "Setup in progress" else "${saved.timetable.sessions.size} sessions · ${saved.timetable.subjects.size} subjects", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    Row {
                        TextButton(onClick = { renameId = saved.id; name = saved.timetable.name; naming = true }) { Text("Rename") }
                        TextButton(onClick = { model.duplicate(saved.id); dismiss() }) { Text("Duplicate") }
                        TextButton(enabled = collection.timetables.size > 1, onClick = { deleteId = saved.id }) { Text("Delete") }
                    }
                }
            }
        }
        Button(onClick = { renameId = null; name = ""; naming = true }, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Outlined.Add, null); Text("New timetable", Modifier.padding(start = 8.dp))
        }
        OutlinedButton(onClick = { files.import(); dismiss() }, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Outlined.FileOpen, null); Text("Import timetable", Modifier.padding(start = 8.dp))
        }
        OutlinedButton(onClick = { files.export(); dismiss() }, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Outlined.FileDownload, null); Text("Export ${collection.active.name}", Modifier.padding(start = 8.dp))
        }
    }
    if (naming) AlertDialog(onDismissRequest = { naming = false }, title = { Text(if (renameId == null) "New timetable" else "Rename timetable") },
        text = { OutlinedTextField(name, { name = it }, label = { Text("Name") }, singleLine = true, modifier = Modifier.fillMaxWidth()) },
        confirmButton = { TextButton(enabled = name.isNotBlank(), onClick = {
            val id = renameId
            if (id == null) { model.create(name); dismiss() } else model.rename(id, name)
            naming = false
        }) { Text(if (renameId == null) "Create" else "Save") } },
        dismissButton = { TextButton(onClick = { naming = false }) { Text("Cancel") } })
    deleteId?.let { id ->
        val saved = collection.timetables.firstOrNull { it.id == id }
        AlertDialog(onDismissRequest = { deleteId = null }, title = { Text("Delete ${saved?.timetable?.name}?") },
            text = { Text("This removes its subjects, sessions, breaks and settings from this device. Export a copy first if you need it again.") },
            confirmButton = { TextButton(onClick = { model.delete(id); deleteId = null }) { Text("Delete", color = MaterialTheme.colorScheme.error) } },
            dismissButton = { TextButton(onClick = { deleteId = null }) { Text("Keep") } })
    }
}
