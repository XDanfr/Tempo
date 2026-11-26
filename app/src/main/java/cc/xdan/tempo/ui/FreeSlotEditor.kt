package cc.xdan.tempo.ui

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Coffee
import androidx.compose.material.icons.outlined.EventAvailable
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cc.xdan.tempo.model.*

/** Two choices, two buttons: each opens its editor pre-filled with this gap's exact times. */
@Composable
fun FreeSlotEditor(day: Int, time: TimeSpan, dismiss: () -> Unit, session: () -> Unit, pause: () -> Unit) {
    EditorSheet("${durationLabel(time.minutes)} free", dismiss) {
        Text("${weekdayNames[day - 1]} · ${minuteLabel(time.start)}–${minuteLabel(time.end)}", style = MaterialTheme.typography.titleMedium)
        Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            val sessionPress = remember { MutableInteractionSource() }
            val breakPress = remember { MutableInteractionSource() }
            Button(onClick = session, modifier = squishWeight(sessionPress).height(56.dp), interactionSource = sessionPress) {
                Icon(Icons.Outlined.EventAvailable, null); Spacer(Modifier.width(8.dp)); Text("Session")
            }
            FilledTonalButton(onClick = pause, modifier = squishWeight(breakPress).height(56.dp), interactionSource = breakPress) {
                Icon(Icons.Outlined.Coffee, null); Spacer(Modifier.width(8.dp)); Text("Break time")
            }
        }
        Text("Starts with this time range; you can adjust it before saving.", style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
