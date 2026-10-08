package cc.xdan.tempo.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cc.xdan.tempo.model.*

fun validSpan(start: String, end: String): TimeSpan? {
    val a = parseMinute(start) ?: return null
    val b = parseMinute(end) ?: return null
    return if (a in 0..1439 && b > a) TimeSpan(a, b) else null
}

@Composable
fun TimeFields(start: String, end: String, changeStart: (String) -> Unit, changeEnd: (String) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedTextField(start, changeStart, label = { Text("Start · HH:mm") }, singleLine = true, modifier = Modifier.weight(1f))
        OutlinedTextField(end, changeEnd, label = { Text("End · HH:mm") }, singleLine = true, modifier = Modifier.weight(1f))
    }
    if (validSpan(start, end) == null) Text("Use 24-hour times with the end after the start. Overnight sessions arrive in a later milestone.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
}
