package cc.xdan.tempo.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.unit.dp

@Composable
fun CycleWeekFields(weeks: String, change: (String) -> Unit) {
    val focus = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        listOf(1, 2, 4).forEach { suggestion ->
            FilterChip(weeks == suggestion.toString(), { change(suggestion.toString()) }, label = { Text(if (suggestion == 1) "1 week" else "$suggestion weeks") })
        }
    }
    OutlinedTextField(weeks, change, label = { Text("Number of weeks") }, supportingText = { Text("Choose a suggestion or enter 1–52") },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { focus.clearFocus(); keyboard?.hide() }),
        singleLine = true, modifier = Modifier.fillMaxWidth())
}
