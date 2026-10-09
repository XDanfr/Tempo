package cc.xdan.tempo.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import cc.xdan.tempo.model.*
import java.util.UUID

@Composable
fun LibraryScreen(timetable: Timetable, update: ((Timetable) -> Timetable) -> Unit) {
    var editor by remember { mutableStateOf(false) }
    var selected by remember { mutableStateOf<Subject?>(null) }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Text("Make it yours", style = MaterialTheme.typography.headlineMedium)
            Text("Subjects, work activities and the places you go.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Button(onClick = { selected = null; editor = true }, modifier = Modifier.padding(top = 16.dp)) { Icon(Icons.Outlined.Add, null); Text("Add subject", Modifier.padding(start = 8.dp)) }
        }
        if (timetable.subjects.isEmpty()) item { Text("Your library is empty. Add your first subject to start scheduling.") }
        items(timetable.subjects, key = { it.id }) { subject ->
            Card(onClick = { selected = subject; editor = true }, modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = if (timetable.appearance.blockStyle == BlockStyle.OUTLINED) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceContainerHigh),
                border = if (timetable.appearance.blockStyle == BlockStyle.OUTLINED) BorderStroke(1.5.dp, Color(subject.colour)) else null) {
                Row(Modifier.padding(20.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    Box(Modifier.size(20.dp).background(Color(subject.colour), CircleShape))
                    if (timetable.appearance.showIcons) subject.icon.vector()?.let { Icon(it, null) }
                    Column { Text(subject.name, style = MaterialTheme.typography.titleMedium)
                        Text("${timetable.sessions.count { it.subjectId == subject.id }} sessions" + if (subject.location.isNotBlank()) " · ${subject.location}" else "", style = MaterialTheme.typography.bodySmall) }
                }
            }
        }
    }
    if (editor) SubjectEditor(selected, timetable.sessions.count { it.subjectId == selected?.id }, { editor = false },
        { subject -> update { it.copy(subjects = it.subjects.filterNot { s -> s.id == subject.id } + subject) }; editor = false },
        { id -> update { it.copy(subjects = it.subjects.filterNot { s -> s.id == id }, sessions = it.sessions.filterNot { s -> s.subjectId == id }) }; editor = false })
}

@Composable
fun SubjectEditor(existing: Subject?, sessionCount: Int, onDismiss: () -> Unit, onSave: (Subject) -> Unit, onDelete: (String) -> Unit) {
    val id = rememberSaveable { existing?.id ?: UUID.randomUUID().toString() }
    var name by rememberSaveable { mutableStateOf(existing?.name ?: "") }
    var location by rememberSaveable { mutableStateOf(existing?.location ?: "") }
    var colour by rememberSaveable { mutableLongStateOf(existing?.colour ?: 0xFFB7F397) }
    var icon by rememberSaveable { mutableStateOf(existing?.icon ?: SubjectIcon.NONE) }
    var showIcons by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    val colours = listOf(0xFFB7F397, 0xFFD3BCFF, 0xFFA2DFFF, 0xFFFFD59B, 0xFFFFB6C6, 0xFFACE0D3)
    EditorSheet(if (existing == null) "New subject / activity" else "Edit subject / activity", onDismiss,
        confirm = { onSave(Subject(id, name.trim(), colour, icon, location.trim())) }, enabled = name.isNotBlank()) {
        OutlinedTextField(name, { name = it }, label = { Text("Name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(location, { location = it }, label = { Text("Default location") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        Text("Subject colour", style = MaterialTheme.typography.labelLarge)
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            colours.forEachIndexed { index, value -> Surface(onClick = { colour = value }, color = Color(value), shape = CircleShape,
                border = if (colour == value) BorderStroke(3.dp, MaterialTheme.colorScheme.onSurface) else null) {
                Box(Modifier.size(44.dp), contentAlignment = androidx.compose.ui.Alignment.Center) {
                    if (colour == value) Icon(Icons.Outlined.Check, "Selected colour ${index + 1}", tint = Color(0xFF172116))
                    else Text("${index + 1}", color = Color(0xFF172116), style = MaterialTheme.typography.labelSmall)
                }
            } }
        }
        OutlinedButton(onClick = { showIcons = true }, modifier = Modifier.fillMaxWidth()) {
            Icon(icon.vector() ?: Icons.Outlined.GridView, null)
            Text("${icon.label()} · Choose icon", Modifier.padding(start = 12.dp))
        }
        if (existing != null) TextButton(onClick = { confirmDelete = true }) { Text("Delete subject", color = MaterialTheme.colorScheme.error) }
    }
    if (showIcons) IconPicker(icon, { showIcons = false }, { icon = it; showIcons = false })
    if (confirmDelete) AlertDialog(onDismissRequest = { confirmDelete = false }, title = { Text("Delete ${existing?.name}?") }, text = { Text("This also removes $sessionCount scheduled sessions.") },
        confirmButton = { TextButton(onClick = { onDelete(id) }) { Text("Delete") } }, dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Keep") } })
}
