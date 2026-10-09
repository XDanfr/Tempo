package cc.xdan.tempo.widgets

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cc.xdan.tempo.data.TimetableRepository
import cc.xdan.tempo.design.TempoTheme
import cc.xdan.tempo.model.*
import cc.xdan.tempo.ui.ChoiceField

class WidgetConfigureActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setResult(RESULT_CANCELED)
        val id = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
        if (id == AppWidgetManager.INVALID_APPWIDGET_ID) { finish(); return }
        val provider = AppWidgetManager.getInstance(this).getAppWidgetInfo(id)?.provider?.className
        val pill = provider == PillWidget::class.java.name
        val square = provider == SquareWidget::class.java.name
        enableEdgeToEdge()
        setContent {
            val repository = remember { TimetableRepository(applicationContext) }
            val collection by repository.collection.collectAsState(initial = null)
            val initial = remember { WidgetSettings.read(applicationContext, id) }
            var table by rememberSaveable { mutableStateOf(initial.timetableId ?: "") }
            var theme by rememberSaveable { mutableStateOf(initial.preset?.name ?: "") }
            var mode by rememberSaveable { mutableStateOf(initial.mode) }
            var location by rememberSaveable { mutableStateOf(initial.showLocation) }
            var icons by rememberSaveable { mutableStateOf(if (initial.showIcons == null) "Follow timetable" else if (initial.showIcons == true) "Show icons" else "Hide icons") }
            var lessonBackground by rememberSaveable { mutableStateOf(initial.lessonBackground) }
            var showOutline by rememberSaveable { mutableStateOf(initial.showOutline) }
            var doneForDay by rememberSaveable { mutableStateOf(initial.doneForDayMessage) }
            var empty by rememberSaveable { mutableStateOf(initial.emptyText) }
            val previewAppearance = collection?.let { c -> c.timetables.firstOrNull { it.id == table }?.timetable?.appearance ?: c.active.appearance } ?: Appearance()
            TempoTheme(if (theme.isBlank()) previewAppearance else Appearance(preset = ThemePreset.valueOf(theme), mode = mode)) {
                Surface(Modifier.fillMaxSize()) {
                    Column(Modifier.safeDrawingPadding().verticalScroll(rememberScrollState()).padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Text("Your Tempo widget", style = MaterialTheme.typography.headlineMedium)
                        Text("Now or next, with a live countdown.")
                        collection?.let { c ->
                            ChoiceField("Timetable", table, listOf("") + c.timetables.map { it.id },
                                { key -> c.timetables.firstOrNull { it.id == key }?.timetable?.name ?: if (key.isBlank()) "Follow current timetable" else "Removed timetable" }, { table = it })
                        }
                        ChoiceField("Theme", theme, listOf("") + ThemePreset.entries.map { it.name }, { if (it.isBlank()) "Follow timetable theme" else it.lowercase().replace('_', ' ').replaceFirstChar { c -> c.titlecase() } }, { theme = it })
                        if (theme.isNotBlank()) ChoiceField("Appearance", mode, ThemeMode.entries, { it.name.lowercase().replaceFirstChar { c -> c.titlecase() } }, { mode = it })
                        if (pill || square) WidgetOptionCards("Background", lessonBackground, "Theme colour", "Lesson colour", WidgetPreview.THEME, WidgetPreview.LESSON, pill) { lessonBackground = it }
                        ChoiceField("Icons", icons, listOf("Follow timetable", "Show icons", "Hide icons"), { it }, { icons = it })
                        if (pill || square) WidgetOptionCards("Crisp outlines", showOutline, "Filled", "Outlined", WidgetPreview.NO_OUTLINE, WidgetPreview.OUTLINE, pill) { showOutline = it }
                        else Text("Lesson panels use your subject colours, with larger text for a quick glance.", style = MaterialTheme.typography.bodyMedium)
                        if (square) WidgetOptionCards("After today’s lessons", doneForDay, "Done + next lesson", "Done message only", WidgetPreview.NEXT, WidgetPreview.DONE, false) { doneForDay = it }
                        Row { Text("Show locations", Modifier.weight(1f)); Switch(location, { location = it }) }
                        OutlinedTextField(empty, { empty = it.take(120) }, label = { Text("When nothing is scheduled") }, modifier = Modifier.fillMaxWidth())
                        Text("Touch and hold the widget on your home screen to change these settings. The 4×2 also shows later periods and your day’s finish time.", style = MaterialTheme.typography.bodySmall)
                        Text("Countdowns tick on your home screen. Android may delay changing activities in battery-saving modes.", style = MaterialTheme.typography.bodySmall)
                        Button(onClick = {
                            WidgetSettings(table.ifBlank { null }, theme.takeIf { it.isNotBlank() }?.let(ThemePreset::valueOf), mode, location, empty.ifBlank { "Nothing scheduled this week" },
                                when (icons) { "Show icons" -> true; "Hide icons" -> false; else -> null },
                                null, lessonBackground, showOutline, doneForDay).save(applicationContext, id)
                            WidgetRefresh.start(applicationContext)
                            setResult(RESULT_OK, Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id)); finish()
                        }, enabled = collection != null && (table.isBlank() || collection!!.timetables.any { it.id == table }), modifier = Modifier.fillMaxWidth()) { Text("Save widget") }
                        TextButton(onClick = { finish() }) { Text("Cancel") }
                    }
                }
            }
        }
    }
}
