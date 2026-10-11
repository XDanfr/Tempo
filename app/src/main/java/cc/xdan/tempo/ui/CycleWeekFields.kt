package cc.xdan.tempo.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

@Composable
fun CycleWeekFields(weeks: String, change: (String) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        listOf(1, 2, 4).forEach { suggestion ->
            FilterChip(weeks == suggestion.toString(), { change(suggestion.toString()) }, label = { Text(if (suggestion == 1) "1 week" else "$suggestion weeks") })
        }
    }
    OutlinedTextField(weeks, change, label = { Text("Number of weeks") }, supportingText = { Text("Choose a suggestion or enter 1–52") },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, modifier = Modifier.fillMaxWidth())
}
