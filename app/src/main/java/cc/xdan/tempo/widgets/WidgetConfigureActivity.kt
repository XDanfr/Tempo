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
import cc.xdan.tempo.ui.PillChoiceField
import cc.xdan.tempo.ui.ThemeSelector

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
            var location by rememberSaveable { mutableStateOf(if (initial.followLocations) "Follow timetable" else if (initial.showLocation) "Show" else "Hide") }
            var icons by rememberSaveable { mutableStateOf(if (initial.showIcons == null) "Follow timetable" else if (initial.showIcons == true) "Show icons" else "Hide icons") }
            var lessonBackground by rememberSaveable { mutableStateOf(initial.lessonBackground) }
            var themedLessons by rememberSaveable { mutableStateOf(initial.themedLessons) }
            var showOutline by rememberSaveable { mutableStateOf(initial.showOutline) }
            var outerOutline by rememberSaveable { mutableStateOf(initial.outerOutline) }
            var doneForDay by rememberSaveable { mutableStateOf(initial.doneForDayMessage) }
            var empty by rememberSaveable { mutableStateOf(initial.emptyText) }
            val previewAppearance = collection?.let { c -> c.timetables.firstOrNull { it.id == table }?.timetable?.appearance ?: c.active.appearance } ?: Appearance()
            TempoTheme(if (theme.isBlank()) previewAppearance else Appearance(preset = ThemePreset.valueOf(theme), mode = mode)) {
                Surface(Modifier.fillMaxSize()) {
                    Column(Modifier.safeDrawingPadding().verticalScroll(rememberScrollState()).padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Text("Your Tempo widget", style = MaterialTheme.typography.headlineMedium)
                        Text("Now or next, with a live countdown.")
                        collection?.let { c ->
                            PillChoiceField("Timetable", table, listOf("") + c.timetables.map { it.id },
                                { key -> c.timetables.firstOrNull { it.id == key }?.timetable?.name ?: if (key.isBlank()) "Follow current timetable" else "Removed timetable" }, { table = it })
                        }
                        PillChoiceField("Theme", theme.isBlank(), listOf(true, false), { if (it) "Follow timetable" else "Custom theme" }, { follow -> theme = if (follow) "" else previewAppearance.selectedPreset.name })
                        if (theme.isNotBlank()) ThemeSelector(Appearance(preset = ThemePreset.valueOf(theme), mode = mode)) { appearance -> theme = appearance.selectedPreset.name; mode = appearance.mode }
                        if (pill || square) WidgetOptionCards("Background", lessonBackground, "Theme colour", "Lesson colour", WidgetPreview.THEME, WidgetPreview.LESSON, pill) { lessonBackground = it }
                        PillChoiceField("Icons", icons, listOf("Follow timetable", "Show icons", "Hide icons"), { it }, { icons = it })
                        PillChoiceField("Show locations", location, listOf("Follow timetable", "Show", "Hide"), { it }, { location = it })
                        WidgetOptionCards(if (!pill && !square) "Subject outlines" else "Crisp outlines", showOutline, "Filled", "Outlined", WidgetPreview.NO_OUTLINE, WidgetPreview.OUTLINE, pill, wide = !pill && !square, themedLessons = themedLessons) { showOutline = it }
                        if (!pill && !square) Row { Text("Outer outline", Modifier.weight(1f)); Switch(outerOutline, { outerOutline = it }) }
                        if (!pill && !square) WidgetOptionCards("Lesson colours", themedLessons, "Subject colours", "Theme shades", WidgetPreview.LESSON, WidgetPreview.THEME_LESSONS, false, wide = true) { themedLessons = it }
                        if (pill) WidgetOptionCards("After today’s lessons", doneForDay, "Next lesson", "Done for today", WidgetPreview.NEXT, WidgetPreview.DONE, true) { doneForDay = it }
                        if (square) WidgetOptionCards("After today’s lessons", doneForDay, "Done + next lesson", "Done message only", WidgetPreview.NEXT, WidgetPreview.DONE, false) { doneForDay = it }
                        OutlinedTextField(empty, { empty = it.take(120) }, label = { Text("When nothing is scheduled") }, modifier = Modifier.fillMaxWidth())
                        Text("Touch and hold the widget on your home screen to change these settings. Drag its edges to resize. The 4×2 shows later periods and your day’s finish time when there is room.", style = MaterialTheme.typography.bodySmall)
                        Text("Countdowns tick on your home screen. Android may delay changing activities in battery-saving modes.", style = MaterialTheme.typography.bodySmall)
                        Button(onClick = {
                            WidgetSettings(table.ifBlank { null }, theme.takeIf { it.isNotBlank() }?.let(ThemePreset::valueOf), mode, location != "Hide", empty.ifBlank { "Nothing scheduled this week" },
                                when (icons) { "Show icons" -> true; "Hide icons" -> false; else -> null },
                                initial.outlined, lessonBackground, showOutline, doneForDay, themedLessons, outerOutline, followLocations = location == "Follow timetable").save(applicationContext, id)
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
