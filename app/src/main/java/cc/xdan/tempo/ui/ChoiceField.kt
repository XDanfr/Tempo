package cc.xdan.tempo.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowDropDown
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

/**
 * Read-only field that opens a bottom sheet of options, styled like Tempo's other sheets.
 * Deliberately avoids ExposedDropdownMenuBox, whose anchor gesture never expanded in R8 release builds.
 */
@Composable
fun <T> ChoiceField(label: String, selected: T, options: List<T>, optionLabel: (T) -> String, select: (T) -> Unit,
    leading: (@Composable (T) -> Unit)? = null) {
    var open by remember { mutableStateOf(false) }
    Box(Modifier.fillMaxWidth()) {
        OutlinedTextField(value = optionLabel(selected), onValueChange = {}, readOnly = true, singleLine = true,
            label = { Text(label) },
            leadingIcon = leading?.let { { it(selected) } },
            trailingIcon = { Icon(Icons.Outlined.ArrowDropDown, null) },
            modifier = Modifier.fillMaxWidth())
        // Covers the text field so taps open the sheet instead of focusing the read-only input.
        Box(Modifier.matchParentSize().clickable(remember { MutableInteractionSource() }, null,
            role = Role.DropdownList, onClickLabel = "Choose $label") { open = true })
    }
    if (open) ChoiceSheet(label, selected, options, optionLabel, leading, { open = false }) { select(it) }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun <T> ChoiceSheet(title: String, selected: T, options: List<T>, optionLabel: (T) -> String,
    leading: (@Composable (T) -> Unit)?, dismiss: () -> Unit, select: (T) -> Unit) {
    val state = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val close: (T?) -> Unit = { choice ->
        scope.launch { state.hide() }.invokeOnCompletion { if (choice != null) select(choice); dismiss() }
    }
    ModalBottomSheet(onDismissRequest = dismiss, sheetState = state) {
        Text(title, Modifier.padding(start = 24.dp, end = 24.dp, bottom = 12.dp), style = MaterialTheme.typography.headlineSmall)
        LazyColumn(Modifier.fillMaxWidth(), contentPadding = PaddingValues(start = 24.dp, end = 24.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)) {
            itemsIndexed(options) { _, option ->
                val active = option == selected
                Surface(onClick = { close(option) }, shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth().semantics { this.selected = active },
                    color = if (active) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh) {
                    Row(Modifier.heightIn(min = 56.dp).padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        leading?.invoke(option)
                        Text(optionLabel(option), Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
                        if (active) Icon(Icons.Outlined.CheckCircle, "Selected")
                    }
                }
            }
        }
        Spacer(Modifier.navigationBarsPadding())
    }
}
