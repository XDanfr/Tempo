package cc.xdan.tempo.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChevronLeft
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.time.LocalDate

fun validDate(text: String): LocalDate? = runCatching { LocalDate.parse(text) }.getOrNull()

@Composable
fun DateField(label: String, value: String, change: (String) -> Unit) {
    OutlinedTextField(value, change, label = { Text(label) }, placeholder = { Text("YYYY-MM-DD") },
        supportingText = { if (validDate(value) == null) Text("Enter a date as YYYY-MM-DD") },
        isError = validDate(value) == null, singleLine = true, modifier = Modifier.fillMaxWidth())
}

@Composable
fun DateNavigation(date: LocalDate, select: (LocalDate) -> Unit) {
    var choosing by remember { mutableStateOf(false) }
    Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = { select(date.minusWeeks(1)) }) { Icon(Icons.Outlined.ChevronLeft, "Previous week") }
        TextButton(onClick = { choosing = true }, modifier = Modifier.weight(1f)) { Text(date.toString()) }
        TextButton(onClick = { select(LocalDate.now()) }) { Text("Today") }
        IconButton(onClick = { select(date.plusWeeks(1)) }) { Icon(Icons.Outlined.ChevronRight, "Next week") }
    }
    if (choosing) {
        var text by rememberSaveable { mutableStateOf(date.toString()) }
        val parsed = validDate(text)
        EditorSheet("Go to date", { choosing = false }, confirm = { parsed?.let(select); choosing = false }, enabled = parsed != null) {
            DateField("Date", text) { text = it }
        }
    }
}
