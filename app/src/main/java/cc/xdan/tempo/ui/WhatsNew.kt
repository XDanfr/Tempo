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
private const val WHATS_NEW_VERSION = 12

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
    if (show) EditorSheet("What's new in Tempo 0.7", { prefs.edit().putInt("whatsNewSeen", WHATS_NEW_VERSION).apply(); show = false }) {
        WhatsNewItems()
        Button(onClick = { prefs.edit().putInt("whatsNewSeen", WHATS_NEW_VERSION).apply(); show = false }, modifier = Modifier.fillMaxWidth().height(52.dp)) { Text("Got it") }
    }
}

/** The list of changes, shared by the sheet and screenshots. */
@Composable
fun WhatsNewItems() {
        Item(Icons.Outlined.Home, "Home", "Tempo opens on Home: what's next, who's free with you, your progress and achievements.")
        Item(Icons.Outlined.EmojiEvents, "Achievements", "16 to find, each worth XP. Tap your profile to see them all.")
        Item(Icons.Outlined.LocalFireDepartment, "Streak bonus", "Each streak day adds 10% XP, up to double. Ill or away? Freeze the day.")
        Item(Icons.Outlined.Link, "Share links", "Share a timetable from its ⋮ menu. It's encrypted and stays up to date for friends.")
        Item(Icons.Outlined.Settings, "New Settings", "Simpler pages. Your week lets you fill in each period by tapping it.")
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
