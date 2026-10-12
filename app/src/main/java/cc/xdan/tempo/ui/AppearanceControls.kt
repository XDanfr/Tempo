package cc.xdan.tempo.ui

import android.os.Build
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import cc.xdan.tempo.model.*
import cc.xdan.tempo.design.presetColorScheme
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.selected

fun ThemePreset.label(): String = when (this) {
    ThemePreset.FOREST -> "Forest"; ThemePreset.OCEAN -> "Ocean"; ThemePreset.AMBER -> "Amber"
    ThemePreset.MATERIAL_YOU -> "Material You"; ThemePreset.RED -> "Red"
    ThemePreset.VIOLET -> "Violet"; ThemePreset.ROSE -> "Rose"
}

@Composable
fun AppearanceControls(appearance: Appearance, change: (Appearance) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Appearance", style = MaterialTheme.typography.titleLarge)
        ThemeSelector(appearance, change)
        PillChoiceField("Subject blocks", appearance.blockStyle, BlockStyle.entries,
            { if (it == BlockStyle.FILLED) "Filled" else "Outlines" },
            { change(appearance.copy(blockStyle = it, breakStyle = appearance.effectiveBreakStyle)) })
        PillChoiceField("Break blocks", appearance.effectiveBreakStyle, BlockStyle.entries,
            { if (it == BlockStyle.FILLED) "Filled" else "Outlines" },
            { change(appearance.copy(breakStyle = it)) })
        Row(verticalAlignment = Alignment.CenterVertically) { Text("Show icons", Modifier.weight(1f)); Switch(appearance.showIcons, { change(appearance.copy(showIcons = it)) }) }
        Row(verticalAlignment = Alignment.CenterVertically) { Text("Show locations", Modifier.weight(1f)); Switch(appearance.showLocations, { change(appearance.copy(showLocations = it)) }) }
    }
}

@Composable
fun ThemeSelector(appearance: Appearance, change: (Appearance) -> Unit) {
    val dark = when (appearance.mode) { ThemeMode.DARK -> true; ThemeMode.LIGHT -> false; ThemeMode.SYSTEM -> isSystemInDarkTheme() }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            items(ThemePreset.entries, key = { it.name }) { preset ->
                val active = appearance.selectedPreset == preset
                val enabled = preset != ThemePreset.MATERIAL_YOU || Build.VERSION.SDK_INT >= 31
                val preview = if (preset == ThemePreset.MATERIAL_YOU && Build.VERSION.SDK_INT >= 31) {
                    val context = androidx.compose.ui.platform.LocalContext.current
                    if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
                } else presetColorScheme(preset, dark)
                val swatches = listOf(preview.primary, preview.tertiary, preview.surfaceContainerHigh)
                Surface(onClick = { change(appearance.copy(preset = preset, dynamicColour = false)) }, enabled = enabled,
                    modifier = Modifier.semantics { selected = active },
                    color = if (active) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
                    shape = MaterialTheme.shapes.large, border = if (active) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null) {
                    Column(Modifier.width(128.dp).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) { swatches.forEach { colour -> Box(Modifier.size(26.dp).background(colour, CircleShape)) } }
                        Text(preset.label(), style = MaterialTheme.typography.titleSmall)
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(if (active) Icons.Outlined.CheckCircleOutline else Icons.Outlined.Palette, null, Modifier.size(16.dp))
                            Text(if (!enabled) "Android 12+" else if (active) "Selected" else if (preset == ThemePreset.MATERIAL_YOU) "Wallpaper" else "Preset", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ThemeMode.entries.forEach { mode -> FilterChip(appearance.mode == mode, { change(appearance.copy(mode = mode)) }, label = { Text(mode.name.lowercase().replaceFirstChar { it.titlecase() }) }) }
        }
    }
}
