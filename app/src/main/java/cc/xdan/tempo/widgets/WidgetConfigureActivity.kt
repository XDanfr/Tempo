package cc.xdan.tempo.widgets

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import cc.xdan.tempo.data.TimetableRepository
import cc.xdan.tempo.design.TempoTheme
import cc.xdan.tempo.model.*
import cc.xdan.tempo.ui.PillChoiceField
import kotlinx.coroutines.delay

/** One configuration contract for every adaptive size and all compatibility providers. */
class WidgetConfigureActivity : ComponentActivity() {
    private var saveForExit: (() -> Unit)? = null

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setResult(RESULT_CANCELED)
        val id = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
        if (id == AppWidgetManager.INVALID_APPWIDGET_ID) { finish(); return }
        enableEdgeToEdge()
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() { saveForExit?.invoke(); finish() }
        })
        setContent {
            val repository = remember { TimetableRepository(applicationContext) }
            val collection by repository.collection.collectAsState(initial = null)
            var settings by remember { mutableStateOf(WidgetSettings.read(applicationContext, id)) }
            val selected = collection?.let { c ->
                if (settings.timetableId == null) c.active else c.timetables.firstOrNull { it.id == settings.timetableId }?.timetable
            }
            val appearance = selected?.appearance ?: Appearance()
            val valid = collection != null && (settings.timetableId == null || selected != null)
            val save = {
                if (valid) {
                    settings.save(applicationContext, id)
                    WidgetRefresh.start(applicationContext)
                    setResult(RESULT_OK, Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id))
                }
            }
            SideEffect { saveForExit = save }
            // Retain immediate application and Home/back persistence from the existing configuration.
            LaunchedEffect(settings, valid) { if (valid) { delay(250); save() } }
            TempoTheme(if (settings.preset == null) appearance else Appearance(preset = settings.preset!!, mode = settings.mode)) {
                val view = LocalView.current
                val lightBars = MaterialTheme.colorScheme.background.luminance() > .5f
                SideEffect {
                    WindowCompat.getInsetsController(window, view).apply {
                        isAppearanceLightStatusBars = lightBars
                        isAppearanceLightNavigationBars = lightBars
                    }
                }
                Scaffold(
                    topBar = {
                        TopAppBar(title = { Text("Your Tempo widget") }, navigationIcon = {
                            IconButton(onClick = { save(); finish() }) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Save and close") }
                        })
                    },
                    bottomBar = {
                        Surface(tonalElevation = 3.dp) {
                            Button(onClick = { save(); finish() }, enabled = valid,
                                modifier = Modifier.navigationBarsPadding().fillMaxWidth().padding(horizontal = 24.dp, vertical = 12.dp).heightIn(min = 52.dp)) {
                                Text("Done")
                            }
                        }
                    }
                ) { padding ->
                    Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp),
                        verticalArrangement = Arrangement.spacedBy(20.dp)) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("A little more you.", style = MaterialTheme.typography.headlineLarge)
                            Text("One widget. More of your day as it grows.", style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        AdaptiveWidgetPreview(selected, settings)
                        SettingsSection("Your timetable", "Follow your selection in Tempo, or keep this widget on one timetable.") {
                            collection?.let { c ->
                                PillChoiceField("Timetable", settings.timetableId ?: "", listOf("") + c.timetables.map { it.id },
                                    { key -> c.timetables.firstOrNull { it.id == key }?.timetable?.name ?: if (key.isBlank()) "Follow current timetable" else "Removed timetable" },
                                    { settings = settings.copy(timetableId = it.ifBlank { null }) })
                            } ?: Text("Loading timetables…")
                            if (collection != null && !valid) Text("This timetable was removed. Choose another to continue.", color = MaterialTheme.colorScheme.error)
                        }
                        SettingsSection("Colour & shape", "Material colour, expressive shapes and one clear countdown.") {
                            WidgetThemeControl(settings.preset, settings.mode, appearance) { preset, mode -> settings = settings.copy(preset = preset, mode = mode) }
                            SettingToggle("Subject-colour hero", "Use the subject colour for the main event surface", settings.lessonBackground) { settings = settings.copy(lessonBackground = it) }
                            SettingToggle("Theme shades", "Use your theme in place of subject colours", settings.themedLessons) { settings = settings.copy(themedLessons = it) }
                            SettingToggle("Segment outlines", "Outline the connected event and countdown shapes", settings.showOutline) { settings = settings.copy(showOutline = it) }
                            SettingToggle("Outer outline", "Outline the full square, wide or agenda surface", settings.outerOutline) { settings = settings.copy(outerOutline = it) }
                        }
                        SettingsSection("At a glance", "Choose the details that belong on your home screen.") {
                            PillChoiceField("Subject icons", settings.showIcons, listOf(null, true, false),
                                { when (it) { null -> "Follow timetable"; true -> "Show"; false -> "Hide" } },
                                { settings = settings.copy(showIcons = it) })
                            val location = if (settings.followLocations) 0 else if (settings.showLocation) 1 else 2
                            PillChoiceField("Locations", location, listOf(0, 1, 2),
                                { when (it) { 0 -> "Follow timetable"; 1 -> "Show"; else -> "Hide" } },
                                { settings = settings.copy(followLocations = it == 0, showLocation = it != 2) })
                            SettingToggle("Pause at the end of the day", "Show Done for today instead of counting down to the next day", settings.doneForDayMessage) { settings = settings.copy(doneForDayMessage = it) }
                            OutlinedTextField(settings.emptyText, { settings = settings.copy(emptyText = it.take(120)) },
                                label = { Text("Empty week message") }, modifier = Modifier.fillMaxWidth(), minLines = 2)
                        }
                        Text("Touch and hold your widget to return here. Android may delay changing activities in battery-saving modes.",
                            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(8.dp))
                    }
                }
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        saveForExit?.invoke()
        super.onSaveInstanceState(outState)
    }

    override fun onUserLeaveHint() { saveForExit?.invoke(); finish(); super.onUserLeaveHint() }
}

@Composable
private fun SettingsSection(title: String, description: String, content: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(title, style = MaterialTheme.typography.titleLarge)
        Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Column(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceContainer, RoundedCornerShape(28.dp)).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp), content = content)
    }
}

@Composable
private fun SettingToggle(title: String, description: String, checked: Boolean, change: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().heightIn(min = 56.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f).padding(end = 12.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked, change)
    }
}
