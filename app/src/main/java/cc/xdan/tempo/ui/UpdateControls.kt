package cc.xdan.tempo.ui

import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import cc.xdan.tempo.updates.TempoUpdates
import kotlinx.coroutines.launch

@Composable
fun UpdateControls() {
    val context = LocalContext.current
    val p = remember { TempoUpdates.prefs(context) }
    var revision by remember { mutableIntStateOf(0) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    DisposableEffect(p) {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, _ -> revision++ }
        p.registerOnSharedPreferenceChangeListener(listener)
        onDispose { p.unregisterOnSharedPreferenceChangeListener(listener) }
    }
    val status = remember(revision) { p.getString("status", "Check GitHub for the latest stable release") ?: "" }
    val automatic = remember(revision) { p.getBoolean("automatic", false) }
    val ready = remember(revision) { p.getBoolean("ready", false) }
    val available = remember(revision) { p.getLong("versionCode", 0) > TempoUpdates.installedCode(context) }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Updates", style = MaterialTheme.typography.titleLarge)
        Text(status)
        Row {
            Text("Automatic updates", Modifier.weight(1f))
            Switch(automatic, { TempoUpdates.schedule(context, it) })
        }
        Text("Check daily and download stable GitHub releases automatically on an unmetered connection. Android asks before installing.", style = MaterialTheme.typography.bodySmall)
        OutlinedButton(onClick = { TempoUpdates.check(context) }, modifier = Modifier.fillMaxWidth()) { Text("Check for updates") }
        if (available) Button(onClick = {
            scope.launch {
                try {
                    if (ready) {
                        TempoUpdates.verify(context)
                        if (!context.packageManager.canRequestPackageInstalls()) context.startActivity(Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${context.packageName}")))
                        else TempoUpdates.install(context)
                    } else TempoUpdates.download(context)
                } catch (e: kotlinx.coroutines.CancellationException) { throw e }
                catch (e: Exception) { error = e.message ?: "Could not update Tempo" }
            }
        }, enabled = ready || p.getLong("downloadId", -1) == -1L, modifier = Modifier.fillMaxWidth()) { Text(if (ready) "Install update" else "Download update") }
        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
    }
}
