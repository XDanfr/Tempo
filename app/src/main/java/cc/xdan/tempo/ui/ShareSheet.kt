package cc.xdan.tempo.ui

import android.content.Intent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cc.xdan.tempo.data.ShareCrypto
import cc.xdan.tempo.data.ShareOptions
import cc.xdan.tempo.data.SharedTimetable
import cc.xdan.tempo.design.TempoMotion
import cc.xdan.tempo.model.Timetable
import cc.xdan.tempo.model.sessionWord
import cc.xdan.tempo.model.subjectWord
import cc.xdan.tempo.share.OwnedShare
import cc.xdan.tempo.share.ShareClient
import cc.xdan.tempo.share.ShareStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Create, update or stop an encrypted share link for one of your timetables. Rooms and notes are
 * left out unless chosen; the link's key never reaches the server.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ShareSheet(timetableId: String, timetable: Timetable, profileName: String, setProfileName: (String) -> Unit, dismiss: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val clipboard = LocalClipboardManager.current
    var owned by remember { mutableStateOf(ShareStore.owned(context, timetableId)) }
    var name by rememberSaveable { mutableStateOf(profileName) }
    var rooms by rememberSaveable { mutableStateOf(owned?.rooms ?: false) }
    var notes by rememberSaveable { mutableStateOf(owned?.notes ?: false) }
    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    fun run(action: suspend () -> Unit) = scope.launch {
        busy = true; message = null
        try { action() } catch (e: kotlinx.coroutines.CancellationException) { throw e } catch (e: Exception) { message = e.message ?: "Sharing didn't work. Try again." }
        finally { busy = false }
    }
    suspend fun encrypt(key: String) = withContext(Dispatchers.Default) { ShareCrypto.encrypt(name.trim(), timetable, ShareOptions(rooms, notes), key) }
    EditorSheet("Share ${timetable.name}", dismiss) {
        Text("Anyone with the link can see this timetable and compare frees with you. It's end-to-end encrypted, so Tempo's server can't read it.",
            style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        OutlinedTextField(name, { name = it.take(40) }, label = { Text("Your name") }, singleLine = true, modifier = Modifier.fillMaxWidth(),
            supportingText = { Text("Shown to people you share with, e.g. “Alex also has a free”.") }, leadingIcon = { Icon(Icons.Outlined.Person, null) })
        ShareToggle("Include rooms", "Subject and session locations", rooms) { rooms = it }
        ShareToggle("Include notes", "Private notes on sessions", notes) { notes = it }
        AnimatedContent(owned, label = "share state", contentKey = { it != null }, transitionSpec = { fadeIn(TempoMotion.effects()) togetherWith fadeOut(TempoMotion.exit()) }) { share ->
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (share == null) {
                    Button(enabled = name.isNotBlank() && !busy, modifier = Modifier.fillMaxWidth().height(52.dp), onClick = { run {
                        if (name.trim() != profileName) setProfileName(name)
                        val key = ShareCrypto.newKey()
                        val (id, token) = ShareClient.create(encrypt(key))
                        OwnedShare(id, key, token, rooms, notes).also {
                            ShareStore.save(context, timetableId, it); owned = it
                            ShareStore.setUploadedHash(context, timetableId, cc.xdan.tempo.share.ShareSync.contentHash(name.trim(), timetable, it))
                            cc.xdan.tempo.share.ShareSync.ensureScheduled(context)
                        }
                    } }) { Icon(Icons.Outlined.Link, null); Spacer(Modifier.width(8.dp)); Text("Create link") }
                } else {
                    Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.secondaryContainer) {
                        Text(share.link.url, Modifier.fillMaxWidth().padding(14.dp), style = MaterialTheme.typography.bodySmall, maxLines = 2)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        val sharePress = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
                        val copyPress = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
                        Button(onClick = {
                            context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).setType("text/plain")
                                .putExtra(Intent.EXTRA_TEXT, "${name.trim().ifBlank { "My" }}'s timetable on Tempo: ${share.link.url}"), "Share timetable link"))
                        }, modifier = squishWeight(sharePress), interactionSource = sharePress) { Icon(Icons.Outlined.Share, null); Spacer(Modifier.width(8.dp)); Text("Share") }
                        OutlinedButton(onClick = { clipboard.setText(AnnotatedString(share.link.url)); message = "Link copied" },
                            modifier = squishWeight(copyPress), interactionSource = copyPress) { Icon(Icons.Outlined.ContentCopy, null); Spacer(Modifier.width(8.dp)); Text("Copy") }
                    }
                    OutlinedButton(enabled = !busy, modifier = Modifier.fillMaxWidth(), onClick = { run {
                        ShareClient.update(share.id, share.token, encrypt(share.key))
                        share.copy(rooms = rooms, notes = notes).also {
                            ShareStore.save(context, timetableId, it); owned = it
                            ShareStore.setUploadedHash(context, timetableId, cc.xdan.tempo.share.ShareSync.contentHash(name.trim(), timetable, it))
                        }
                        message = "Shared copy updated"
                    } }) { Icon(Icons.Outlined.Sync, null); Spacer(Modifier.width(8.dp)); Text("Update now") }
                    DeleteButton("Stop sharing", { run {
                        ShareClient.delete(share.id, share.token)
                        ShareStore.forget(context, timetableId); owned = null
                        message = "Stopped sharing. People's copies are removed when they next check."
                    } })
                }
            }
        }
        if (busy) LinearWavyProgressIndicator(Modifier.fillMaxWidth())
        message?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary) }
        Text("Changes you make update the shared copy automatically, and people who added it get them too. Links expire after 180 days without an update. Someone could still keep a screenshot or export.", style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun ShareToggle(title: String, detail: String, value: Boolean, change: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().hapticToggleable(value, role = Role.Switch, onValueChange = change), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(value, null)
    }
}

/** Preview of a timetable someone shared with you. */
@Composable
fun SharePreviewDialog(shared: SharedTimetable, addAsPerson: () -> Unit, importOwn: () -> Unit, dismiss: () -> Unit) {
    val name = shared.sharer.ifBlank { "Someone" }
    AlertDialog(onDismissRequest = dismiss, icon = { Icon(Icons.Outlined.Group, null) },
        title = { Text("$name's timetable") },
        text = { Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(shared.timetable.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text("${countLabel(shared.timetable.subjects.size, shared.timetable.subjectWord)} · ${countLabel(shared.timetable.sessions.size, shared.timetable.sessionWord)}",
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("Add it to see when you're both free. You can also import it as your own independent copy.")
            Button(onClick = addAsPerson, modifier = Modifier.fillMaxWidth()) { Text("Add $name") }
            OutlinedButton(onClick = importOwn, modifier = Modifier.fillMaxWidth()) { Text("Import as my own") }
        } },
        confirmButton = { TextButton(onClick = dismiss) { Text("Not now") } })
}
