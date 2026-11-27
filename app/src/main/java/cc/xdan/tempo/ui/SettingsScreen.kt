package cc.xdan.tempo.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import cc.xdan.tempo.model.*

private enum class SettingsPage(val label: String) {
    APPEARANCE("Appearance"), CELEBRATIONS("Progress"), UPDATES("Updates"),
    SCHEDULE("Dates and weeks"), HOURS("Day hours"), PERIODS("Periods")
}

private fun ProgressMode.label() = when (this) { ProgressMode.OFF -> "Off"; ProgressMode.CELEBRATIONS -> "Celebrations"; ProgressMode.STREAKS -> "Streaks and XP" }

/**
 * Settings as a list of pages: app-wide settings first, then the open timetable's own. Timetables
 * that follow someone else's link have no timetable section, since they can't be edited.
 */
@Composable
fun SettingsScreen(timetable: Timetable, update: ((Timetable) -> Timetable) -> Unit, profileName: String = "", setProfileName: (String) -> Unit = {}, openUpdate: () -> Unit = {},
    progress: ProgressMode = ProgressMode.CELEBRATIONS, setProgress: (ProgressMode) -> Unit = {}, achievements: Boolean = true, setAchievements: (Boolean) -> Unit = {},
    appearance: Appearance = timetable.appearance, setAppearance: (Appearance) -> Unit = { a -> update { it.copy(appearance = a) } }, editable: Boolean = true) {
    var editingDay by remember { mutableStateOf<DayHours?>(null) }
    var page by rememberSaveable { mutableStateOf<SettingsPage?>(null) }
    PredictiveSettingsHost(page?.name, { page = null }) { shownPage ->
    val currentPage = shownPage?.let(SettingsPage::valueOf)
    key(currentPage) {
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(vertical = 12.dp)) {
        if (currentPage == null) {
            item {
                SettingsHeader("App")
                SettingsRow(Icons.Outlined.Palette, "Appearance", appearance.selectedPreset.label()) { page = SettingsPage.APPEARANCE }
                SettingsRow(Icons.Outlined.Celebration, "Progress", progress.label()) { page = SettingsPage.CELEBRATIONS }
                SettingsRow(Icons.Outlined.SystemUpdate, "Updates") { page = SettingsPage.UPDATES }
            }
            if (editable) item {
                HorizontalDivider(Modifier.padding(vertical = 8.dp))
                SettingsHeader(timetable.name)
                ListItem(headlineContent = { Text("Timetable for") }, leadingContent = { Icon(Icons.Outlined.Category, null) },
                    colors = ListItemDefaults.colors(containerColor = androidx.compose.ui.graphics.Color.Transparent), modifier = Modifier.padding(horizontal = 4.dp))
                FlowChoice(timetable.purpose) { purpose -> update { it.copy(purpose = purpose) } }
                SettingsRow(Icons.Outlined.CalendarMonth, "Dates and weeks", if (timetable.cycle.weeks > 1) "${timetable.cycle.weeks}-week cycle" else null) { page = SettingsPage.SCHEDULE }
                SettingsRow(Icons.Outlined.Schedule, "Day hours", timetable.days.sortedBy { it.day }.let { days ->
                    if (days.isEmpty()) "Off" else days.joinToString(" ") { weekdayNames[it.day - 1].take(3) } }) { page = SettingsPage.HOURS }
                SettingsRow(Icons.Outlined.ViewAgenda, "Periods", if (timetable.periods.isEmpty()) "Off" else countLabel(timetable.periods.size, "period")) { page = SettingsPage.PERIODS }
            }
            item { HorizontalDivider(Modifier.padding(vertical = 8.dp)); Box(Modifier.padding(horizontal = 20.dp)) { AboutTempo() } }
        } else item {
            Row(Modifier.padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                FilledTonalIconButton(onClick = { page = null }, shape = CircleShape) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Back") }
                Text(currentPage.label, style = MaterialTheme.typography.headlineSmall)
            }
        }
        val padded = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
        when (currentPage) {
            SettingsPage.APPEARANCE -> item { Box(padded) { AppearanceControls(appearance, setAppearance) } }
            SettingsPage.CELEBRATIONS -> item {
                ProgressMode.entries.forEach { mode ->
                    SettingsRow(when (mode) { ProgressMode.OFF -> Icons.Outlined.DoNotDisturbOn; ProgressMode.CELEBRATIONS -> Icons.Outlined.Celebration; ProgressMode.STREAKS -> Icons.Outlined.LocalFireDepartment },
                        mode.label(), chevron = false, trailing = { RadioButton(progress == mode, null) }) { setProgress(mode) }
                }
                HorizontalDivider(Modifier.padding(vertical = 8.dp))
                SettingsRow(Icons.Outlined.EmojiEvents, "Achievement notices", chevron = false, enabled = progress != ProgressMode.OFF,
                    trailing = { Switch(achievements && progress != ProgressMode.OFF, null, enabled = progress != ProgressMode.OFF) }) { setAchievements(!achievements) }
            }
            SettingsPage.UPDATES -> item { Box(padded) { UpdateControls(openUpdate) } }
            SettingsPage.SCHEDULE -> item { DatedScheduleSettings(timetable, update) }
            SettingsPage.HOURS -> item {
                (1..7).forEach { day ->
                    val hours = timetable.days.firstOrNull { it.day == day }
                    SettingsRow(null, weekdayNames[day - 1], hours?.let { "${minuteLabel(it.time.start)}–${minuteLabel(it.time.end)}" } ?: "Off", chevron = false,
                        trailing = { Row(Modifier.height(32.dp), verticalAlignment = Alignment.CenterVertically) {
                            // The divider separates the tappable hours from the on/off switch.
                            VerticalDivider(Modifier.padding(end = 16.dp))
                            Switch(hours != null, { enabled ->
                            update { t -> t.copy(days = t.days.filterNot { it.day == day } + if (enabled) listOf(DayHours(day, TimeSpan(540, 990))) else emptyList()) }
                        }) } }) { hours?.let { editingDay = it } }
                }
            }
            SettingsPage.PERIODS -> item { Box(padded) { PeriodSetup(timetable, update) } }
            null -> Unit
        }
    }
    }
    }
    editingDay?.let { hours ->
        HoursEditor(hours, { editingDay = null }) { new -> update { t -> t.copy(days = t.days.filterNot { it.day == new.day } + new) }; editingDay = null }
    }
}

/** Small primary-coloured label above a group of rows. */
@Composable
fun SettingsHeader(text: String) {
    Text(text, Modifier.padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 4.dp), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
}

/** One tappable settings row: icon, title, current value, and a chevron or a control. */
@Composable
fun SettingsRow(icon: ImageVector?, title: String, value: String? = null, enabled: Boolean = true, chevron: Boolean = true,
    trailing: (@Composable () -> Unit)? = null, onClick: () -> Unit) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = value?.let { { Text(it) } },
        leadingContent = icon?.let { { Icon(it, null) } },
        trailingContent = trailing ?: if (chevron) ({ Icon(Icons.Outlined.ChevronRight, null) }) else null,
        colors = ListItemDefaults.colors(containerColor = androidx.compose.ui.graphics.Color.Transparent),
        modifier = Modifier.alpha(if (enabled) 1f else .5f).hapticClickable(enabled, Role.Button, onClick).padding(horizontal = 4.dp))
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FlowChoice(selected: TimetablePurpose?, choose: (TimetablePurpose) -> Unit) {
    FlowRow(Modifier.padding(start = 60.dp, end = 20.dp, bottom = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        TimetablePurpose.entries.forEach { purpose ->
            FilterChip(selected == purpose, { choose(purpose) }, label = { Text(purpose.label) })
        }
    }
}

@Composable
private fun HoursEditor(hours: DayHours, dismiss: () -> Unit, save: (DayHours) -> Unit) {
    var start by rememberSaveable { mutableStateOf(minuteLabel(hours.time.start)) }
    var end by rememberSaveable { mutableStateOf(minuteLabel(hours.time.end)) }
    val time = validSpan(start, end)
    EditorSheet("${weekdayNames[hours.day - 1]} hours", dismiss, confirm = { time?.let { save(hours.copy(time = it)) } }, enabled = time != null, compact = true) {
        TimeFields(start, end, { start = it }, { end = it })
    }
}
