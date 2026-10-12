package cc.xdan.tempo.widgets

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import cc.xdan.tempo.model.*
import cc.xdan.tempo.ui.EditorSheet
import cc.xdan.tempo.ui.PillChoiceField
import cc.xdan.tempo.ui.ThemeSelector
import cc.xdan.tempo.ui.label

@Composable
fun WidgetThemeControl(preset: ThemePreset?, mode: ThemeMode, followed: Appearance,
    change: (ThemePreset?, ThemeMode) -> Unit) {
    var choosing by rememberSaveable { mutableStateOf(false) }
    var pendingPreset by rememberSaveable { mutableStateOf(preset ?: followed.selectedPreset) }
    var pendingMode by rememberSaveable { mutableStateOf(if (preset == null) followed.mode else mode) }
    val focus = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    val open = {
        focus.clearFocus()
        keyboard?.hide()
        pendingPreset = preset ?: followed.selectedPreset
        pendingMode = if (preset == null) followed.mode else mode
        choosing = true
    }
    Column {
        PillChoiceField("Theme", preset == null, listOf(true, false),
            { if (it) "Follow timetable" else "Custom theme" },
            { follow -> if (follow) change(null, mode) else open() })
        if (preset != null) Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("${preset.label()} · ${mode.name.lowercase().replaceFirstChar { it.titlecase() }}", Modifier.weight(1f))
            TextButton(onClick = open) { Text("Change theme") }
        }
    }
    if (choosing) EditorSheet("Widget theme", { choosing = false }, confirm = {
        change(pendingPreset, pendingMode)
        choosing = false
    }, confirmLabel = "Use theme") {
        Text("Applies only to this widget.", style = MaterialTheme.typography.bodyMedium)
        ThemeSelector(Appearance(preset = pendingPreset, mode = pendingMode)) {
            pendingPreset = it.selectedPreset
            pendingMode = it.mode
        }
    }
}
