package cc.xdan.tempo.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CollectionsBookmark
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ViewWeek
import androidx.compose.material.icons.filled.Home
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
import cc.xdan.tempo.design.pressScale
import cc.xdan.tempo.design.rememberTempoHaptics
import androidx.compose.material.icons.automirrored.outlined.Redo
import androidx.compose.material.icons.automirrored.outlined.Undo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import cc.xdan.tempo.model.*
import java.time.LocalDateTime
import java.time.LocalDate
import cc.xdan.tempo.schedule.*
import kotlinx.coroutines.delay

/** Bottom-bar destinations. Today and Timetable were merged into the single day-first Schedule. */
enum class Destination(val label: String, val inBar: Boolean = true) {
    HOME("Home"), SCHEDULE("Schedule"), WEEK("Week"), LIBRARY("Library"), PROFILE("Profile", false), SETTINGS("Settings", false);
    /** Where Back goes: Settings returns to Profile, everything else to Home. */
    val parent: Destination? get() = when (this) { HOME -> null; SETTINGS -> PROFILE; else -> HOME }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
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
        var showComparison by rememberSaveable { mutableStateOf(false) }
        var showTimetables by rememberSaveable { mutableStateOf(false) }
        var showImageExport by rememberSaveable { mutableStateOf(false) }
        var showProgress by rememberSaveable { mutableStateOf(false) }
        var forceUpdatePrompt by rememberSaveable { mutableStateOf(false) }
        val feedbackHost: @Composable (Modifier) -> Unit = { modifier ->
            FeedbackHost(state.feedback, { model.undo(it) }, { model.redo(it) }, model::dismissFeedback, modifier)
        }
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Box(Modifier.fillMaxSize()) {
            key(state.collection?.activeId, state.contentEpoch) {
            if (timetable == null) {
                Box(Modifier.fillMaxSize().safeDrawingPadding(), contentAlignment = Alignment.Center) {
                    if (state.error != null) Text(state.error!!, Modifier.padding(24.dp))
                    else LoadingIndicator()
                }
            } else if (!timetable.onboarded) {
                Onboarding(timetable, model::update, files.import, if ((state.collection?.timetables?.size ?: 0) > 1) ({ showTimetables = true }) else null)
            } else {
                var destination by rememberSaveable { mutableStateOf(Destination.HOME) }
                var fabExpanded by remember { mutableStateOf(true) }
                val navHaptics = rememberTempoHaptics()
                // Other people's timetables saved on this device, for shared frees.
                val people = remember(state.collection) {
                    state.collection?.timetables.orEmpty().filter { it.id != state.collection?.activeId && it.timetable.onboarded }
                        .mapNotNull { saved -> saved.timetable.person?.let { it to saved.timetable } }
                }
                var selectedDate by rememberSaveable { mutableStateOf(LocalDate.now().toString()) }
                var editingBaseDate by rememberSaveable { mutableStateOf(LocalDate.now().toString()) }
                var editorDate by rememberSaveable { mutableStateOf<String?>(null) }
                val datedTarget = (editorDate ?: editingBaseDate.takeIf { date -> timetable.exceptions.any { it.date == date } })?.let(LocalDate::parse)
                val editorTable = datedTarget?.let { date ->
                    timetable.replacementDay(date).let { replacement -> timetable.copy(sessions = replacement.sessions, breaks = replacement.breaks) }
                } ?: timetable
                var editingSessionId by rememberSaveable { mutableStateOf<String?>(null) }
                val editingSession = editorTable.sessions.firstOrNull { it.id == editingSessionId }
                var showEditor by rememberSaveable { mutableStateOf(false) }
                var initialStart by rememberSaveable { mutableStateOf<Int?>(null) }
                var initialEnd by rememberSaveable { mutableStateOf<Int?>(null) }
                var slotDay by rememberSaveable { mutableIntStateOf(LocalDate.now().dayOfWeek.value) }
                var showFree by rememberSaveable { mutableStateOf(false) }
                var showBreak by rememberSaveable { mutableStateOf(false) }
                var breakId by rememberSaveable { mutableStateOf<String?>(null) }
                var now by remember { mutableStateOf(LocalDateTime.now()) }
                val visibleDate = LocalDate.parse(selectedDate)
                val scopeDate = LocalDate.parse(editingBaseDate)
                val scopeLocked = timetable.exceptions.any { it.date == editingBaseDate }
                val scopeControls: @Composable () -> Unit = {
                    EditScopeControl(scopeDate, datedTarget != null, scopeLocked) { editorDate = if (it) editingBaseDate else null }
                }
                fun beginEdit() {
                    editingBaseDate = visibleDate.toString()
                    editorDate = if (timetable.exceptions.any { it.date == editingBaseDate }) editingBaseDate else null
                }
                val editFree: (Int, TimeSpan) -> Unit = { d, time -> beginEdit(); slotDay = d; initialStart = time.start; initialEnd = time.end; showFree = true }
                val editPause: (ScheduledBreak) -> Unit = { pause -> beginEdit(); breakId = pause.id; initialStart = null; initialEnd = null; showBreak = true }
                val achievementContext = androidx.compose.ui.platform.LocalContext.current
                LaunchedEffect(destination) { if (destination == Destination.WEEK) Achievements.mark(achievementContext, Achievement.WEEK_AT_A_GLANCE) }
                LaunchedEffect(state.feedback) {
                    when (val f = state.feedback) {
                        is cc.xdan.tempo.Feedback.Undone -> Achievements.mark(achievementContext, Achievement.SECOND_THOUGHTS)
                        is cc.xdan.tempo.Feedback.Info -> if (f.text.startsWith("Exported")) Achievements.mark(achievementContext, Achievement.SAFE_KEEPING)
                        else -> Unit
                    }
                }
                // Minute-aligned clock: past blocks dim and the now-line moves exactly on the minute.
                LaunchedEffect(Unit) { while (true) { now = LocalDateTime.now(); delay(60_000L - System.currentTimeMillis() % 60_000L) } }
                Scaffold(
                    // The timetable chip is the only way into the timetable switcher; the overflow
                    // holds history and sharing so they never compete with the content.
                    topBar = { TopAppBar(title = { TimetableChip(timetable.name) { showTimetables = true } }, actions = {
                        if (timetable.progress == ProgressMode.STREAKS && destination in listOf(Destination.HOME, Destination.SCHEDULE, Destination.WEEK)) {
                            val summary = remember(timetable, now.toLocalDate()) { cc.xdan.tempo.schedule.Progress.summary(timetable, now.toLocalDate()) }
                            StreakChip(summary) { showProgress = true }
                        }
                        AppOverflowMenu(state.undoLabel, state.redoLabel, state.canUndo, state.canRedo,
                            undo = { model.undo() }, redo = { model.redo() }, shareImage = { showImageExport = true })
                        IconButton(onClick = { destination = Destination.PROFILE }) { ProfileAvatar(state.collection?.profileName ?: "", 32.dp) }
                    }) },
                    snackbarHost = { feedbackHost(Modifier) },
                    bottomBar = { NavigationBar {
                        Destination.entries.filter { it.inBar }.forEach { item ->
                            val selected = destination == item
                            NavigationBarItem(selected = selected, onClick = { if (!selected) { navHaptics.tick(); destination = item } },
                                icon = { Icon(when (item) {
                                    Destination.HOME -> if (selected) Icons.Filled.Home else Icons.Outlined.Home
                                    Destination.SCHEDULE -> if (selected) Icons.Filled.CalendarToday else Icons.Outlined.CalendarToday
                                    Destination.WEEK -> if (selected) Icons.Filled.ViewWeek else Icons.Outlined.ViewWeek
                                    Destination.LIBRARY -> if (selected) Icons.Filled.CollectionsBookmark else Icons.Outlined.CollectionsBookmark
                                    else -> Icons.Outlined.Settings
                                }, null) }, label = { Text(item.label, maxLines = 1) })
                        }
                    } },
                    floatingActionButton = {
                        // Shrinks to an icon while scrolling so it never hides the timeline.
                        if (destination == Destination.SCHEDULE) ExtendedFloatingActionButton(
                            onClick = { navHaptics.press(); beginEdit(); editingSessionId = null; initialStart = null; initialEnd = null; showEditor = true },
                            expanded = fabExpanded, icon = { Icon(Icons.Outlined.Add, "Add ${timetable.sessionWord}") }, text = { Text("Add ${timetable.sessionWord}") })
                    },
                ) { padding ->
                    Box(Modifier.padding(padding).fillMaxSize()) {
                        PredictiveRouteHost(destination, enabled = !showEditor && !showFree && !showBreak && !showTimetables && !showComparison && state.importPreview == null, navigate = { destination = it }) { shown ->
                        when (shown) {
                            Destination.SCHEDULE -> ScheduleScreen(timetable, now, LocalDate.parse(selectedDate), { selectedDate = it.toString() },
                                { beginEdit(); editingSessionId = it.id; initialStart = null; initialEnd = null; showEditor = true },
                                { d, time -> editFree(d.dayOfWeek.value, time) }, editPause, onTopChanged = { fabExpanded = it },
                                people = people,
                                checkIn = { day, session, on ->
                                    val subject = timetable.subjects.firstOrNull { it.id == session.subjectId }?.name ?: timetable.sessionWord
                                    model.update(if (on) "Checked in to $subject" else "Removed check-in") { t ->
                                        val others = t.checkIns.filterNot { it.date == day.toString() && it.sessionId == session.id }
                                        t.copy(checkIns = if (on) others + CheckIn(day.toString(), session.id) else others)
                                    }
                                })
                            Destination.WEEK -> WeekScreen(timetable, now, LocalDate.parse(selectedDate), { selectedDate = it.toString() },
                                { day -> selectedDate = day.toString(); destination = Destination.SCHEDULE }, people)
                            Destination.LIBRARY -> LibraryScreen(timetable) { label, change -> model.update(label, change) }
                            Destination.HOME -> HomeScreen(timetable, now, state.collection?.profileName ?: "", people,
                                { day -> day?.let { selectedDate = it.toString() }; destination = Destination.SCHEDULE }, { destination = Destination.WEEK },
                                { destination = Destination.PROFILE }, { showTimetables = true },
                                { beginEdit(); editingSessionId = it.id; initialStart = null; initialEnd = null; showEditor = true },
                                { time -> editFree(now.dayOfWeek.value, time) }, editPause)
                            Destination.PROFILE -> ProfileScreen(timetable, state.collection?.profileName ?: "", model::setProfileName) { destination = Destination.SETTINGS }
                            Destination.SETTINGS -> SettingsScreen(timetable, model::update, state.collection?.profileName ?: "", model::setProfileName) { forceUpdatePrompt = true }
                        }
                        }
                    }
                }
                if (showEditor) SessionEditor(editorTable, editingSession, datedTarget?.dayOfWeek?.value ?: if (initialStart != null) slotDay else visibleDate.dayOfWeek.value,
                    initialTime = initialStart?.let { a -> initialEnd?.let { b -> TimeSpan(a, b) } },
                    onDismiss = { showEditor = false },
                    onSave = { session -> model.update(sessionLabel(if (editingSession == null) "Added" else "Updated", session.subjectId, editorTable)) { it.editSessions(datedTarget) { sessions -> sessions.filterNot { s -> s.id == session.id } + session } }; showEditor = false },
                    onDelete = { id -> model.update(sessionLabel("Deleted", editorTable.sessions.firstOrNull { s -> s.id == id }?.subjectId, editorTable)) { it.editSessions(datedTarget) { sessions -> sessions.filterNot { s -> s.id == id } } }; showEditor = false },
                    onAddSubject = { subject -> model.update("Added ${subject.name}") { it.copy(subjects = it.subjects + subject) } },
                    initialCycleWeek = ScheduleResolver.cycleWeek(timetable, visibleDate), dated = datedTarget != null,
                    onSplit = { split -> model.update(sessionLabel("Split", split.first().subjectId, editorTable)) { it.editSessions(datedTarget) { sessions -> sessions.filterNot { s -> s.id == split.first().id } + split } }; showEditor = false }, scopeControls = scopeControls)
                if (showFree) FreeSlotEditor(slotDay, TimeSpan(initialStart!!, initialEnd!!), { showFree = false },
                    { showFree = false; editingSessionId = null; showEditor = true }, { showFree = false; breakId = null; showBreak = true })
                if (showBreak) BreakEditor(editorTable, editorTable.breaks.firstOrNull { it.id == breakId },
                    initialDay = datedTarget?.dayOfWeek?.value ?: if (initialStart != null) slotDay else null,
                    initialTime = initialStart?.let { a -> initialEnd?.let { b -> TimeSpan(a, b) } },
                    dismiss = { showBreak = false }, save = { pause -> model.update("${if (editorTable.breaks.any { b -> b.id == pause.id }) "Updated" else "Added"} ${pause.name.ifBlank { "break" }}") { it.editBreaks(datedTarget) { breaks -> breaks.filterNot { b -> b.id == pause.id } + pause } }; showBreak = false },
                    delete = { id -> model.update("Deleted ${editorTable.breaks.firstOrNull { b -> b.id == id }?.name?.ifBlank { null } ?: "break"}") { it.editBreaks(datedTarget) { breaks -> breaks.filterNot { b -> b.id == id } } }; showBreak = false },
                    initialCycleWeek = ScheduleResolver.cycleWeek(timetable, visibleDate), dated = datedTarget != null, scopeControls = scopeControls)
            }
            }
            if (showComparison) state.collection?.let { ComparisonScreen(it) { showComparison = false } }
            WhatsNewOnUpdate(state.collection)
            val (achievementQueue, consumeAchievement) = rememberAchievementQueue(state.collection.takeIf { timetable?.onboarded == true },
                celebrate = timetable?.progress != ProgressMode.OFF)
            AchievementNotices(achievementQueue, consumeAchievement, Modifier.align(Alignment.TopCenter).statusBarsPadding().padding(top = 8.dp))
            val shareContext = androidx.compose.ui.platform.LocalContext.current
            state.sharePreview?.let { SharePreviewDialog(it, {
                model.acceptShare(true) { id, link -> cc.xdan.tempo.share.ShareStore.follow(shareContext, id, link); cc.xdan.tempo.share.ShareSync.ensureScheduled(shareContext) }
            }, { model.acceptShare(false) }, model::dismissSharePreview) }
            // Sync shared timetables: on open, and shortly after your edits settle.
            LaunchedEffect(Unit) { cc.xdan.tempo.share.ShareSync.ensureScheduled(shareContext); cc.xdan.tempo.share.ShareSync.soon(shareContext, 0) }
            LaunchedEffect(state.collection) {
                if (state.collection != null && cc.xdan.tempo.share.ShareStore.allOwned(shareContext).isNotEmpty()) cc.xdan.tempo.share.ShareSync.soon(shareContext)
            }
            // Tell people once when someone stopped sharing and their copy was removed.
            LaunchedEffect(state.collection) {
                val prefs = shareContext.getSharedPreferences("shares", android.content.Context.MODE_PRIVATE)
                prefs.getString("removedNotice", null)?.let { names -> model.info("$names stopped sharing, so their timetable was removed"); prefs.edit().remove("removedNotice").apply() }
            }
            if (state.openingShare) Surface(Modifier.align(Alignment.BottomCenter).safeDrawingPadding().padding(20.dp), shape = MaterialTheme.shapes.medium) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    LoadingIndicator(Modifier.size(32.dp)); Text("Opening shared timetable…")
                }
            }
            // The updater waits until setup is finished, and never shows over the What's new sheet's first launch.
            if (timetable?.onboarded == true) UpdatePrompt(forceUpdatePrompt) { forceUpdatePrompt = false }
            if (showProgress && timetable != null) ProgressSheet(timetable, LocalDate.now()) { showProgress = false }
            if (showImageExport && timetable != null) PngExportSheet(timetable, { showImageExport = false }, model::info)
            if (showTimetables) state.collection?.let { TimetableManager(it, model, files, { showComparison = true }) { showTimetables = false } }
            if (state.fileBusy) Surface(Modifier.align(Alignment.BottomCenter).safeDrawingPadding().padding(20.dp), shape = MaterialTheme.shapes.medium) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    LoadingIndicator(Modifier.size(32.dp)); Text("Working with your timetable file…")
                }
            }
            // Outside the main Scaffold (onboarding, loading) feedback sits above the system bars.
            if (timetable?.onboarded != true) feedbackHost(Modifier.align(Alignment.BottomCenter).safeDrawingPadding().padding(16.dp))
            val errorHaptics = rememberTempoHaptics()
            LaunchedEffect(state.error) { if (state.error != null) errorHaptics.reject() }
            if (state.error != null && timetable != null) AlertDialog(onDismissRequest = model::dismissError,
                title = { Text("Something needs attention") }, text = { Text(state.error!!) },
                confirmButton = { TextButton(onClick = model::dismissError) { Text("Close") } })
            }
        }
    }
}

/** "Added Physics session", falling back to "session" when the subject is unknown. */
private fun sessionLabel(verb: String, subjectId: String?, table: Timetable): String =
    "$verb ${table.subjects.firstOrNull { it.id == subjectId }?.name?.let { "$it session" } ?: "session"}"

/** The single entry point to switching and managing timetables. */
@Composable
private fun TimetableChip(name: String, open: () -> Unit) {
    val interaction = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    Surface(onClick = open, shape = androidx.compose.foundation.shape.CircleShape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh, interactionSource = interaction,
        modifier = Modifier.pressScale(interaction).semantics { contentDescription = "Timetable: $name. Switch or manage timetables" }) {
        Row(Modifier.padding(start = 16.dp, end = 10.dp, top = 8.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(name, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                modifier = Modifier.widthIn(max = 220.dp))
            Icon(Icons.Outlined.ExpandMore, null, Modifier.size(20.dp))
        }
    }
}

/** History and sharing, kept out of the way. Undo/Redo stay here after a snackbar has gone. */
@Composable
private fun AppOverflowMenu(undoLabel: String?, redoLabel: String?, canUndo: Boolean, canRedo: Boolean,
    undo: () -> Unit, redo: () -> Unit, shareImage: () -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { open = true }) { Icon(Icons.Outlined.MoreVert, "More options") }
        DropdownMenu(open, { open = false }, shape = MaterialTheme.shapes.medium) {
            DropdownMenuItem(text = { MenuLabel("Undo", undoLabel) }, enabled = canUndo, onClick = { open = false; undo() },
                leadingIcon = { Icon(Icons.AutoMirrored.Outlined.Undo, null) })
            DropdownMenuItem(text = { MenuLabel("Redo", redoLabel) }, enabled = canRedo, onClick = { open = false; redo() },
                leadingIcon = { Icon(Icons.AutoMirrored.Outlined.Redo, null) })
            HorizontalDivider()
            DropdownMenuItem(text = { Text("Share as image") }, onClick = { open = false; shareImage() },
                leadingIcon = { Icon(Icons.Outlined.Image, null) })
        }
    }
}

@Composable
private fun MenuLabel(action: String, detail: String?) {
    Column {
        Text(action)
        if (detail != null) Text(detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis, modifier = Modifier.widthIn(max = 200.dp))
    }
}
