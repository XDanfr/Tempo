package cc.xdan.tempo.ui

import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cc.xdan.tempo.account.*
import cc.xdan.tempo.data.ShareCrypto
import cc.xdan.tempo.data.ShareOptions
import cc.xdan.tempo.design.subjectForeground
import cc.xdan.tempo.model.*
import cc.xdan.tempo.share.OwnedShare
import cc.xdan.tempo.share.ShareClient
import cc.xdan.tempo.share.ShareStore
import kotlinx.coroutines.launch

/** Your timetable's share link for friends, creating one (without rooms or notes) if it has none yet. */
suspend fun friendLink(context: android.content.Context, timetableId: String, timetable: Timetable, name: String): cc.xdan.tempo.data.ShareLink {
    ShareStore.owned(context, timetableId)?.let { return it.link }
    val key = ShareCrypto.newKey()
    val (id, token) = ShareClient.create(ShareCrypto.encrypt(name, timetable, ShareOptions(false, false), key))
    return OwnedShare(id, key, token, rooms = false, notes = false).also { ShareStore.save(context, timetableId, it) }.link
}

/** Friends on your profile: invite, accept requests, and choose whether friends see your timetable. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun FriendsCard(account: SignedIn, collection: TimetableCollection?, profileName: String, profileChanged: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var friends by remember { mutableStateOf<Accounts.Friends?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    var refresh by remember { mutableIntStateOf(0) }
    var removing by remember { mutableStateOf<Accounts.Person?>(null) }
    var sharedId by remember { mutableStateOf(FriendStore.sharedTimetable(context)) }
    LaunchedEffect(account, refresh) {
        runCatching { Accounts.friends(account, FriendStore.keys(context)) }
            .onSuccess { friends = it; error = null; runCatching { FriendStore.syncTimetables(context, it.friends) } }
            .onFailure { error = it.message }
    }
    fun act(action: suspend () -> Unit) {
        busy = true
        scope.launch { try { action(); refresh++ } catch (e: Exception) { error = e.message } finally { busy = false } }
    }
    Surface(shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surfaceContainer, modifier = Modifier.padding(top = 8.dp)) {
        Column(Modifier.fillMaxWidth().padding(vertical = 14.dp)) {
            Row(Modifier.padding(horizontal = 18.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("Friends", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                if (busy) LoadingIndicator(Modifier.size(28.dp))
            }
            error?.let { Text(it, Modifier.padding(horizontal = 18.dp, vertical = 4.dp), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error) }
            friends?.incoming?.forEach { request ->
                val name = request.profile?.name?.ifBlank { null } ?: "Someone"
                ListItem(headlineContent = { Text(name) }, supportingContent = { Text("Wants to be friends") },
                    leadingContent = { Avatar(name) },
                    trailingContent = { Row {
                        TextButton(onClick = { act { Accounts.removeFriend(account, request.accountId) } }) { Text("Decline") }
                        FilledTonalButton(onClick = { act {
                            request.profileKey?.let { FriendStore.saveKey(context, request.accountId, it) }
                            Accounts.accept(account, request.id)
                        } }) { Text("Accept") }
                    } },
                    colors = ListItemDefaults.colors(containerColor = androidx.compose.ui.graphics.Color.Transparent))
            }
            friends?.friends?.forEach { friend ->
                val name = friend.profile?.name?.ifBlank { null } ?: "Friend"
                ListItem(headlineContent = { Text(name) },
                    supportingContent = { Text(friend.profile?.let { p -> "Level ${p.level} · ${p.achievements} achievements" + if (p.link != null) " · shares a timetable" else "" } ?: "Profile not shared yet") },
                    leadingContent = { Avatar(name) },
                    trailingContent = { IconButton(onClick = { removing = friend }) { Icon(Icons.Outlined.PersonRemove, "Remove $name") } },
                    colors = ListItemDefaults.colors(containerColor = androidx.compose.ui.graphics.Color.Transparent))
            }
            if (friends != null && friends!!.friends.isEmpty() && friends!!.incoming.isEmpty())
                Text("Invite friends to see each other's level and shared frees, without sending links around.",
                    Modifier.padding(horizontal = 18.dp, vertical = 4.dp), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            // Your timetable for friends: the open one if it's yours, otherwise your first.
            val mine = collection?.timetables?.filter { it.timetable.person == null && it.timetable.onboarded }.orEmpty()
            val candidate = mine.firstOrNull { it.id == collection?.activeId } ?: mine.firstOrNull()
            if (candidate != null) Row(Modifier.fillMaxWidth().hapticToggleable(sharedId != null, role = Role.Switch) { on ->
                if (on) act { friendLink(context, candidate.id, candidate.timetable, profileName); FriendStore.setSharedTimetable(context, candidate.id); sharedId = candidate.id; profileChanged() }
                else { FriendStore.setSharedTimetable(context, null); sharedId = null; profileChanged() }
            }.padding(horizontal = 18.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Friends see my timetable", style = MaterialTheme.typography.bodyLarge)
                    Text((mine.firstOrNull { it.id == sharedId } ?: candidate).timetable.name + " · without rooms or notes", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(sharedId != null, null)
            }
            Button(onClick = { act {
                val link = Accounts.invite(account)
                context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).setType("text/plain")
                    .putExtra(Intent.EXTRA_TEXT, "Be my friend on Tempo: $link"), "Invite a friend"))
            } }, modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 4.dp)) {
                Icon(Icons.Outlined.PersonAdd, null); Spacer(Modifier.width(8.dp)); Text("Invite a friend")
            }
        }
    }
    removing?.let { friend ->
        val name = friend.profile?.name?.ifBlank { null } ?: "this friend"
        EditorSheet("Remove $name?", { removing = null }) {
            Text("You'll stop seeing each other's profile, and their shared timetable is removed from this phone.")
            Button(onClick = { removing = null; act {
                Accounts.removeFriend(account, friend.accountId); FriendStore.forget(context, friend.accountId)
            } }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error, contentColor = MaterialTheme.colorScheme.onError)) { Text("Remove") }
        }
    }
}

@Composable
private fun Avatar(name: String) {
    Surface(shape = CircleShape, color = personColour(name), contentColor = subjectForeground(personColour(name)), modifier = Modifier.size(36.dp)) {
        Box(contentAlignment = Alignment.Center) { Text(name.take(1).uppercase(), fontWeight = FontWeight.Bold) }
    }
}

/** Opened from an invite link: send a friend request, signing in first if needed. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun FriendInviteSheet(inviteId: String, theirKey: String, dismiss: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val flow = rememberAccountFlow("")
    var sent by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    EditorSheet("Friend invite", dismiss) {
        val account = flow.account
        when {
            sent -> {
                Text("Request sent. You'll see each other's profile once they accept.")
                Button(onClick = dismiss, modifier = Modifier.fillMaxWidth()) { Text("Done") }
            }
            account?.vaultKey == null -> {
                Text("Sign in to your Tempo account to add a friend. No password: one tap with your fingerprint, face or screen lock.")
                (flow.error ?: error)?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
                if (flow.busy) LoadingIndicator(Modifier.size(36.dp))
                else Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = flow::signInAccount, modifier = Modifier.weight(1f)) { Text("Sign in") }
                    OutlinedButton(onClick = flow::createAccount, modifier = Modifier.weight(1f)) { Text("Create account") }
                }
            }
            else -> {
                Text("Send a friend request? Once they accept, you'll see each other's level and any timetable you choose to share.")
                error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
                if (busy) LoadingIndicator(Modifier.size(36.dp))
                else Button(onClick = {
                    busy = true
                    scope.launch {
                        try {
                            val inviter = Accounts.request(account, inviteId, theirKey)
                            FriendStore.saveKey(context, inviter, theirKey); sent = true
                        } catch (e: Exception) { error = e.message } finally { busy = false }
                    }
                }, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Outlined.PersonAdd, null); Spacer(Modifier.width(8.dp)); Text("Send request") }
            }
        }
    }
}
