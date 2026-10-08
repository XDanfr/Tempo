package cc.xdan.tempo.ui

import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import cc.xdan.tempo.model.*

@Composable
fun FreeSlotEditor(day: Int, time: TimeSpan, dismiss: () -> Unit, session: () -> Unit, pause: () -> Unit) {
    var type by rememberSaveable { mutableStateOf("Session") }
    EditorSheet("Make use of this free", dismiss, confirm = { if (type == "Session") session() else pause() }, confirmLabel = "Continue") {
        Text("${weekdayNames[day - 1]} · ${minuteLabel(time.start)}–${minuteLabel(time.end)}", style = MaterialTheme.typography.titleMedium)
        Text("The editor will start with this exact time range. You can adjust it before saving.")
        ChoiceField("Add here", type, listOf("Session", "Break time"), { it }, { type = it })
    }
}
