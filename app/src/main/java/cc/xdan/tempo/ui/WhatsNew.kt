package cc.xdan.tempo.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import cc.xdan.tempo.model.TimetableCollection

/** Version code whose changes the sheet describes; bump it when a release moves things around. */
private const val WHATS_NEW_VERSION = 11

/**
 * Shows "What's new" once to people updating from an older version, pointing to where familiar
 * controls moved. New installs never see it.
 */
@Composable
fun WhatsNewOnUpdate(collection: TimetableCollection?) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("tempo_ui", android.content.Context.MODE_PRIVATE) }
    var show by remember { mutableStateOf(false) }
    LaunchedEffect(collection != null) {
        if (collection == null) return@LaunchedEffect
        val seen = prefs.getInt("whatsNewSeen", 0)
        if (seen >= WHATS_NEW_VERSION) return@LaunchedEffect
        if (collection.timetables.any { it.timetable.onboarded }) show = true
        else prefs.edit().putInt("whatsNewSeen", WHATS_NEW_VERSION).apply()
    }
    if (show) EditorSheet("What's new in Tempo 0.6", { prefs.edit().putInt("whatsNewSeen", WHATS_NEW_VERSION).apply(); show = false }) {
        WhatsNewItems()
        Button(onClick = { prefs.edit().putInt("whatsNewSeen", WHATS_NEW_VERSION).apply(); show = false }, modifier = Modifier.fillMaxWidth().height(52.dp)) { Text("Got it") }
    }
}

/** The list of changes, shared by the sheet and screenshots. */
@Composable
fun WhatsNewItems() {
        Item(Icons.Outlined.CalendarToday, "One Schedule", "Today and Timetable are now one tab. Swipe between days, tap the date to jump, or use the week strip.")
        Item(Icons.Outlined.ViewWeek, "New Week tab", "Your week at a glance, with totals, subjects and frees you share with others.")
        Item(Icons.Outlined.ExpandMore, "Timetables, import and export", "Tap your timetable's name at the top. The calendar button and the Timetables & files settings page have gone.")
        Item(Icons.Outlined.Undo, "Undo after every change", "A snackbar offers Undo, then Redo. Both are also in the ⋮ menu, along with Share as image.")
        Item(Icons.Outlined.Delete, "Deleting is instant", "Sessions, breaks and subjects delete straight away. Undo brings them back.")
        Item(Icons.Outlined.Celebration, "Celebrations and streaks", "Confetti when your day is done. Turn on check-ins, XP and streaks in Settings › Appearance › Celebrations.")
        Item(Icons.Outlined.Group, "Frees with friends", "Add your name in Settings, then mark a friend's imported timetable as theirs to see when you're both free.")
}

@Composable
private fun Item(icon: ImageVector, title: String, detail: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.Top) {
        Surface(shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.secondaryContainer, modifier = Modifier.size(44.dp)) {
            Box(contentAlignment = Alignment.Center) { Icon(icon, null) }
        }
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(detail, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
