package cc.xdan.tempo.ui

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import java.time.LocalDate

@Composable
fun EditScopeControl(date: LocalDate, dated: Boolean, locked: Boolean, change: (Boolean) -> Unit) {
    val label = friendlyDate(date)
    if (locked) Text("Applies only to $label. This date has its own schedule.", style = MaterialTheme.typography.bodyMedium)
    else ChoiceField("Applies to", dated, listOf(false, true), { if (it) "Only $label" else "Repeating schedule" }, change)
}
