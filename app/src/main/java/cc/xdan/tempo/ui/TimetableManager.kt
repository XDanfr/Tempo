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
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import cc.xdan.tempo.TempoViewModel
import cc.xdan.tempo.model.*

@Composable
fun TimetableManager(collection: TimetableCollection, model: TempoViewModel, files: TimetableFileActions, compare: () -> Unit, dismiss: () -> Unit) {
    var naming by rememberSaveable { mutableStateOf(false) }
    var renameId by rememberSaveable { mutableStateOf<String?>(null) }
    var name by rememberSaveable { mutableStateOf("") }
    var deleteId by rememberSaveable { mutableStateOf<String?>(null) }
    EditorSheet("Your timetables", dismiss) {
        collection.timetables.forEach { saved ->
            val active = saved.id == collection.activeId
            // The whole card switches timetable; secondary actions live in its overflow menu.
            Surface(onClick = { if (!active) model.select(saved.id); dismiss() }, shape = MaterialTheme.shapes.medium,
                color = if (active) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
                modifier = Modifier.semantics { selected = active }) {
                Row(Modifier.fillMaxWidth().padding(start = 16.dp, top = 12.dp, bottom = 12.dp, end = 4.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(if (active) Icons.Outlined.CheckCircle else Icons.Outlined.CalendarViewWeek, null)
                    Column(Modifier.weight(1f)) {
                        Text(saved.timetable.name, style = MaterialTheme.typography.titleMedium)
                        Text(if (!saved.timetable.onboarded) "Setup in progress" else "${countLabel(saved.timetable.sessions.size, "session")} · ${countLabel(saved.timetable.subjects.size, "subject")}",
                            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    var menu by remember { mutableStateOf(false) }
                    Box {
                        IconButton(onClick = { menu = true }) { Icon(Icons.Outlined.MoreVert, "Options for ${saved.timetable.name}") }
                        DropdownMenu(menu, { menu = false }, shape = MaterialTheme.shapes.medium) {
                            DropdownMenuItem(text = { Text("Rename") }, leadingIcon = { Icon(Icons.Outlined.Edit, null) },
                                onClick = { menu = false; renameId = saved.id; name = saved.timetable.name; naming = true })
                            DropdownMenuItem(text = { Text("Duplicate") }, leadingIcon = { Icon(Icons.Outlined.ContentCopy, null) },
                                onClick = { menu = false; model.duplicate(saved.id) })
                            // Export always saves the active timetable, so it is offered only there.
                            if (active) DropdownMenuItem(text = { Text("Export file") }, leadingIcon = { Icon(Icons.Outlined.FileDownload, null) },
                                onClick = { menu = false; files.export(); dismiss() })
                            DropdownMenuItem(text = { Text("Delete") }, enabled = collection.timetables.size > 1,
                                leadingIcon = { Icon(Icons.Outlined.Delete, null) }, onClick = { menu = false; deleteId = saved.id })
                        }
                    }
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { renameId = null; name = ""; naming = true }, modifier = Modifier.weight(1f)) {
                Icon(Icons.Outlined.Add, null); Text("New", Modifier.padding(start = 8.dp))
            }
            OutlinedButton(onClick = { files.import(); dismiss() }, modifier = Modifier.weight(1f)) {
                Icon(Icons.Outlined.FileOpen, null); Text("Import", Modifier.padding(start = 8.dp))
            }
        }
        if (collection.timetables.size >= 2) TextButton(onClick = { dismiss(); compare() }, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Outlined.ViewColumn, null); Text("Compare timetables", Modifier.padding(start = 8.dp))
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
            text = { Text("This removes its subjects, sessions, breaks and settings from this device. You can undo it until Tempo closes; export a copy to keep it longer.") },
            confirmButton = { TextButton(onClick = { model.delete(id); deleteId = null }) { Text("Delete", color = MaterialTheme.colorScheme.error) } },
            dismissButton = { TextButton(onClick = { deleteId = null }) { Text("Keep") } })
    }
}
