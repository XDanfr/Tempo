package cc.xdan.tempo.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import cc.xdan.tempo.TempoState
import cc.xdan.tempo.TempoViewModel

data class TimetableFileActions(val import: () -> Unit, val export: () -> Unit)

@Composable
fun timetableFileActions(model: TempoViewModel, state: TempoState): TimetableFileActions {
    val resolver = LocalContext.current.contentResolver
    val open = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { model.readImport(resolver, it) }
    val save = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { model.writeExport(resolver, it) }
    state.importPreview?.let { preview ->
        val timetable = preview.timetable
        val target = state.collection?.timetables?.firstOrNull { it.id == preview.targetId }?.timetable
        var confirmReplace by remember(preview) { mutableStateOf(false) }
        if (confirmReplace) AlertDialog(onDismissRequest = { confirmReplace = false },
            title = { Text("Replace ${target?.name ?: "this timetable"}?") },
            text = { Text("Its subjects, sessions, breaks, hours and appearance will be replaced by ${timetable.name}. Export it first if you want to keep a copy.") },
            confirmButton = { TextButton(onClick = { model.acceptImport(true) }) { Text("Replace timetable") } },
            dismissButton = { TextButton(onClick = { confirmReplace = false }) { Text("Keep current") } })
        else AlertDialog(onDismissRequest = model::dismissImport,
            title = { Text("Import ${timetable.name}") },
            text = { Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("${timetable.subjects.size} subjects · ${timetable.sessions.size} sessions · ${timetable.breaks.size} breaks")
                Text("Includes colours, icons, notes, day hours, periods and appearance.")
                Button(onClick = { model.acceptImport(false) }, modifier = Modifier.fillMaxWidth()) { Text("Add as a new timetable") }
                if (target != null) OutlinedButton(onClick = {
                    if (!target.onboarded && target.subjects.isEmpty() && target.sessions.isEmpty() && target.breaks.isEmpty()) model.acceptImport(true)
                    else confirmReplace = true
                }, modifier = Modifier.fillMaxWidth()) { Text(if (!target.onboarded) "Use for this setup" else "Replace ${target.name}") }
            } },
            confirmButton = { TextButton(onClick = model::dismissImport) { Text("Cancel") } })
    }
    return TimetableFileActions(
        import = { if (!state.fileBusy) { model.beginImport(); open.launch(arrayOf("application/json", "application/octet-stream", "text/plain")) } },
        export = { if (!state.fileBusy) model.beginExport()?.let(save::launch) },
    )
}
