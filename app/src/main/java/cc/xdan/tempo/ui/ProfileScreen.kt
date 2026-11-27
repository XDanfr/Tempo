package cc.xdan.tempo.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import kotlinx.coroutines.launch

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
fun ProfileScreen(timetable: Timetable, profileName: String, setProfileName: (String) -> Unit, collection: TimetableCollection? = null, openSettings: () -> Unit) {
    val context = LocalContext.current
    val unlocked = remember(timetable) { Achievements.unlocked(context) }
    var opened by remember { mutableStateOf<Achievement?>(null) }
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
        item(span = { GridItemSpan(maxLineSpan) }) { AccountCard(profileName, timetable, unlocked.size, collection) }
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
        item(span = { GridItemSpan(maxLineSpan) }) { Text("Achievements", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 8.dp, start = 4.dp)) }
        items(Achievement.entries, key = { it.name }) { achievement ->
            val done = achievement in unlocked
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).clickable { opened = achievement }.alpha(if (done) 1f else .7f).padding(vertical = 6.dp), horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)) {
                AchievementBadge(achievement, done, size = 60.dp)
                Text(achievement.title, style = MaterialTheme.typography.labelLarge, textAlign = TextAlign.Center, maxLines = 2)
                Text(achievement.detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center, maxLines = 3)
            }
        }
    }
    opened?.let { achievement ->
        val done = achievement in unlocked
        val progress = remember(timetable, achievement) { achievementProgress(achievement, timetable) }
        EditorSheet(achievement.title, { opened = null }) {
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { AchievementBadge(achievement, done, size = 88.dp) }
                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(achievement.detail, textAlign = TextAlign.Center)
                    if (done) Text("Unlocked ${java.time.Instant.ofEpochMilli(unlocked.getValue(achievement)).atZone(java.time.ZoneId.systemDefault()).toLocalDate().let { friendlyDate(it) }} · +${Achievements.XP} XP",
                        style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, textAlign = TextAlign.Center)
                    else {
                        progress?.let { (have, need) ->
                            LinearWavyProgressIndicator(progress = { have.coerceAtMost(need).toFloat() / need }, modifier = Modifier.fillMaxWidth(), waveSpeed = 0.dp)
                            Text("${have.coerceAtMost(need)} of $need", style = MaterialTheme.typography.labelLarge)
                        }
                        Text("Worth ${Achievements.XP} XP", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
        }
    }
    if (editing) AlertDialog(onDismissRequest = { editing = false }, title = { Text("Your name") },
        text = { OutlinedTextField(draft, { draft = it.take(40) }, singleLine = true, modifier = Modifier.fillMaxWidth(),
            supportingText = { Text("Shown to people you share with. Stays on this device.") }) },
        confirmButton = { Button(onClick = { setProfileName(draft); editing = false }) { Text("Save") } })
}

/** How far along a countable achievement is, as (have, need); null for one-off actions. */
fun achievementProgress(achievement: Achievement, timetable: Timetable): Pair<Int, Int>? = when (achievement) {
    Achievement.FULL_LIBRARY -> timetable.subjects.size to 5
    Achievement.ON_A_ROLL -> Progress.summary(timetable, LocalDate.now()).bestStreak to 5
    Achievement.LEVEL_THREE -> Progress.summary(timetable, LocalDate.now()).level to 3
    else -> null
}

/**
 * Optional Tempo account with a passkey. Signed out: create or sign in. Signed in: timetables are saved to the
 * cloud (encrypted on this phone first) and the profile is kept in sync, with sign out and delete account.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun AccountCard(profileName: String, timetable: Timetable, achievements: Int, collection: TimetableCollection?) {
    val context = LocalContext.current
    val flow = rememberAccountFlow(profileName)
    val account = flow.account
    var confirmDelete by remember { mutableStateOf(false) }
    val level = remember(timetable) { if (timetable.progress == ProgressMode.STREAKS) Progress.summary(timetable, LocalDate.now()).level else 1 }
    var profileVersion by remember { mutableIntStateOf(0) }
    // Keep the encrypted profile current while signed in (name, level, achievements and the timetable friends see).
    LaunchedEffect(account, profileName, level, achievements, profileVersion) {
        val signedIn = account ?: return@LaunchedEffect
        kotlinx.coroutines.delay(1_000)
        val link = cc.xdan.tempo.account.FriendStore.sharedTimetable(context)?.let { cc.xdan.tempo.share.ShareStore.owned(context, it)?.link }
        runCatching { cc.xdan.tempo.account.Accounts.saveProfile(signedIn, cc.xdan.tempo.account.Profile(profileName, level, achievements, link)) }
            .onFailure { if (it.message == "Signed out") { cc.xdan.tempo.account.AccountStore.clear(context); flow.account = null } }
    }
    val synced = remember(account, timetable) { cc.xdan.tempo.account.CloudSync.lastSynced(context) }
    Column {
    Surface(shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surfaceContainer, modifier = Modifier.padding(top = 8.dp)) {
        Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                Icon(if (account?.vaultKey != null) Icons.Outlined.CloudDone else if (account != null) Icons.Outlined.CloudOff else Icons.Outlined.Key, null)
                Column(Modifier.weight(1f)) {
                    Text(when { account == null -> "Tempo account"; account.vaultKey == null -> "Cloud saving is off"; else -> "Saved to your account" },
                        style = MaterialTheme.typography.titleMedium)
                    Text(when {
                        account == null -> "Optional. Sign in with a passkey to save your timetables and use them on any phone."
                        account.vaultKey == null -> "Sign in again on this phone to unlock your saved timetables."
                        synced > 0 -> "Encrypted on this phone before saving · last saved ${java.text.DateFormat.getTimeInstance(java.text.DateFormat.SHORT).format(java.util.Date(synced))}"
                        else -> "Encrypted on this phone before saving."
                    }, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            flow.error?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error) }
            if (flow.busy) LoadingIndicator(Modifier.size(36.dp).align(Alignment.CenterHorizontally))
            else if (account == null) Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = flow::createAccount, modifier = Modifier.weight(1f)) { Text("Create account") }
                OutlinedButton(onClick = flow::signInAccount, modifier = Modifier.weight(1f)) { Text("Sign in") }
            } else {
                if (account.vaultKey == null) Button(onClick = flow::signInAccount, modifier = Modifier.fillMaxWidth()) { Text("Unlock cloud saving") }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { flow.run {
                        cc.xdan.tempo.account.Accounts.signOut(account); cc.xdan.tempo.account.AccountStore.clear(context); flow.account = null
                    } }, modifier = Modifier.weight(1f)) { Text("Sign out") }
                    TextButton(onClick = { confirmDelete = true }, modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)) { Text("Delete account") }
                }
            }
        }
    }
    if (account?.vaultKey != null) FriendsCard(account, collection, profileName) { profileVersion++ }
    }
    if (confirmDelete) EditorSheet("Delete your account?", { confirmDelete = false }) {
        Text("This removes your profile, your saved timetables in the cloud and every link your account owns, straight away. Friends' copies of your timetables disappear the next time they open Tempo. " +
            "Your timetables stay on this phone. Remove the passkey from your password manager afterwards if you like.")
        Button(onClick = { val signedIn = account!!; confirmDelete = false; flow.run {
            // Forget the links the server is about to remove, so nothing tries to update them afterwards.
            val owned = runCatching { cc.xdan.tempo.account.Accounts.ownedShares(signedIn) }.getOrDefault(emptySet())
            cc.xdan.tempo.account.Accounts.delete(signedIn)
            cc.xdan.tempo.share.ShareStore.allOwned(context).filterValues { it.id in owned }.keys.forEach { cc.xdan.tempo.share.ShareStore.forget(context, it) }
            cc.xdan.tempo.account.AccountStore.clear(context); flow.account = null
        } }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error, contentColor = MaterialTheme.colorScheme.onError)) {
            Text("Delete account")
        }
    }
}
