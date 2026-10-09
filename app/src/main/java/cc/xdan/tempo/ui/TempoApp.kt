package cc.xdan.tempo.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.graphics.luminance
import androidx.core.view.WindowCompat
import android.app.Activity
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
        val view = LocalView.current
        val lightBars = MaterialTheme.colorScheme.background.luminance() > .5f
        SideEffect {
            (view.context as? Activity)?.let { activity ->
                WindowCompat.getInsetsController(activity.window, view).apply {
                    isAppearanceLightStatusBars = lightBars
                    isAppearanceLightNavigationBars = lightBars
                }
            }
        }
        val files = timetableFileActions(model, state)
        var showTimetables by rememberSaveable { mutableStateOf(false) }
        val snackbar = remember { SnackbarHostState() }
        LaunchedEffect(state.message) {
            state.message?.let { snackbar.showSnackbar(it); model.dismissMessage() }
        }
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Box(Modifier.fillMaxSize()) {
            key(state.collection?.activeId, state.contentEpoch) {
            if (timetable == null) {
                Box(Modifier.fillMaxSize().safeDrawingPadding(), contentAlignment = Alignment.Center) {
                    if (state.error != null) Text(state.error!!, Modifier.padding(24.dp))
                    else CircularProgressIndicator()
                }
            } else if (!timetable.onboarded) {
                Onboarding(timetable, model::update, files.import, if ((state.collection?.timetables?.size ?: 0) > 1) ({ showTimetables = true }) else null)
            } else {
                var destination by rememberSaveable { mutableStateOf(Destination.TODAY) }
                var day by rememberSaveable { mutableIntStateOf(LocalDateTime.now().dayOfWeek.value) }
                var editingSessionId by rememberSaveable { mutableStateOf<String?>(null) }
                val editingSession = timetable.sessions.firstOrNull { it.id == editingSessionId }
                var showEditor by rememberSaveable { mutableStateOf(false) }
                var initialStart by rememberSaveable { mutableStateOf<Int?>(null) }
                var initialEnd by rememberSaveable { mutableStateOf<Int?>(null) }
                var slotDay by rememberSaveable { mutableIntStateOf(day) }
                var showFree by rememberSaveable { mutableStateOf(false) }
                var showBreak by rememberSaveable { mutableStateOf(false) }
                var breakId by rememberSaveable { mutableStateOf<String?>(null) }
                val editFree: (Int, TimeSpan) -> Unit = { d, time -> slotDay = d; initialStart = time.start; initialEnd = time.end; showFree = true }
                val editPause: (ScheduledBreak) -> Unit = { pause -> breakId = pause.id; initialStart = null; initialEnd = null; showBreak = true }
                var now by remember { mutableStateOf(LocalDateTime.now()) }
                LaunchedEffect(Unit) { while (true) { now = LocalDateTime.now(); delay(30_000) } }
                Scaffold(
                    topBar = { TopAppBar(title = { Column {
                        Text(if (destination == Destination.TODAY) "Tempo" else destination.name.lowercase().replaceFirstChar { it.titlecase() })
                        Row(Modifier.clickable(onClickLabel = "Manage timetables") { showTimetables = true }, verticalAlignment = Alignment.CenterVertically) {
                            Text(timetable.name, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1,
                                modifier = Modifier.weight(1f, fill = false), overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                            Icon(Icons.Outlined.ArrowDropDown, null, Modifier.size(18.dp))
                        }
                    } }, actions = { IconButton(onClick = { showTimetables = true }) { Icon(Icons.Outlined.CalendarMonth, "Manage timetables") } }) },
                    bottomBar = { NavigationBar {
                        val icons = listOf(Icons.Outlined.Today, Icons.Outlined.CalendarViewWeek, Icons.Outlined.CollectionsBookmark, Icons.Outlined.Settings)
                        Destination.entries.forEachIndexed { index, item ->
                            NavigationBarItem(selected = destination == item, onClick = { destination = item },
                                icon = { Icon(icons[index], null) }, label = { Text(item.name.lowercase().replaceFirstChar { it.titlecase() }) })
                        }
                    } },
                    floatingActionButton = {
                        if (destination == Destination.TODAY || destination == Destination.TIMETABLE) ExtendedFloatingActionButton(
                            onClick = { editingSessionId = null; initialStart = null; initialEnd = null; showEditor = true }, icon = { Icon(Icons.Outlined.Add, null) }, text = { Text("Add session") })
                    },
                ) { padding ->
                    Box(Modifier.padding(padding).fillMaxSize()) {
                        PredictiveRouteHost(destination, enabled = !showEditor && !showFree && !showBreak && !showTimetables && state.importPreview == null, navigate = { destination = it }) { shown ->
                        when (shown) {
                            Destination.TODAY -> DayScreen(timetable, now.dayOfWeek.value, now, true, {}, { editingSessionId = it.id; initialStart = null; initialEnd = null; showEditor = true }, editFree, editPause)
                            Destination.TIMETABLE -> DayScreen(timetable, day, now, false, { day = it }, { editingSessionId = it.id; initialStart = null; initialEnd = null; showEditor = true }, editFree, editPause)
                            Destination.LIBRARY -> LibraryScreen(timetable, model::update)
                            Destination.SETTINGS -> SettingsScreen(timetable, model::update, files, { showTimetables = true }, state.fileBusy)
                        }
                        }
                    }
                }
                if (showEditor) SessionEditor(timetable, editingSession, if (initialStart != null) slotDay else if (destination == Destination.TODAY) now.dayOfWeek.value else day,
                    initialTime = initialStart?.let { a -> initialEnd?.let { b -> TimeSpan(a, b) } },
                    onDismiss = { showEditor = false },
                    onSave = { session -> model.update { it.copy(sessions = it.sessions.filterNot { s -> s.id == session.id } + session) }; showEditor = false },
                    onDelete = { id -> model.update { it.copy(sessions = it.sessions.filterNot { s -> s.id == id }) }; showEditor = false },
                    onAddSubject = { subject -> model.update { it.copy(subjects = it.subjects + subject) } })
                if (showFree) FreeSlotEditor(slotDay, TimeSpan(initialStart!!, initialEnd!!), { showFree = false },
                    { showFree = false; editingSessionId = null; showEditor = true }, { showFree = false; breakId = null; showBreak = true })
                if (showBreak) BreakEditor(timetable, timetable.breaks.firstOrNull { it.id == breakId },
                    initialDay = if (initialStart != null) slotDay else null,
                    initialTime = initialStart?.let { a -> initialEnd?.let { b -> TimeSpan(a, b) } },
                    dismiss = { showBreak = false }, save = { pause -> model.update { it.copy(breaks = it.breaks.filterNot { b -> b.id == pause.id } + pause) }; showBreak = false },
                    delete = { id -> model.update { it.copy(breaks = it.breaks.filterNot { b -> b.id == id }) }; showBreak = false })
            }
            }
            if (showTimetables) state.collection?.let { TimetableManager(it, model, files) { showTimetables = false } }
            if (state.fileBusy) Surface(Modifier.align(Alignment.BottomCenter).safeDrawingPadding().padding(20.dp), shape = MaterialTheme.shapes.medium) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    CircularProgressIndicator(Modifier.size(24.dp)); Text("Working with your timetable file…")
                }
            }
            SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter).safeDrawingPadding().padding(16.dp))
            if (state.error != null && timetable != null) AlertDialog(onDismissRequest = model::dismissError,
                title = { Text("Something needs attention") }, text = { Text(state.error!!) },
                confirmButton = { TextButton(onClick = model::dismissError) { Text("Close") } })
            }
        }
    }
}
