package cc.xdan.tempo.ui

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
            ElevatedCard(onClick = { selected = subject; editor = true }, modifier = Modifier.fillMaxWidth()) {
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
    var confirmDelete by remember { mutableStateOf(false) }
    val colours = listOf(0xFFB7F397, 0xFFD3BCFF, 0xFFA2DFFF, 0xFFFFD59B, 0xFFFFB6C6, 0xFFACE0D3)
    AlertDialog(onDismissRequest = onDismiss, title = { Text(if (existing == null) "New subject / activity" else "Edit subject / activity") },
        text = { Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(name, { name = it }, label = { Text("Name") }, singleLine = true)
            OutlinedTextField(location, { location = it }, label = { Text("Default location") }, singleLine = true)
            Text("Subject colour", style = MaterialTheme.typography.labelLarge)
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                colours.forEach { value -> FilterChip(colour == value, { colour = value }, label = { Box(Modifier.size(24.dp).background(Color(value), CircleShape)) }) }
            }
            Text("Optional icon", style = MaterialTheme.typography.labelLarge)
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SubjectIcon.entries.forEach { option -> FilterChip(icon == option, { icon = option }, label = { option.vector()?.let { Icon(it, option.name.lowercase()) } ?: Text("None") }) }
            }
            if (existing != null) TextButton(onClick = { confirmDelete = true }) { Text("Delete subject", color = MaterialTheme.colorScheme.error) }
        } },
        confirmButton = { TextButton(enabled = name.isNotBlank(), onClick = { onSave(Subject(id, name.trim(), colour, icon, location.trim())) }) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } })
    if (confirmDelete) AlertDialog(onDismissRequest = { confirmDelete = false }, title = { Text("Delete ${existing?.name}?") }, text = { Text("This also removes $sessionCount scheduled sessions.") },
        confirmButton = { TextButton(onClick = { onDelete(id) }) { Text("Delete") } }, dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Keep") } })
}
