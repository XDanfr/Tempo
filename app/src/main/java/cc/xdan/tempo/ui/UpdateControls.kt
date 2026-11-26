package cc.xdan.tempo.ui

import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.SystemUpdate
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cc.xdan.tempo.design.TempoMotion
import cc.xdan.tempo.design.rememberTempoHaptics
import cc.xdan.tempo.updates.TempoUpdates
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Re-reads update preferences whenever the updater changes them. */
@Composable
private fun rememberUpdateRevision(): Int {
    val context = LocalContext.current
    val p = remember { TempoUpdates.prefs(context) }
    var revision by remember { mutableIntStateOf(0) }
    DisposableEffect(p) {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, _ -> revision++ }
        p.registerOnSharedPreferenceChangeListener(listener)
        onDispose { p.unregisterOnSharedPreferenceChangeListener(listener) }
    }
    return revision
}

@Composable
fun UpdateControls(openPrompt: () -> Unit = {}) {
    val context = LocalContext.current
    val p = remember { TempoUpdates.prefs(context) }
    val revision = rememberUpdateRevision()
    val status = remember(revision) { p.getString("status", "Tempo checks GitHub for new stable releases") ?: "" }
    val automatic = remember(revision) { TempoUpdates.autoCheck(context) }
    val available = remember(revision) { TempoUpdates.available(context) }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(status, style = MaterialTheme.typography.titleMedium)
        Row(Modifier.fillMaxWidth().hapticToggleable(automatic, role = androidx.compose.ui.semantics.Role.Switch) { TempoUpdates.schedule(context, it) },
            verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Check for updates automatically", style = MaterialTheme.typography.bodyLarge)
                Text("Daily. Tempo tells you when an update is ready and downloads it only when you tap Update.", style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Switch(automatic, null)
        }
        if (available) Button(onClick = openPrompt, modifier = Modifier.fillMaxWidth()) { Text("Update to ${p.getString("versionName", "")}") }
        else OutlinedButton(onClick = { TempoUpdates.check(context) }, modifier = Modifier.fillMaxWidth()) { Text("Check now") }
        Text("Update checks contact GitHub and never send your timetables.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

private sealed interface PromptState {
    data object Available : PromptState
    data class Downloading(val fraction: Float?) : PromptState
    data object Ready : PromptState
    data class Failed(val message: String) : PromptState
}

/**
 * The in-app updater: offers a new release once per version, downloads it with a wavy progress
 * indicator, verifies its checksum and signing key, then asks to install. [forceOpen] shows it
 * again from Settings after it was dismissed.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun UpdatePrompt(forceOpen: Boolean, closed: () -> Unit) {
    val context = LocalContext.current
    val p = remember { TempoUpdates.prefs(context) }
    val revision = rememberUpdateRevision()
    val scope = rememberCoroutineScope()
    val haptics = rememberTempoHaptics()
    // Ask for notification permission once, so later updates can be announced.
    val notifications = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    LaunchedEffect(Unit) {
        if (android.os.Build.VERSION.SDK_INT >= 33 && TempoUpdates.autoCheck(context) && !p.getBoolean("askedNotifications", false)) {
            p.edit().putBoolean("askedNotifications", true).apply()
            notifications.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        }
        TempoUpdates.ensureScheduled(context)
        if (TempoUpdates.autoCheck(context) && System.currentTimeMillis() - p.getLong("checked", 0) > 6 * 60 * 60 * 1000L) TempoUpdates.check(context)
    }
    val available = remember(revision) { TempoUpdates.available(context) }
    val code = remember(revision) { p.getLong("versionCode", 0) }
    val downloading = remember(revision) { p.getLong("downloadId", -1) != -1L }
    val ready = remember(revision) { p.getBoolean("ready", false) }
    var failure by remember { mutableStateOf<String?>(null) }
    var progress by remember { mutableStateOf<Float?>(null) }
    LaunchedEffect(downloading, ready) {
        while (downloading && !ready) {
            TempoUpdates.progress(context)?.let { now ->
                if (now.failed) { failure = "The download didn't finish. Check your connection and try again."; TempoUpdates.cancelDownload(context) }
                progress = if (now.total > 0) (now.downloaded.toFloat() / now.total).coerceIn(0f, 1f) else null
            }
            delay(250)
        }
    }
    val visible = available && (forceOpen || downloading || ready || p.getLong("dismissedCode", 0) != code)
    if (!visible) return
    val state: PromptState = when {
        failure != null -> PromptState.Failed(failure!!)
        ready -> PromptState.Ready
        downloading -> PromptState.Downloading(progress)
        else -> PromptState.Available
    }
    val version = p.getString("versionName", "") ?: ""
    fun later() { if (state is PromptState.Available || state is PromptState.Failed) TempoUpdates.dismiss(context); closed() }
    fun install() = scope.launch {
        try {
            TempoUpdates.verify(context)
            if (!context.packageManager.canRequestPackageInstalls())
                context.startActivity(Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${context.packageName}")))
            else { haptics.confirm(); TempoUpdates.install(context) }
        } catch (e: kotlinx.coroutines.CancellationException) { throw e }
        catch (e: Exception) { failure = e.message ?: "This update couldn't be verified, so it wasn't installed." }
    }
    AlertDialog(onDismissRequest = ::later, icon = { Icon(Icons.Outlined.SystemUpdate, null) },
        title = { Text(when (state) { is PromptState.Ready -> "Ready to install"; is PromptState.Failed -> "Update didn't finish"; else -> "Tempo $version is here" }) },
        text = {
            AnimatedContent(state, label = "update state", contentKey = { it::class }, transitionSpec = { fadeIn(TempoMotion.effects()) togetherWith fadeOut(TempoMotion.exit()) }) { shown ->
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    when (shown) {
                        PromptState.Available -> {
                            val notes = p.getString("notes", "")?.lines()?.map { it.trim().removePrefix("- ").removePrefix("* ") }?.filter { it.isNotBlank() && !it.startsWith("#") }?.take(5).orEmpty()
                            Column(Modifier.heightIn(max = 220.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                if (notes.isEmpty()) Text("A new version of Tempo is available.")
                                notes.forEach { Text("• ${it.replace("**", "")}", style = MaterialTheme.typography.bodyMedium) }
                            }
                        }
                        is PromptState.Downloading -> {
                            val fraction = shown.fraction
                            if (fraction != null) LinearWavyProgressIndicator(progress = { fraction }, modifier = Modifier.fillMaxWidth()
                                .semantics { contentDescription = "Downloading, ${(fraction * 100).toInt()} percent" })
                            else LinearWavyProgressIndicator(Modifier.fillMaxWidth())
                            Text(if (fraction != null) "Downloading… ${(fraction * 100).toInt()}%" else "Starting download…", style = MaterialTheme.typography.bodyMedium)
                            Text("You can keep using Tempo. It'll ask to install when the download finishes.", style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        PromptState.Ready -> Text("Tempo $version downloaded and checked. Android will ask you to confirm the install. Your timetables stay as they are.")
                        is PromptState.Failed -> Text(shown.message)
                    }
                }
            }
        },
        confirmButton = {
            when (state) {
                PromptState.Available -> Button(onClick = {
                    try { TempoUpdates.download(context) } catch (e: Exception) { failure = e.message ?: "Couldn't start the download." }
                }) { Text("Update") }
                is PromptState.Downloading -> TextButton(onClick = closed) { Text("Hide") }
                PromptState.Ready -> Button(onClick = { install() }) { Text("Install") }
                is PromptState.Failed -> Button(onClick = { failure = null; TempoUpdates.cancelDownload(context) }) { Text("Try again") }
            }
        },
        dismissButton = {
            when (state) {
                PromptState.Available, is PromptState.Failed -> TextButton(onClick = ::later) { Text("Later") }
                is PromptState.Downloading -> TextButton(onClick = { TempoUpdates.cancelDownload(context); closed() }) { Text("Cancel download") }
                PromptState.Ready -> TextButton(onClick = closed) { Text("Later") }
            }
        })
}
