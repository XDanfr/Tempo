package cc.xdan.tempo.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cc.xdan.tempo.model.SubjectIcon

@Composable
fun IconPicker(selected: SubjectIcon, dismiss: () -> Unit, choose: (SubjectIcon) -> Unit) {
    var query by rememberSaveable { mutableStateOf("") }
    var category by rememberSaveable { mutableStateOf("All") }
    var pending by rememberSaveable { mutableStateOf(selected) }
    val matches = iconOptions.filter { option ->
        (category == "All" || option.category == category) &&
            (option.title + " " + option.keywords).contains(query.trim(), ignoreCase = true)
    }
    EditorSheet("Choose an icon", dismiss, confirm = { choose(pending) }, confirmLabel = "Use icon") {
        OutlinedTextField(query, { query = it }, label = { Text("Search ${iconOptions.size} icons") },
            leadingIcon = { Icon(Icons.Outlined.Search, null) }, modifier = Modifier.fillMaxWidth(), singleLine = true,
            trailingIcon = { if (query.isNotEmpty()) IconButton(onClick = { query = "" }) { Icon(Icons.Outlined.Close, "Clear search") } })
        ChoiceField("Category", category, listOf("All", "Study", "Creative", "Work", "Everyday"), { it }, { category = it })
        if (matches.isEmpty()) Text("No matching icons. Try another word or category.")
        LazyVerticalGrid(columns = GridCells.Adaptive(88.dp), modifier = Modifier.fillMaxWidth().heightIn(min = 112.dp, max = 360.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(matches, key = { it.icon.name }) { option ->
                val active = pending == option.icon
                Surface(onClick = { pending = option.icon }, shape = MaterialTheme.shapes.medium,
                    color = if (active) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
                    border = if (active) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null) {
                    Column(Modifier.heightIn(min = 88.dp).padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(option.icon.vector() ?: Icons.Outlined.Block, null, Modifier.size(28.dp))
                        Text(option.title, style = MaterialTheme.typography.labelMedium, maxLines = 2)
                    }
                }
            }
        }
    }
}
