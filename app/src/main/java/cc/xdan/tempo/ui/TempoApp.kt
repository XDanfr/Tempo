package cc.xdan.tempo.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cc.xdan.tempo.TempoViewModel
import cc.xdan.tempo.design.TempoTheme
import cc.xdan.tempo.model.*
import java.time.LocalDateTime
import kotlinx.coroutines.delay

enum class Destination { TODAY, TIMETABLE, LIBRARY, SETTINGS }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TempoApp(model: TempoViewModel) {
    val state by model.state.collectAsStateWithLifecycle()
    val timetable = state.timetable
    TempoTheme(timetable?.appearance ?: Appearance()) {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            if (timetable == null) {
                Box(Modifier.fillMaxSize().safeDrawingPadding(), contentAlignment = Alignment.Center) {
                    if (state.error != null) Text(state.error!!, Modifier.padding(24.dp))
                    else CircularProgressIndicator()
                }
            } else if (!timetable.onboarded) {
                Onboarding(timetable, model::update)
            } else {
                var destination by rememberSaveable { mutableStateOf(Destination.TODAY) }
                var day by rememberSaveable { mutableIntStateOf(LocalDateTime.now().dayOfWeek.value) }
                var editingSessionId by rememberSaveable { mutableStateOf<String?>(null) }
                val editingSession = timetable.sessions.firstOrNull { it.id == editingSessionId }
                var showEditor by rememberSaveable { mutableStateOf(false) }
                var now by remember { mutableStateOf(LocalDateTime.now()) }
                LaunchedEffect(Unit) { while (true) { now = LocalDateTime.now(); delay(30_000) } }
                Scaffold(
                    topBar = { TopAppBar(title = { Column {
                        Text(if (destination == Destination.TODAY) "Tempo" else destination.name.lowercase().replaceFirstChar { it.titlecase() })
                        Text(timetable.name, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } }) },
                    bottomBar = { NavigationBar {
                        val icons = listOf(Icons.Outlined.Today, Icons.Outlined.CalendarViewWeek, Icons.Outlined.CollectionsBookmark, Icons.Outlined.Settings)
                        Destination.entries.forEachIndexed { index, item ->
                            NavigationBarItem(selected = destination == item, onClick = { destination = item },
                                icon = { Icon(icons[index], null) }, label = { Text(item.name.lowercase().replaceFirstChar { it.titlecase() }) })
                        }
                    } },
                    floatingActionButton = {
                        if (destination == Destination.TODAY || destination == Destination.TIMETABLE) ExtendedFloatingActionButton(
                            onClick = { editingSessionId = null; showEditor = true }, icon = { Icon(Icons.Outlined.Add, null) }, text = { Text("Add session") })
                    },
                ) { padding ->
                    Box(Modifier.padding(padding).fillMaxSize()) {
                        when (destination) {
                            Destination.TODAY -> DayScreen(timetable, now.dayOfWeek.value, now, true, {}, { editingSessionId = it.id; showEditor = true })
                            Destination.TIMETABLE -> DayScreen(timetable, day, now, false, { day = it }, { editingSessionId = it.id; showEditor = true })
                            Destination.LIBRARY -> LibraryScreen(timetable, model::update)
                            Destination.SETTINGS -> SettingsScreen(timetable, model::update)
                        }
                    }
                }
                if (showEditor) SessionEditor(timetable, editingSession, if (destination == Destination.TODAY) now.dayOfWeek.value else day,
                    onDismiss = { showEditor = false },
                    onSave = { session -> model.update { it.copy(sessions = it.sessions.filterNot { s -> s.id == session.id } + session) }; showEditor = false },
                    onDelete = { id -> model.update { it.copy(sessions = it.sessions.filterNot { s -> s.id == id }) }; showEditor = false })
            }
            if (state.error != null && timetable != null) AlertDialog(onDismissRequest = model::dismissError,
                title = { Text("Change not saved") }, text = { Text(state.error!!) },
                confirmButton = { TextButton(onClick = model::dismissError) { Text("Close") } })
        }
    }
}
