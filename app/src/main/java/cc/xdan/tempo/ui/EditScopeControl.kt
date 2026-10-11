package cc.xdan.tempo.ui

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import java.time.LocalDate

@Composable
fun EditScopeControl(date: LocalDate, dated: Boolean, locked: Boolean, change: (Boolean) -> Unit) {
    if (locked) Text("Applies only to $date. This date has its own schedule.", style = MaterialTheme.typography.bodyMedium)
    else ChoiceField("Applies to", dated, listOf(false, true), { if (it) "Only $date" else "Repeating schedule" }, change)
}
