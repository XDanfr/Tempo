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

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        intent.dataString?.let { shareIntent = it }
    }

    override fun onStop() {
        super.onStop()
        if (!isChangingConfigurations) cc.xdan.tempo.widgets.WidgetRefresh.refreshNow(applicationContext)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val repository = TimetableRepository(applicationContext)
        if (savedInstanceState == null) intent?.dataString?.let { shareIntent = it }
        setContent {
            val model: TempoViewModel = viewModel(factory = object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T = TempoViewModel(repository) as T
            })
            TempoApp(model)
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
