package cc.xdan.tempo.ui

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
import cc.xdan.tempo.data.demoTimetable
import cc.xdan.tempo.model.*

@Composable
fun Onboarding(timetable: Timetable, update: ((Timetable) -> Timetable) -> Unit) {
    val step = timetable.onboardingStep
    var name by rememberSaveable { mutableStateOf(timetable.name) }
    var days by rememberSaveable { mutableStateOf(timetable.days.map { it.day }.joinToString(",")) }
    var start by rememberSaveable { mutableStateOf("09:00") }
    var end by rememberSaveable { mutableStateOf("16:30") }
    var subjectEditor by remember { mutableStateOf(false) }
    val selectedDays = days.split(",").mapNotNull { it.toIntOrNull() }
    Column(Modifier.fillMaxSize().safeDrawingPadding().padding(24.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(Icons.Outlined.CalendarViewWeek, null, Modifier.size(36.dp))
            Column { Text("Tempo", style = MaterialTheme.typography.headlineLarge); Text("Part of Axis", color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
        LinearProgressIndicator(progress = { (step + 1) / 3f }, modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp))
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            when (step) {
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
                    Text("Or explore a fictional college timetable first:", style = MaterialTheme.typography.bodySmall)
                    OutlinedButton(onClick = { update { demoTimetable(it.appearance) } }) { Text("Try the demo") }
                }
                1 -> {
                    Text("What fills your week?", style = MaterialTheme.typography.headlineMedium)
                    Text("Add subjects or work activities, with optional icons, colours and default locations. You can schedule them after setup.")
                    timetable.subjects.forEach { subject -> Text(subject.name, style = MaterialTheme.typography.titleMedium) }
                    Button(onClick = { subjectEditor = true }) { Text("Add subject / activity") }
                    Text("Usual period shortcuts are ready in Settings. Custom session times are always available.", style = MaterialTheme.typography.bodySmall)
                }
                2 -> {
                    Text("Find your colour", style = MaterialTheme.typography.headlineMedium)
                    Text("A calm forest palette, a warmer preset or colours from your wallpaper.")
                    AppearanceControls(timetable.appearance) { appearance -> update { it.copy(appearance = appearance) } }
                    Text("Your timetable stays on this device. Setup can be changed whenever you need.")
                }
            }
        }
        Row(Modifier.fillMaxWidth().padding(top = 16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            if (step > 0) TextButton(onClick = { update { it.copy(onboardingStep = step - 1) } }) { Text("Back") } else Text("1 / 3")
            Button(enabled = step != 0 || (name.isNotBlank() && validSpan(start, end) != null && selectedDays.isNotEmpty()), onClick = {
                if (step == 0) update { it.copy(name = name.trim(), days = selectedDays.map { d -> DayHours(d, validSpan(start, end)!!) }, onboardingStep = 1) }
                else if (step == 1) update { it.copy(onboardingStep = 2) }
                else update { it.copy(onboarded = true) }
            }) { Text(if (step == 2) "Start using Tempo" else if (step == 1 && timetable.subjects.isEmpty()) "Add subjects later" else "Continue") }
        }
    }
    if (subjectEditor) SubjectEditor(null, 0, { subjectEditor = false }, { subject -> update { it.copy(subjects = it.subjects + subject) }; subjectEditor = false }, {})
}
