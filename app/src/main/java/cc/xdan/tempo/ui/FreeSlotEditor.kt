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
fun FreeSlotEditor(day: Int, time: TimeSpan, dismiss: () -> Unit, session: () -> Unit, pause: () -> Unit,
    people: List<Pair<String, TimeSpan>> = emptyList(), readOnly: Boolean = false) {
    EditorSheet("${durationLabel(time.minutes)} free", dismiss) {
        Text("${weekdayNames[day - 1]} · ${minuteLabel(time.start)}–${minuteLabel(time.end)}", style = MaterialTheme.typography.titleMedium)
        // Who else is free, each with the part of this gap they share.
        if (people.isNotEmpty()) Surface(shape = androidx.compose.foundation.shape.RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surfaceContainerHigh) {
            Column(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                people.forEach { (name, span) ->
                    Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 8.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        PeopleFree(listOf(name), showText = false)
                        Text(name, Modifier.weight(1f), style = MaterialTheme.typography.titleSmall)
                        if (span != time) Text("${minuteLabel(span.start)}–${minuteLabel(span.end)}", style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
        if (!readOnly) Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            val sessionPress = remember { MutableInteractionSource() }
            val breakPress = remember { MutableInteractionSource() }
            Button(onClick = session, modifier = squishWeight(sessionPress).height(56.dp), interactionSource = sessionPress) {
                Icon(Icons.Outlined.EventAvailable, null); Spacer(Modifier.width(8.dp)); Text("Session")
            }
            FilledTonalButton(onClick = pause, modifier = squishWeight(breakPress).height(56.dp), interactionSource = breakPress) {
                Icon(Icons.Outlined.Coffee, null); Spacer(Modifier.width(8.dp)); Text("Break time")
            }
        }
        if (!readOnly) Text("Starts with this time range; you can adjust it before saving.", style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
