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
    var personId by rememberSaveable { mutableStateOf<String?>(null) }
    var shareId by rememberSaveable { mutableStateOf<String?>(null) }
    var pasting by rememberSaveable { mutableStateOf(false) }
    var pasted by rememberSaveable { mutableStateOf("") }
    var personName by rememberSaveable { mutableStateOf("") }
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
                        Text(saved.timetable.person?.let { "$it · ${saved.timetable.name}" } ?: saved.timetable.name, style = MaterialTheme.typography.titleMedium)
                        Text(if (!saved.timetable.onboarded) "Setup in progress" else "${countLabel(saved.timetable.sessions.size, "session")} · ${countLabel(saved.timetable.subjects.size, "subject")}",
                            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    var menu by remember { mutableStateOf(false) }
                    Box {
                        IconButton(onClick = { menu = true }) { Icon(Icons.Outlined.MoreVert, "Options for ${saved.timetable.name}") }
                        DropdownMenu(menu, { menu = false }, shape = MaterialTheme.shapes.medium) {
                            DropdownMenuItem(text = { Text("Rename") }, leadingIcon = { Icon(Icons.Outlined.Edit, null) },
                                onClick = { menu = false; renameId = saved.id; name = saved.timetable.name; naming = true })
                            DropdownMenuItem(text = { Text(if (saved.timetable.person == null) "Someone else's…" else "Mark as mine") },
                                leadingIcon = { Icon(if (saved.timetable.person == null) Icons.Outlined.PersonAdd else Icons.Outlined.Person, null) },
                                onClick = { menu = false; if (saved.timetable.person == null) { personId = saved.id; personName = "" } else model.setPerson(saved.id, null) })
                            if (saved.timetable.person == null && saved.timetable.onboarded) DropdownMenuItem(text = { Text("Share link…") },
                                leadingIcon = { Icon(Icons.Outlined.Link, null) }, onClick = { menu = false; shareId = saved.id })
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
            val newPress = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
            val importPress = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
            Button(onClick = { renameId = null; name = ""; naming = true }, modifier = squishWeight(newPress), interactionSource = newPress) {
                Icon(Icons.Outlined.Add, null); Text("New", Modifier.padding(start = 8.dp))
            }
            OutlinedButton(onClick = { files.import(); dismiss() }, modifier = squishWeight(importPress), interactionSource = importPress) {
                Icon(Icons.Outlined.FileOpen, null); Text("Import", Modifier.padding(start = 8.dp))
            }
        }
        TextButton(onClick = { pasted = ""; pasting = true }, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Outlined.AddLink, null); Text("Add from link", Modifier.padding(start = 8.dp))
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
        // Deleting a whole timetable is the one delete that still confirms first.
        AlertDialog(onDismissRequest = { deleteId = null }, icon = { Icon(Icons.Outlined.Delete, null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Delete ${saved?.timetable?.name}?") },
            text = { Text("This removes its subjects, sessions, breaks and settings from this device. You can undo it until Tempo closes; export a copy to keep it longer.") },
            confirmButton = { Button(onClick = { model.delete(id); deleteId = null }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error,
                contentColor = MaterialTheme.colorScheme.onError)) { Text("Delete timetable") } },
            dismissButton = { TextButton(onClick = { deleteId = null }) { Text("Keep") } })
    }
    personId?.let { id ->
        AlertDialog(onDismissRequest = { personId = null }, icon = { Icon(Icons.Outlined.PersonAdd, null) },
            title = { Text("Whose timetable is this?") },
            text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Tempo will tell you when your frees line up with theirs.")
                OutlinedTextField(personName, { personName = it }, label = { Text("Their name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            } },
            confirmButton = { Button(enabled = personName.isNotBlank(), onClick = { model.setPerson(id, personName); personId = null }) { Text("Save") } })
    }
    shareId?.let { id -> collection.timetables.firstOrNull { it.id == id }?.let { saved ->
        ShareSheet(id, saved.timetable, collection.profileName, model::setProfileName) { shareId = null }
    } }
    if (pasting) AlertDialog(onDismissRequest = { pasting = false }, icon = { Icon(Icons.Outlined.AddLink, null) }, title = { Text("Add from link") },
        text = { OutlinedTextField(pasted, { pasted = it }, label = { Text("Paste a Tempo link") }, singleLine = true, modifier = Modifier.fillMaxWidth()) },
        confirmButton = { Button(enabled = cc.xdan.tempo.data.ShareLink.parse(pasted) != null, onClick = { pasting = false; dismiss(); model.openShareLink(pasted) }) { Text("Open") } })
}
