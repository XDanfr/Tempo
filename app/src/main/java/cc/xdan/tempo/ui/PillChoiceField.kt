package cc.xdan.tempo.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

@Composable
fun <T> PillChoiceField(label: String, selected: T, options: List<T>, optionLabel: (T) -> String, select: (T) -> Unit) {
    Column {
        Text(label, style = MaterialTheme.typography.labelLarge)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(options) { option -> FilterChip(selected == option, { select(option) }, label = { Text(optionLabel(option)) }) }
        }
    }
}
