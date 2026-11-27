package cc.xdan.tempo.ui

import androidx.compose.foundation.background
import androidx.compose.animation.core.animateFloat
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import cc.xdan.tempo.design.TempoMotion
import cc.xdan.tempo.design.subjectForeground
import cc.xdan.tempo.model.*
import cc.xdan.tempo.schedule.Progress
import java.time.LocalDate

/** Your initial on a slowly turning cookie shape. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ProfileAvatar(name: String, size: androidx.compose.ui.unit.Dp, spin: Boolean = false) {
    val colour = personColour(name.ifBlank { "Tempo" })
    val turn = androidx.compose.animation.core.rememberInfiniteTransition(label = "avatar")
    val rotation by turn.animateFloat(0f, if (spin) 360f else 0f,
        androidx.compose.animation.core.infiniteRepeatable(androidx.compose.animation.core.tween(30_000, easing = androidx.compose.animation.core.LinearEasing)), label = "spin")
    Box(Modifier.size(size), contentAlignment = Alignment.Center) {
        Box(Modifier.matchParentSize().graphicsLayer { rotationZ = rotation }.clip(MaterialShapes.Cookie9Sided.toShape()).background(colour))
        if (name.isBlank()) Icon(Icons.Outlined.Person, null, Modifier.size(size * .5f), tint = subjectForeground(colour))
        else Text(name.take(1).uppercase(), style = if (size > 60.dp) MaterialTheme.typography.displaySmall else MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold, color = subjectForeground(colour))
    }
}

/** Profile: your name, progress, achievements and settings. Everything here stays on this device. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ProfileScreen(timetable: Timetable, profileName: String, setProfileName: (String) -> Unit, openSettings: () -> Unit) {
    val context = LocalContext.current
    val unlocked = remember(timetable) { Achievements.unlocked(context) }
    var editing by rememberSaveable { mutableStateOf(false) }
    var draft by rememberSaveable { mutableStateOf(profileName) }
    LazyVerticalGrid(GridCells.Adaptive(104.dp), Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            Column(Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                ProfileAvatar(profileName, 112.dp, spin = true)
                Surface(onClick = { draft = profileName; editing = true }, shape = RoundedCornerShape(16.dp), color = androidx.compose.ui.graphics.Color.Transparent) {
                    Row(Modifier.padding(horizontal = 10.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(profileName.ifBlank { "Add your name" }, style = MaterialTheme.typography.headlineMedium)
                        Icon(Icons.Outlined.Edit, "Edit name", Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Text("${unlocked.size} of ${Achievement.entries.size} achievements", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        if (timetable.progress == ProgressMode.STREAKS) item(span = { GridItemSpan(maxLineSpan) }) {
            val summary = remember(timetable) { Progress.summary(timetable, LocalDate.now()) }
            Surface(shape = RoundedCornerShape(28.dp), color = MaterialTheme.colorScheme.tertiaryContainer, contentColor = MaterialTheme.colorScheme.onTertiaryContainer) {
                Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text("Level ${summary.level}", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                        Text("${summary.xp} XP · ${summary.streak}-day streak", style = MaterialTheme.typography.labelLarge)
                    }
                    val fraction by androidx.compose.animation.core.animateFloatAsState(summary.levelXp.toFloat() / summary.levelGoal, TempoMotion.spatial(), label = "level")
                    LinearWavyProgressIndicator(progress = { fraction }, modifier = Modifier.fillMaxWidth(), waveSpeed = 0.dp,
                        color = MaterialTheme.colorScheme.onTertiaryContainer, trackColor = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = .2f))
                    Text("${summary.levelGoal - summary.levelXp} XP to level ${summary.level + 1} · best streak ${summary.bestStreak}", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        item(span = { GridItemSpan(maxLineSpan) }) { Text("Achievements", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 8.dp, start = 4.dp)) }
        items(Achievement.entries, key = { it.name }) { achievement ->
            val done = achievement in unlocked
            Column(Modifier.fillMaxWidth().alpha(if (done) 1f else .7f).padding(vertical = 6.dp), horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)) {
                AchievementBadge(achievement, done, size = 60.dp)
                Text(achievement.title, style = MaterialTheme.typography.labelLarge, textAlign = TextAlign.Center, maxLines = 2)
                Text(achievement.detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center, maxLines = 3)
            }
        }
        item(span = { GridItemSpan(maxLineSpan) }) {
            Surface(onClick = openSettings, shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surfaceContainer, modifier = Modifier.padding(top = 8.dp)) {
                Row(Modifier.fillMaxWidth().padding(18.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    Icon(Icons.Outlined.Settings, null)
                    Column(Modifier.weight(1f)) {
                        Text("Settings", style = MaterialTheme.typography.titleMedium)
                        Text("Appearance, schedule, celebrations and updates", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Icon(Icons.AutoMirrored.Outlined.ArrowForward, null)
                }
            }
        }
    }
    if (editing) AlertDialog(onDismissRequest = { editing = false }, title = { Text("Your name") },
        text = { OutlinedTextField(draft, { draft = it.take(40) }, singleLine = true, modifier = Modifier.fillMaxWidth(),
            supportingText = { Text("Shown to people you share with. Stays on this device.") }) },
        confirmButton = { Button(onClick = { setProfileName(draft); editing = false }) { Text("Save") } })
}
