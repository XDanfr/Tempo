package cc.xdan.tempo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import cc.xdan.tempo.data.TimetableRepository
import cc.xdan.tempo.ui.TempoApp
import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle

class MainActivity : ComponentActivity() {
    private var shareIntent by mutableStateOf<String?>(null)
    private var friendInvite by mutableStateOf<Pair<String, String>?>(null)

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        shareLinkOf(intent)?.let { shareIntent = it }
        inviteOf(intent)?.let { friendInvite = it }
    }

    override fun onStop() {
        super.onStop()
        if (!isChangingConfigurations) cc.xdan.tempo.widgets.WidgetRefresh.refreshNow(applicationContext)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        cc.xdan.tempo.account.AccountStore.restore(this)
        enableEdgeToEdge()
        val repository = TimetableRepository(applicationContext)
        if (savedInstanceState == null) intent?.let(::shareLinkOf)?.let { shareIntent = it }
        if (savedInstanceState == null) intent?.let(::inviteOf)?.let { friendInvite = it }
        setContent {
            val model: TempoViewModel = viewModel(factory = object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T = TempoViewModel(repository) as T
            })
            TempoApp(model)
            friendInvite?.let { invite -> cc.xdan.tempo.ui.FriendInviteSheet(invite.first, invite.second) { friendInvite = null } }
            val current by model.state.collectAsStateWithLifecycle()
            LaunchedEffect(current.collection) { cc.xdan.tempo.widgets.WidgetRefresh.start(applicationContext) }
            // A tapped share link opens once the timetables have loaded.
            LaunchedEffect(current.collection != null, shareIntent) {
                val link = shareIntent
                if (current.collection != null && link != null) { model.openShareLink(link); shareIntent = null }
            }
            LaunchedEffect(current.collection != null) {
                val id = intent.getStringExtra("widgetTimetableId")
                current.collection?.let { collection ->
                    if (id != null && collection.activeId != id && collection.timetables.any { it.id == id }) model.select(id)
                }
            }
        }
    }
}

/** Only real Tempo share links count; widget taps carry their own internal address, which isn't one. */
private fun shareLinkOf(intent: android.content.Intent): String? = intent.data?.takeIf {
    (it.scheme == "https" && it.host == "tempo.xdan.cc" && it.path?.startsWith("/t/") == true) || (it.scheme == "tempo" && it.host == "t")
}?.toString()

/** A friend invite: tempo.xdan.cc/f/<invite>#k=<their profile key>. */
private fun inviteOf(intent: android.content.Intent): Pair<String, String>? = intent.data?.takeIf {
    it.scheme == "https" && it.host == "tempo.xdan.cc" && it.path?.startsWith("/f/") == true
}?.let { uri ->
    val id = uri.path!!.removePrefix("/f/").takeIf { it.matches(Regex("[A-Za-z0-9_-]{22}")) } ?: return null
    val key = uri.fragment?.removePrefix("k=")?.takeIf { it.length in 40..50 } ?: return null
    id to key
}
