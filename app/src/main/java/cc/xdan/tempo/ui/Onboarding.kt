package cc.xdan.tempo.ui

import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import cc.xdan.tempo.design.TempoMotion
import cc.xdan.tempo.design.rememberTempoHaptics
import cc.xdan.tempo.design.subjectColours
import cc.xdan.tempo.design.subjectForeground
import cc.xdan.tempo.design.subjectOutline
import cc.xdan.tempo.model.*
import cc.xdan.tempo.schedule.removeSubject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.util.UUID

private object Step { const val WELCOME = 0; const val PURPOSE = 1; const val DAYS = 2; const val HOURS = 3; const val PERIODS = 4
    const val PERIOD_TIMES = 5; const val SUBJECTS = 6; const val THEME = 7; const val PROGRESS = 8; const val DONE = 9 }

private fun icon(p: TimetablePurpose): ImageVector = when (p) {
    TimetablePurpose.SCHOOL -> Icons.Outlined.Backpack; TimetablePurpose.COLLEGE -> Icons.Outlined.School
    TimetablePurpose.WORK -> Icons.Outlined.Work; TimetablePurpose.PERSONAL -> Icons.Outlined.SelfImprovement
}

private fun defaultHours(p: TimetablePurpose?) = when (p) {
    TimetablePurpose.SCHOOL -> TimeSpan(525, 915); TimetablePurpose.COLLEGE -> TimeSpan(540, 990)
    TimetablePurpose.WORK -> TimeSpan(540, 1050); TimetablePurpose.PERSONAL -> TimeSpan(480, 1320); null -> TimeSpan(540, 990)
}

private fun suggestions(p: TimetablePurpose?) = when (p) {
    TimetablePurpose.SCHOOL -> listOf("Maths", "English", "Science", "History", "Geography", "Art", "Music", "PE", "Computing", "French", "Spanish", "Drama")
    TimetablePurpose.COLLEGE -> listOf("Lecture", "Seminar", "Lab", "Tutorial", "Computer Science", "Physics", "Psychology", "Business", "Media", "Study")
    TimetablePurpose.WORK -> listOf("Shift", "Meeting", "Focus time", "Training", "Commute", "Lunch with team", "Admin", "Calls")
    TimetablePurpose.PERSONAL, null -> listOf("Gym", "Run", "Reading", "Music practice", "Cooking", "Study", "Family", "Walk")
}

/** Steps shown for this purpose: work and personal timetables skip periods. */
private fun stepsFor(p: TimetablePurpose?) = (0 until SETUP_STEPS).filter {
    (it != Step.PERIODS && it != Step.PERIOD_TIMES) || p == TimetablePurpose.SCHOOL || p == TimetablePurpose.COLLEGE || p == null }

/**
 * First-run setup: one question per screen, smart defaults from the timetable's purpose, a live
 * preview of the person's own week and a short celebration at the end.
 */
@Composable
fun Onboarding(timetable: Timetable, update: ((Timetable) -> Timetable) -> Unit, importTimetable: () -> Unit, manageTimetables: (() -> Unit)? = null) {
    val step = timetable.currentSetupStep
    val steps = stepsFor(timetable.purpose)
    val focus = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    val haptics = rememberTempoHaptics()
    var name by rememberSaveable { mutableStateOf(timetable.name) }
    var days by rememberSaveable { mutableStateOf(timetable.days.map { it.day }.joinToString(",")) }
    var start by rememberSaveable { mutableStateOf(minuteLabel(timetable.days.firstOrNull()?.time?.start ?: 540)) }
    var end by rememberSaveable { mutableStateOf(minuteLabel(timetable.days.firstOrNull()?.time?.end ?: 990)) }
    var alternate by rememberSaveable { mutableStateOf(timetable.cycle.weeks > 1) }
    var weeks by rememberSaveable { mutableStateOf(timetable.cycle.weeks.coerceAtLeast(2).toString()) }
    var anchor by rememberSaveable { mutableStateOf(timetable.cycle.anchorDate) }
    var subjectEditor by remember { mutableStateOf(false) }
    var periodCount by rememberSaveable { mutableIntStateOf(4) }
    var breakLength by rememberSaveable { mutableStateOf<Int?>(20) }
    var lunchLength by rememberSaveable { mutableStateOf<Int?>(50) }
    var changeoverLength by rememberSaveable { mutableStateOf<Int?>(10) }
    val plan = remember(periodCount, breakLength, lunchLength, changeoverLength, timetable.days) {
        timetable.days.firstOrNull()?.time?.let { day ->
            cc.xdan.tempo.schedule.PeriodPlanner.plan(day, periodCount, cc.xdan.tempo.schedule.BreakLengths(breakLength, lunchLength, changeoverLength), timetable.days.map { it.day })
        }
    }
    var selectedSubject by remember { mutableStateOf<Subject?>(null) }
    val selectedDays = days.split(",").mapNotNull { it.toIntOrNull() }
    val highestWeek = (timetable.sessions.mapNotNull { it.cycleWeek } + timetable.breaks.mapNotNull { it.cycleWeek }).maxOrNull() ?: 1
    val cycleValid = !alternate || (weeks.toIntOrNull()?.let { it in maxOf(2, highestWeek)..52 } == true && validDate(anchor) != null)

    fun goTo(target: Int) { focus.clearFocus(); keyboard?.hide(); update { it.copy(setupStep = target) } }
    fun next() {
        val index = steps.indexOf(step)
        when (step) {
            Step.DAYS -> update { it.copy(name = name.trim(), days = selectedDays.map { d -> DayHours(d, it.days.firstOrNull()?.time ?: defaultHours(it.purpose)) }) }
            Step.HOURS -> update { t -> t.copy(days = selectedDays.map { d -> DayHours(d, validSpan(start, end)!!) },
                cycle = if (alternate) WeekCycle(weeks.toInt(), anchor) else WeekCycle(1, anchor)) }
            // The suggested plan becomes real periods and breaks, editable on the next screen.
            Step.PERIODS -> plan?.let { p -> update { it.copy(periods = p.periods, breaks = p.breaks) } }
        }
        goTo(steps.getOrElse(index + 1) { Step.DONE })
    }
    fun back() { steps.getOrNull(steps.indexOf(step) - 1)?.let { goTo(it) } }

    val backProgress = remember { Animatable(0f) }
    val latestStep by rememberUpdatedState(step)
    PredictiveBackHandler(enabled = step > Step.WELCOME && !subjectEditor) { events ->
        try {
            events.collect { event -> backProgress.snapTo(event.progress) }
            if (latestStep == step) back()
            backProgress.snapTo(0f)
        } catch (cancelled: CancellationException) {
            withContext(NonCancellable) { backProgress.animateTo(0f, TempoMotion.spatial()) }
        }
    }

    val canContinue = when (step) {
        Step.PURPOSE -> timetable.purpose != null
        Step.DAYS -> name.isNotBlank() && selectedDays.isNotEmpty()
        Step.HOURS -> validSpan(start, end) != null && cycleValid
        Step.PERIODS -> plan != null
        else -> true
    }
    Column(Modifier.fillMaxSize().safeDrawingPadding().imePadding()) {
        if (step != Step.WELCOME && step != Step.DONE) SetupTopBar(steps.indexOf(step), steps.size, ::back)
        AnimatedContent(step, Modifier.weight(1f).graphicsLayer {
            scaleX = 1f - .05f * backProgress.value; scaleY = scaleX; alpha = 1f - .15f * backProgress.value
        }, label = "setup step", transitionSpec = {
            val direction = if (targetState > initialState) 1 else -1
            (slideInHorizontally(TempoMotion.slide) { it * direction / 4 } + fadeIn(TempoMotion.effects())) togetherWith
                (slideOutHorizontally(TempoMotion.slide) { -it * direction / 5 } + fadeOut(TempoMotion.exit()))
        }) { shown ->
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)) {
                when (shown) {
                    Step.WELCOME -> Welcome(importTimetable, manageTimetables)
                    Step.PURPOSE -> {
                        Question("What's this timetable for?", "Tempo uses this to pick sensible defaults and words. You can change it later.")
                        TimetablePurpose.entries.forEach { purpose ->
                            OptionCard(icon(purpose), purpose.label, when (purpose) {
                                TimetablePurpose.SCHOOL -> "Lessons, periods and breaks"
                                TimetablePurpose.COLLEGE -> "Lectures, seminars and rooms"
                                TimetablePurpose.WORK -> "Shifts, meetings and focus time"
                                TimetablePurpose.PERSONAL -> "Routines, hobbies and plans"
                            }, timetable.purpose == purpose) {
                                val hours = defaultHours(purpose)
                                val sameName = name == timetable.name && (name == "My timetable" || TimetablePurpose.entries.any { it.label == name || it.name.lowercase().replaceFirstChar(Char::titlecase) == name })
                                if (sameName) name = when (purpose) { TimetablePurpose.COLLEGE -> "College"; else -> purpose.name.lowercase().replaceFirstChar(Char::titlecase) }
                                days = (if (purpose == TimetablePurpose.PERSONAL) 1..7 else 1..5).joinToString(",")
                                start = minuteLabel(hours.start); end = minuteLabel(hours.end)
                                update { it.copy(purpose = purpose, periods = if (purpose == TimetablePurpose.WORK || purpose == TimetablePurpose.PERSONAL) emptyList() else it.periods) }
                            }
                        }
                    }
                    Step.DAYS -> {
                        Question("Name it and pick your days", "Each day's hours can be adjusted later.")
                        OutlinedTextField(name, { name = it }, label = { Text("Timetable name") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            weekdayNames.forEachIndexed { i, day ->
                                val on = i + 1 in selectedDays
                                DayToggle(day.take(1), day, on, Modifier.weight(1f)) {
                                    days = (if (on) selectedDays - (i + 1) else selectedDays + (i + 1)).sorted().joinToString(",")
                                }
                            }
                        }
                        if (selectedDays.isEmpty()) Text("Choose at least one day.", color = MaterialTheme.colorScheme.error)
                    }
                    Step.HOURS -> {
                        Question("When does your day run?", "Free time is worked out inside these hours.")
                        TimeFields(start, end, { start = it }, { end = it })
                        Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).hapticToggleable(alternate, role = Role.Switch) { alternate = it }
                            .background(MaterialTheme.colorScheme.surfaceContainerHigh).padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text("My weeks alternate", style = MaterialTheme.typography.titleMedium)
                                Text("For A/B weeks or longer cycles", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Switch(alternate, null)
                        }
                        AnimatedVisibility(alternate, enter = expandVertically(TempoMotion.spatial()) + fadeIn(), exit = shrinkVertically(TempoMotion.spatial()) + fadeOut()) {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                CycleWeekFields(weeks, { weeks = it })
                                DateField("A date in week 1", anchor) { anchor = it }
                                if (!cycleValid) Text("Choose a valid date and ${maxOf(2, highestWeek)}–52 weeks.", color = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                    Step.PERIODS -> {
                        Question("How many periods in a day?", "Tempo will suggest times that fit your day.")
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            (3..8).forEach { count ->
                                NumberToggle(count.toString(), count == periodCount, if (count == 4) "Suggested" else null, Modifier.weight(1f)) { periodCount = count }
                            }
                        }
                        Text("Breaks between periods", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 8.dp))
                        BreakLengthRow("Break", Icons.Outlined.Coffee, breakLength, listOf(15, 20, 30), 20) { breakLength = it }
                        BreakLengthRow("Lunch", Icons.Outlined.Restaurant, lunchLength, listOf(30, 45, 50, 60), 50) { lunchLength = it }
                        BreakLengthRow("Changeover", Icons.Outlined.SwapHoriz, changeoverLength, listOf(5, 10, 15), 10) { changeoverLength = it }
                        if (plan == null) Text("These don't fit in your day. Try fewer periods or shorter breaks.", color = MaterialTheme.colorScheme.error)
                        else PlanPreview(plan)
                    }
                    Step.PERIOD_TIMES -> {
                        Question("Your period times", "Tap any period to rename it, change its times, or add a room and colour.")
                        PeriodSetup(timetable, update)
                    }
                    Step.SUBJECTS -> SubjectStep(timetable, update, { selectedSubject = it; subjectEditor = true })
                    Step.THEME -> {
                        Question("Make it yours", "Pick colours. This is your week with them.")
                        WeekPreview(timetable)
                        AppearanceControls(timetable.appearance) { appearance -> update { it.copy(appearance = appearance) } }
                    }
                    Step.PROGRESS -> {
                        Question("Celebrate your progress?", "Optional, and you can change it any time in Settings.")
                        OptionCard(Icons.Outlined.DoNotDisturbOn, "Keep it simple", "Just your timetable", timetable.progress == ProgressMode.OFF) {
                            update { it.copy(progress = ProgressMode.OFF) } }
                        OptionCard(Icons.Outlined.Celebration, "Celebrations", "Confetti when your day is done, and milestones", timetable.progress == ProgressMode.CELEBRATIONS) {
                            update { it.copy(progress = ProgressMode.CELEBRATIONS) } }
                        OptionCard(Icons.Outlined.LocalFireDepartment, "Celebrations, streaks and XP",
                            "Check in to ${timetable.sessionWord}s to earn XP and keep a gentle streak. Days off never break it.", timetable.progress == ProgressMode.STREAKS) {
                            update { it.copy(progress = ProgressMode.STREAKS) } }
                    }
                    Step.DONE -> Finished(timetable)
                }
            }
        }
        val label = when (step) {
            Step.WELCOME -> "Get started"
            Step.SUBJECTS -> if (timetable.subjects.isEmpty()) "Skip for now" else "Continue"
            Step.DONE -> "Open my schedule"
            else -> "Continue"
        }
        Button(enabled = canContinue, onClick = {
            if (step == Step.DONE) { haptics.confirm(); update { it.copy(onboarded = true) } } else next()
        }, modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp).height(56.dp)) {
            Text(label, style = MaterialTheme.typography.titleMedium)
        }
    }
    if (subjectEditor) SubjectEditor(selectedSubject, (timetable.sessions + timetable.exceptions.flatMap { it.sessions }).count { it.subjectId == selectedSubject?.id }, { subjectEditor = false },
        { subject -> update { it.copy(subjects = it.subjects.filterNot { old -> old.id == subject.id } + subject) }; subjectEditor = false },
        { id -> update { it.removeSubject(id) }; subjectEditor = false })
}

@Composable
private fun SetupTopBar(index: Int, count: Int, back: () -> Unit) {
    val progress by animateFloatAsState((index + 1f) / count, TempoMotion.spatial(), label = "setup progress")
    Row(Modifier.fillMaxWidth().padding(start = 4.dp, end = 24.dp, top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = back) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Back") }
        LinearProgressIndicator(progress = { progress }, modifier = Modifier.weight(1f).height(6.dp), strokeCap = androidx.compose.ui.graphics.StrokeCap.Round)
    }
}

@Composable
private fun Question(title: String, detail: String) {
    Column(Modifier.padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(title, style = MaterialTheme.typography.headlineMedium)
        Text(detail, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun OptionCard(icon: ImageVector, title: String, detail: String, selected: Boolean, choose: () -> Unit) {
    val container by animateColorAsState(if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh, TempoMotion.effects(), label = "option")
    val scale by animateFloatAsState(if (selected) 1f else .98f, TempoMotion.fastSpatial(), label = "option scale")
    Surface(onClick = choose, shape = RoundedCornerShape(24.dp), color = container,
        border = if (selected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
        modifier = Modifier.fillMaxWidth().graphicsLayer { scaleX = scale; scaleY = scale }.semantics { this.selected = selected; role = Role.RadioButton }) {
        Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Surface(shape = CircleShape, color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHighest,
                contentColor = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(48.dp)) {
                Box(contentAlignment = Alignment.Center) { Icon(icon, null) }
            }
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(detail, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            AnimatedVisibility(selected, enter = scaleIn(TempoMotion.fastSpatial()) + fadeIn(), exit = scaleOut() + fadeOut()) {
                Icon(Icons.Outlined.CheckCircle, null, tint = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@Composable
private fun DayToggle(letter: String, name: String, on: Boolean, modifier: Modifier, toggle: () -> Unit) {
    val container by animateColorAsState(if (on) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHigh, TempoMotion.effects(), label = "day")
    Surface(onClick = toggle, shape = CircleShape, color = container,
        contentColor = if (on) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
        modifier = modifier.aspectRatio(1f).semantics { selected = on; role = Role.Checkbox; this.contentDescription = name }) {
        Box(contentAlignment = Alignment.Center) { Text(letter, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
    }
}

@Composable
private fun Welcome(importTimetable: () -> Unit, manageTimetables: (() -> Unit)?) {
    Spacer(Modifier.height(24.dp))
    // A small stack of sample blocks springs into place, previewing what Tempo makes.
    val sample = listOf(Triple("Maths", 0xFFA2DFFF, Icons.Outlined.Calculate), Triple("Break", 0xFFFFB6C6, Icons.Outlined.Coffee),
        Triple("Art", 0xFFFFD59B, Icons.Outlined.Palette), Triple("Music", 0xFFD3BCFF, Icons.Outlined.MusicNote))
    var shown by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { delay(120); shown = true }
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        sample.forEachIndexed { index, (label, colour, icon) ->
            val offset by animateFloatAsState(if (shown) 0f else 1f, spring(dampingRatio = .6f, stiffness = 260f - index * 30f), label = "sample $index")
            val fill = Color(colour)
            Surface(shape = RoundedCornerShape(22.dp), color = fill, contentColor = subjectForeground(fill),
                modifier = Modifier.fillMaxWidth(if (index == 1) .7f else 1f).height(if (index == 1) 44.dp else 64.dp)
                    .graphicsLayer { translationY = offset * 220f * (index + 1); alpha = 1f - offset; rotationZ = offset * (if (index % 2 == 0) -6f else 6f) }) {
                Row(Modifier.padding(horizontal = 18.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(icon, null); Text(label, style = MaterialTheme.typography.titleMedium)
                }
            }
        }
    }
    Spacer(Modifier.height(16.dp))
    Text("Your week, in view.", style = MaterialTheme.typography.displaySmall)
    Text("A timetable for school, college, work or anything that fills your days. It stays on your device.",
        style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
    TextButton(onClick = importTimetable) { Icon(Icons.Outlined.FileOpen, null); Spacer(Modifier.width(8.dp)); Text("Import a Tempo timetable") }
    manageTimetables?.let { TextButton(onClick = it) { Text("Choose another timetable") } }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SubjectStep(timetable: Timetable, update: ((Timetable) -> Timetable) -> Unit, edit: (Subject?) -> Unit) {
    val word = timetable.subjectWord
    Question("What fills your week?", "Tap to add ${word}s. Colours and icons are picked for you and can be changed.")
    val taken = timetable.subjects.map { it.name.lowercase() }.toSet()
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        suggestions(timetable.purpose).filter { it.lowercase() !in taken }.forEach { suggestion ->
            AssistChipWithHaptic(suggestion) {
                update { t ->
                    val colour = subjectColours[t.subjects.size % subjectColours.size].argb
                    t.copy(subjects = t.subjects + Subject(UUID.randomUUID().toString(), suggestion, colour, suggestedSubjectIcon(suggestion)))
                }
            }
        }
        AssistChipWithHaptic("Your own…", Icons.Outlined.Add) { edit(null) }
    }
    timetable.subjects.forEach { subject ->
        val fill = Color(subject.colour).compositeOver(MaterialTheme.colorScheme.surface)
        Surface(onClick = { edit(subject) }, shape = RoundedCornerShape(20.dp), color = fill, contentColor = subjectForeground(fill),
            modifier = Modifier.fillMaxWidth().animateContentSize()) {
            Row(Modifier.padding(horizontal = 16.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Icon(subject.icon.vector() ?: Icons.Outlined.GridView, null)
                Column(Modifier.weight(1f)) {
                    Text(subject.name, style = MaterialTheme.typography.titleMedium)
                    if (subject.location.isNotBlank()) Text(subject.location, style = MaterialTheme.typography.bodySmall)
                }
                Icon(Icons.Outlined.Edit, "Edit ${subject.name}", Modifier.size(20.dp))
            }
        }
    }
}

@Composable
private fun AssistChipWithHaptic(label: String, icon: ImageVector? = null, onClick: () -> Unit) {
    FilterChip(false, onClick, label = { Text(label) }, leadingIcon = icon?.let { { Icon(it, null, Modifier.size(18.dp)) } })
}

/** The person's own subjects laid out as a mini week, recoloured live as they pick a theme. */
@Composable
private fun WeekPreview(timetable: Timetable) {
    val subjects = timetable.subjects.ifEmpty { listOf(Subject("a", "Your first", 0xFFA2DFFF), Subject("b", "Another", 0xFFFFD59B)) }
    val days = timetable.days.map { it.day }.ifEmpty { (1..5).toList() }.take(5)
    Surface(shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surfaceContainer) {
        Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            days.forEachIndexed { column, day ->
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(weekdayNames[day - 1].take(1), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    (0 until 3).forEach { row ->
                        val subject = subjects[(column + row * 2) % subjects.size]
                        Box(Modifier.fillMaxWidth().height(if ((column + row) % 3 == 0) 34.dp else 24.dp).clip(RoundedCornerShape(8.dp))
                            .background(Color(subject.colour).compositeOver(MaterialTheme.colorScheme.surface)))
                    }
                }
            }
        }
    }
}

@Composable
private fun Finished(timetable: Timetable) {
    val haptics = rememberTempoHaptics()
    var shown by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { delay(150); shown = true; haptics.celebrate() }
    val scale by animateFloatAsState(if (shown) 1f else .3f, spring(dampingRatio = .45f, stiffness = 300f), label = "done")
    val rotation by animateFloatAsState(if (shown) 0f else -90f, TempoMotion.spatial(), label = "done rotation")
    Column(Modifier.fillMaxWidth().padding(top = 64.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        SetupDoneBadge(Modifier.size(120.dp).graphicsLayer { scaleX = scale; scaleY = scale; rotationZ = rotation })
        Text("You're all set", style = MaterialTheme.typography.displaySmall, textAlign = TextAlign.Center)
        val dayText = timetable.days.map { it.day }.sorted().let { list ->
            if (list == (1..5).toList()) "Mon–Fri" else if (list == (1..7).toList()) "Every day" else list.joinToString(", ") { weekdayNames[it - 1].take(3) } }
        val hours = timetable.days.firstOrNull()?.time?.let { "${minuteLabel(it.start)}–${minuteLabel(it.end)}" } ?: ""
        Text(listOfNotNull(timetable.subjects.takeIf { it.isNotEmpty() }?.let { countLabel(it.size, timetable.subjectWord) }, dayText, hours).joinToString(" · "),
            style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
        Text("Add your first ${timetable.sessionWord} from Schedule whenever you're ready.", style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun SetupDoneBadge(modifier: Modifier) {
    Box(modifier.clip(MaterialShapes.Cookie9Sided.toShape()).background(MaterialTheme.colorScheme.primaryContainer), contentAlignment = Alignment.Center) {
        Icon(Icons.Outlined.Check, null, Modifier.size(56.dp), tint = MaterialTheme.colorScheme.onPrimaryContainer)
    }
}

@Composable
private fun NumberToggle(label: String, selected: Boolean, hint: String?, modifier: Modifier, choose: () -> Unit) {
    val container by animateColorAsState(if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHigh, TempoMotion.effects(), label = "number")
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Surface(onClick = choose, shape = RoundedCornerShape(18.dp), color = container,
            contentColor = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.fillMaxWidth().aspectRatio(1f).semantics { this.selected = selected; role = Role.RadioButton; if (hint != null) contentDescription = "$label, $hint" }) {
            Box(contentAlignment = Alignment.Center) { Text(label, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) }
        }
        // A dot marks the suggested choice; its name is announced instead of squeezed under the number.
        Box(Modifier.size(6.dp).clip(CircleShape).background(if (hint != null) MaterialTheme.colorScheme.primary else Color.Transparent))
    }
}

/** A break type that can be switched off, with suggested lengths. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BreakLengthRow(label: String, icon: ImageVector, value: Int?, options: List<Int>, suggested: Int, change: (Int?) -> Unit) {
    Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surfaceContainerHigh) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(Modifier.fillMaxWidth().hapticToggleable(value != null, role = Role.Switch) { change(if (it) suggested else null) },
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Icon(icon, null)
                Text(label, Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
                Switch(value != null, null)
            }
            AnimatedVisibility(value != null, enter = expandVertically(TempoMotion.spatial()) + fadeIn(), exit = shrinkVertically(TempoMotion.spatial()) + fadeOut()) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    options.forEach { minutes ->
                        FilterChip(value == minutes, { change(minutes) }, label = { Text(if (minutes == suggested) "$minutes min · suggested" else "$minutes min") })
                    }
                }
            }
        }
    }
}

@Composable
private fun PlanPreview(plan: cc.xdan.tempo.schedule.PeriodPlanner.Plan) {
    val rows = (plan.periods.map { Triple(it.name, it.time, null as BreakKind?) } + plan.breaks.map { Triple(it.name, it.time, it.kind) }).sortedBy { it.second.start }
    Surface(shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surfaceContainer) {
        Column(Modifier.fillMaxWidth().padding(16.dp).animateContentSize(TempoMotion.spatial()), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Suggested day", style = MaterialTheme.typography.titleMedium)
            rows.forEach { (name, time, kind) ->
                val colour = kind?.let(::breakColour)
                Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                    .background(colour?.compositeOver(MaterialTheme.colorScheme.surface)?.copy(alpha = .9f) ?: MaterialTheme.colorScheme.primaryContainer)
                    .padding(horizontal = 12.dp, vertical = if (kind == null) 10.dp else 5.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(name, Modifier.weight(1f), style = if (kind == null) MaterialTheme.typography.titleSmall else MaterialTheme.typography.labelMedium,
                        color = if (colour != null) subjectForeground(colour) else MaterialTheme.colorScheme.onPrimaryContainer)
                    Text("${minuteLabel(time.start)}–${minuteLabel(time.end)}", style = MaterialTheme.typography.labelMedium,
                        color = if (colour != null) subjectForeground(colour) else MaterialTheme.colorScheme.onPrimaryContainer)
                }
            }
        }
    }
}
