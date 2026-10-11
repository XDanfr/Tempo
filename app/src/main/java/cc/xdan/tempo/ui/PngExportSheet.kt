package cc.xdan.tempo.ui

import android.graphics.Bitmap
import android.content.ActivityNotFoundException
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.semantics.Role
import cc.xdan.tempo.model.Timetable
import cc.xdan.tempo.schedule.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate

private data class RenderedImage(val plan: ScheduleImagePlan, val result: Result<Bitmap>)

@Composable
fun PngExportSheet(table: Timetable, dismiss: () -> Unit, report: (String) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var dateText by rememberSaveable { mutableStateOf(LocalDate.now().toString()) }
    var week by rememberSaveable { mutableStateOf(false) }
    var widthText by rememberSaveable { mutableStateOf("1080") }
    var title by rememberSaveable { mutableStateOf(true) }
    var subjects by rememberSaveable { mutableStateOf(true) }
    var locations by rememberSaveable { mutableStateOf(false) }
    var notes by rememberSaveable { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var pending by remember { mutableStateOf<Bitmap?>(null) }
    val date = validDate(dateText)
    val width = widthText.toIntOrNull()
    val plan = remember(table, date, week, width, title, subjects, locations, notes) {
        if (date != null && width != null && width in 480..2048) scheduleImagePlan(table, date, week, width, ImagePrivacy(title, subjects, locations, notes)) else null
    }
    val rendered by produceState<RenderedImage?>(null, plan) {
        value = null
        if (plan != null) {
            try { value = withContext(Dispatchers.Default) { RenderedImage(plan, runCatching { renderSchedulePng(context, plan) }) } }
            catch (e: CancellationException) { throw e }
        }
    }
    val rendering = rendered?.takeIf { it.plan == plan }?.result
    val bitmap = rendering?.getOrNull()
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("image/png")) { uri ->
        val snapshot = pending
        pending = null
        if (uri != null) {
            if (snapshot == null) report("PNG export was interrupted. Open the preview and export again.")
            else scope.launch {
                busy = true
                try {
                    withContext(Dispatchers.IO) {
                        context.contentResolver.openOutputStream(uri, "wt")?.use { output ->
                            check(snapshot.compress(Bitmap.CompressFormat.PNG, 100, output)) { "Could not encode the PNG" }
                        } ?: error("Could not write to the chosen location")
                    }
                    report("Saved PNG (${snapshot.width} × ${snapshot.height})")
                } catch (e: CancellationException) { throw e }
                catch (e: Exception) { report("Could not save PNG: ${e.message ?: "Try another location."}") }
                finally { busy = false }
            }
        }
    }
    EditorSheet("Export timetable image", dismiss) {
        ChoiceField("View", week, listOf(false, true), { if (it) "Week (Monday–Sunday)" else "Day" }, { week = it })
        DateField("Date", dateText) { dateText = it }
        OutlinedTextField(widthText, { widthText = it }, label = { Text("Image width (px)") }, supportingText = { Text("480–2048 pixels") }, singleLine = true,
            isError = width == null || width !in 480..2048, modifier = Modifier.fillMaxWidth())
        Text("Include in image", style = MaterialTheme.typography.titleMedium)
        PrivacyToggle("Timetable name", title) { title = it }
        PrivacyToggle("Subject and break names", subjects) { subjects = it }
        PrivacyToggle("Locations", locations) { locations = it }
        PrivacyToggle("Notes", notes) { notes = it }
        Text("Locations and notes start hidden. Review the preview before saving. Colours and dates remain visible.", style = MaterialTheme.typography.bodySmall)
        if (plan != null) {
            Text(bitmap?.let { "Preview · ${it.width} × ${it.height} px" } ?: "Preview", style = MaterialTheme.typography.labelLarge)
            Box(Modifier.fillMaxWidth().height(300.dp), contentAlignment = Alignment.Center) {
                if (bitmap != null) Image(bitmap.asImageBitmap(), "Exact PNG export preview", Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
                else if (rendering == null) Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    CircularProgressIndicator(Modifier.size(24.dp)); Text("Rendering preview…")
                }
                else Text(rendering?.exceptionOrNull()?.message ?: "Could not render this image", color = MaterialTheme.colorScheme.error)
            }
        }
        Button(onClick = {
            pending = bitmap
            try { launcher.launch("Tempo-${if (week) "week" else "day"}-$dateText.png") }
            catch (e: ActivityNotFoundException) { pending = null; report("No file picker is available on this device. Install or enable a document provider to save PNGs.") }
        }, enabled = bitmap != null && !busy && pending == null, modifier = Modifier.fillMaxWidth()) { Text(if (busy) "Saving PNG…" else "Save PNG") }
    }
}

@Composable
private fun PrivacyToggle(label: String, checked: Boolean, change: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().toggleable(checked, role = Role.Switch, onValueChange = change), verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.weight(1f)); Switch(checked, null)
    }
}
