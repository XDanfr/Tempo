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
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        if (maxWidth < 300.dp) Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            TimeField("Start time", start, changeStart, Modifier.fillMaxWidth())
            TimeField("End time", end, changeEnd, Modifier.fillMaxWidth(), allowEndOfDay = true)
        } else Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            TimeField("Start time", start, changeStart, Modifier.weight(1f))
            TimeField("End time", end, changeEnd, Modifier.weight(1f), allowEndOfDay = true)
        }
    }
    if (validSpan(start, end) == null) Text("Choose an end time after the start time.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
}
