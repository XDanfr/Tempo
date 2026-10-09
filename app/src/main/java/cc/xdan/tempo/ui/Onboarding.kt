package cc.xdan.tempo.ui

import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.ui.graphics.graphicsLayer
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.withContext
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarViewWeek
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import cc.xdan.tempo.model.*

@Composable
fun Onboarding(timetable: Timetable, update: ((Timetable) -> Timetable) -> Unit) {
    val step = timetable.onboardingStep
    var name by rememberSaveable { mutableStateOf(timetable.name) }
    var days by rememberSaveable { mutableStateOf(timetable.days.map { it.day }.joinToString(",")) }
    var start by rememberSaveable { mutableStateOf(minuteLabel(timetable.days.firstOrNull()?.time?.start ?: 540)) }
    var end by rememberSaveable { mutableStateOf(minuteLabel(timetable.days.firstOrNull()?.time?.end ?: 990)) }
    var subjectEditor by remember { mutableStateOf(false) }
    val backProgress = remember { Animatable(0f) }
    val latestStep by rememberUpdatedState(step)
    PredictiveBackHandler(enabled = step > 0 && !subjectEditor) { events ->
        try {
            events.collect { event -> backProgress.snapTo(event.progress) }
            update { it.copy(onboardingStep = (latestStep - 1).coerceAtLeast(0)) }
            backProgress.snapTo(0f)
        } catch (cancelled: CancellationException) {
            withContext(NonCancellable) { backProgress.animateTo(0f, spring(dampingRatio = .85f, stiffness = 550f)) }
        }
    }
    val selectedDays = days.split(",").mapNotNull { it.toIntOrNull() }
    Column(Modifier.fillMaxSize().safeDrawingPadding().padding(24.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(Icons.Outlined.CalendarViewWeek, null, Modifier.size(36.dp))
            Column { Text("Tempo", style = MaterialTheme.typography.headlineLarge); Text("Part of Axis", color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
        LinearProgressIndicator(progress = { (step + 1) / 4f }, modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp))
        AnimatedContent(step, modifier = Modifier.weight(1f).graphicsLayer {
            scaleX = 1f - .05f * backProgress.value; scaleY = scaleX
            alpha = 1f - .15f * backProgress.value
        }, label = "setup step", transitionSpec = {
            val direction = if (targetState > initialState) 1 else -1
            (slideInHorizontally(spring(dampingRatio = .88f, stiffness = 500f)) { it * direction / 4 } + fadeIn(tween(170))) togetherWith
                (slideOutHorizontally(tween(130)) { -it * direction / 5 } + fadeOut(tween(100)))
        }) { shownStep ->
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            when (shownStep) {
                0 -> {
                    Text("A week that works for you", style = MaterialTheme.typography.headlineMedium)
                    Text("College, work or whatever fills your day. Start with your usual hours; each day can be adjusted later.")
                    OutlinedTextField(name, { name = it }, label = { Text("Timetable name") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        weekdayNames.forEachIndexed { i, day -> FilterChip(i + 1 in selectedDays, {
                            val next = if (i + 1 in selectedDays) selectedDays - (i + 1) else selectedDays + (i + 1)
                            days = next.sorted().joinToString(",")
                        }, label = { Text(day.take(3)) }) }
                    }
                    TimeFields(start, end, { start = it }, { end = it })
                }
                1 -> {
                    Text("Shape your day", style = MaterialTheme.typography.headlineMedium)
                    Text("Set your usual periods and the breaks between them. Tempo calculates the remaining free time for you.")
                    PeriodSetup(timetable, update)
                }
                2 -> {
                    Text("What fills your week?", style = MaterialTheme.typography.headlineMedium)
                    Text("Add subjects or work activities, with optional icons, colours and default locations. You can schedule them after setup.")
                    timetable.subjects.forEach { subject -> Text(subject.name, style = MaterialTheme.typography.titleMedium) }
                    Button(onClick = { subjectEditor = true }) { Text("Add subject / activity") }
                }
                3 -> {
                    Text("Find your colour", style = MaterialTheme.typography.headlineMedium)
                    Text("A calm forest palette, a warmer preset or colours from your wallpaper.")
                    AppearanceControls(timetable.appearance) { appearance -> update { it.copy(appearance = appearance) } }
                    Text("Your timetable stays on this device. Setup can be changed whenever you need.")
                }
            }
        }
        }
        Row(Modifier.fillMaxWidth().padding(top = 16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            if (step > 0) TextButton(onClick = { update { it.copy(onboardingStep = step - 1) } }) { Text("Back") } else Text("1 / 4")
            Button(enabled = step != 0 || (name.isNotBlank() && validSpan(start, end) != null && selectedDays.isNotEmpty()), onClick = {
                if (step == 0) update { it.copy(name = name.trim(), days = selectedDays.map { d -> DayHours(d, validSpan(start, end)!!) }, onboardingStep = 1) }
                else if (step < 3) update { it.copy(onboardingStep = step + 1) }
                else update { it.copy(onboarded = true) }
            }) { Text(if (step == 3) "Start using Tempo" else if (step == 2 && timetable.subjects.isEmpty()) "Add subjects later" else "Continue") }
        }
    }
    if (subjectEditor) SubjectEditor(null, 0, { subjectEditor = false }, { subject -> update { it.copy(subjects = it.subjects + subject) }; subjectEditor = false }, {})
}
