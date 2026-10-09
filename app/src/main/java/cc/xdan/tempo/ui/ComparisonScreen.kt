package cc.xdan.tempo.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import cc.xdan.tempo.model.*
import cc.xdan.tempo.schedule.*
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ComparisonScreen(collection: TimetableCollection, dismiss: () -> Unit) {
    var selected by rememberSaveable { mutableStateOf(collection.timetables.take(2).map { it.id }) }
    var day by rememberSaveable { mutableIntStateOf(LocalDate.now().dayOfWeek.value) }
    var combined by rememberSaveable { mutableStateOf(false) }
    var choosing by rememberSaveable { mutableStateOf(false) }
    val tables = collection.timetables.filter { it.id in selected }
    val slices = remember(tables, day) { if (tables.size >= 2) compareTimetables(tables.map { it.timetable }, day) else emptyList() }
    Dialog(onDismissRequest = dismiss, properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Scaffold(modifier = Modifier.safeDrawingPadding(), topBar = {
                TopAppBar(title = { Text("Compare timetables") }, navigationIcon = { IconButton(onClick = dismiss) { Icon(Icons.Outlined.ArrowBack, "Back to timetables") } },
                    actions = { IconButton(onClick = { choosing = true }) { Icon(Icons.Outlined.GroupAdd, "Choose timetables") } })
            }) { padding ->
                Column(Modifier.padding(padding).fillMaxSize(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(Modifier.padding(horizontal = 16.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("${tables.size} timetables", style = MaterialTheme.typography.titleMedium)
                            Text(if (tables.size >= 2) "${slices.filter { it.sharedFree }.sumOf { it.time.minutes }} min free together" else "Choose at least two to compare", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                        }
                        TextButton(onClick = { choosing = true }) { Text("Choose") }
                    }
                    Row(Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        (1..7).forEach { d -> FilterChip(day == d, { day = d }, label = { Text(weekdayNames[d - 1].take(3)) }) }
                    }
                    SingleChoiceSegmentedButtonRow(Modifier.padding(horizontal = 16.dp).fillMaxWidth()) {
                        SegmentedButton(selected = !combined, onClick = { combined = false }, shape = SegmentedButtonDefaults.itemShape(0, 2), icon = { Icon(Icons.Outlined.ViewColumn, null, Modifier.size(18.dp)) }) { Text("Side by side") }
                        SegmentedButton(selected = combined, onClick = { combined = true }, shape = SegmentedButtonDefaults.itemShape(1, 2), icon = { Icon(Icons.Outlined.Groups, null, Modifier.size(18.dp)) }) { Text("Together") }
                    }
                    if (tables.size < 2) {
                        Column(Modifier.fillMaxWidth().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Icon(Icons.Outlined.PeopleOutline, null, Modifier.size(48.dp), tint = MaterialTheme.colorScheme.primary)
                            Text("Find time together", style = MaterialTheme.typography.headlineSmall)
                            Text("Select timetables for different people or parts of your week.")
                            Button(onClick = { choosing = true }) { Text("Choose timetables") }
                        }
                    } else if (combined) CombinedAgenda(tables, day, slices, Modifier.weight(1f))
                    else AlignedTimelines(tables, day, Modifier.weight(1f))
                }
            }
        }
        if (choosing) EditorSheet("Choose timetables", { choosing = false }, confirm = { choosing = false }, confirmLabel = "Compare", enabled = selected.count { id -> collection.timetables.any { it.id == id } } >= 2) {
            collection.timetables.forEach { entry ->
                Row(Modifier.fillMaxWidth().toggleable(entry.id in selected) { checked -> selected = if (checked) selected + entry.id else selected - entry.id }, verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(entry.id in selected, null)
                    Column(Modifier.padding(start = 12.dp)) {
                        Text(entry.timetable.name, style = MaterialTheme.typography.titleMedium)
                        Text("${entry.timetable.sessions.size} sessions", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}

@Composable
private fun AlignedTimelines(tables: List<SavedTimetable>, day: Int, modifier: Modifier) {
    val schedules = remember(tables, day) { tables.map { ScheduleResolver.resolve(it.timetable, day) } }
    val blocks = schedules.flatten()
    if (blocks.isEmpty()) { Box(modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { Text("No activities on ${weekdayNames[day - 1]}") }; return }
    val start = (blocks.minOf { it.time.start } / 30) * 30
    val end = ((blocks.maxOf { it.time.end } + 29) / 30) * 30
    val scale = 1.25f
    val totalHeight = ((end - start) * scale).dp
    val vertical = rememberScrollState()
    val horizontal = rememberScrollState()
    BoxWithConstraints(modifier.fillMaxWidth()) {
        val columnWidth = ((maxWidth - 60.dp) / minOf(tables.size, 2)).coerceAtLeast(148.dp)
        Row(Modifier.fillMaxSize().verticalScroll(vertical).padding(bottom = 24.dp)) {
            Column(Modifier.width(56.dp)) {
                Spacer(Modifier.height(64.dp))
                Box(Modifier.height(totalHeight)) {
                    (start until end step 30).forEach { minute -> Text(minuteLabel(minute), Modifier.offset(y = ((minute - start) * scale).dp).padding(start = 8.dp), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                }
            }
            Row(Modifier.horizontalScroll(horizontal), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                tables.forEachIndexed { index, table ->
                    Column(Modifier.width(columnWidth)) {
                        Surface(color = MaterialTheme.colorScheme.surfaceContainerHigh, shape = MaterialTheme.shapes.medium) {
                            Row(Modifier.fillMaxWidth().height(56.dp).padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(Icons.Outlined.PersonOutline, null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
                                Text(table.timetable.name, style = MaterialTheme.typography.titleSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        Box(Modifier.height(totalHeight).fillMaxWidth()) {
                            (start until end step 30).forEach { minute -> HorizontalDivider(Modifier.offset(y = ((minute - start) * scale).dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = .35f)) }
                            val occupied = schedules[index].filterNot { it is ScheduleBlock.Free }
                            // Separate overlap lanes while retaining each activity's real duration.
                            schedules[index].forEach { block ->
                                val collisions = occupied.filter { it.time.start < block.time.end && it.time.end > block.time.start }
                                val lanes = if (block is ScheduleBlock.Free) 1 else maxOf(1, collisions.size)
                                val lane = if (lanes == 1) 0 else collisions.indexOf(block).coerceAtLeast(0)
                                val laneWidth = columnWidth / lanes
                                ComparisonBlock(block, table.timetable.appearance,
                                    Modifier.offset(x = laneWidth * lane, y = ((block.time.start - start) * scale).dp).width(laneWidth - 3.dp).height((block.time.minutes * scale - 3).coerceAtLeast(8f).dp),
                                    compact = block.time.minutes < 45 || lanes > 1)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ComparisonBlock(block: ScheduleBlock, appearance: Appearance, modifier: Modifier, compact: Boolean = false) {
    val accent = block.accent(MaterialTheme.colorScheme.primary)
    val dark = MaterialTheme.colorScheme.background.luminance() < .5f
    val free = block is ScheduleBlock.Free
    val outlined = free || (if (block is ScheduleBlock.Break) appearance.effectiveBreakStyle else appearance.blockStyle) == BlockStyle.OUTLINED
    val fill = if (outlined) MaterialTheme.colorScheme.surface else lerp(MaterialTheme.colorScheme.surface, accent, if (dark) .30f else .55f)
    Surface(modifier, color = fill, shape = RoundedCornerShape(14.dp), border = BorderStroke(if (free) .6.dp else 1.dp, accent.copy(alpha = if (free) .25f else .70f))) {
        Column(Modifier.padding(if (compact) 6.dp else 10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                if (!compact && appearance.showIcons) block.symbol()?.let { Icon(it, null, Modifier.size(18.dp), tint = accent) }
                Text(block.title(), style = if (compact) MaterialTheme.typography.labelMedium else MaterialTheme.typography.titleSmall, maxLines = if (compact) 1 else 2, overflow = TextOverflow.Ellipsis)
            }
            if (!compact) {
                Text("${minuteLabel(block.time.start)}–${minuteLabel(block.time.end)}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (block is ScheduleBlock.Lesson && block.location.isNotBlank()) Text(block.location, style = MaterialTheme.typography.labelSmall, maxLines = 1)
            }
        }
    }
}

private data class ComparisonEvent(val owner: String, val appearance: Appearance, val block: ScheduleBlock)

@Composable
private fun CombinedAgenda(tables: List<SavedTimetable>, day: Int, slices: List<ComparisonSlice>, modifier: Modifier) {
    val events = remember(tables, day, slices) {
        (tables.flatMap { table -> ScheduleResolver.resolve(table.timetable, day).filterNot { it is ScheduleBlock.Free }.map { ComparisonEvent(table.timetable.name, table.timetable.appearance, it) } } +
            slices.filter { it.sharedFree }.map { ComparisonEvent("Everyone", Appearance(), ScheduleBlock.Free(it.time)) }).sortedBy { it.block.time.start }
    }
    LazyColumn(modifier.fillMaxWidth(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (events.isEmpty()) item { Text("No activities on ${weekdayNames[day - 1]}") }
        items(events) { event ->
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(if (event.owner == "Everyone") Icons.Outlined.Groups else Icons.Outlined.PersonOutline, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                    Text(if (event.owner == "Everyone") "Free together" else event.owner, style = MaterialTheme.typography.labelLarge)
                }
                ComparisonBlock(event.block, event.appearance, Modifier.fillMaxWidth().heightIn(min = 84.dp))
            }
        }
    }
}
