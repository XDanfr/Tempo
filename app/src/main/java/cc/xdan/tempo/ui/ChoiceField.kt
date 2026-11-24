package cc.xdan.tempo.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.semantics.Role

/**
 * Read-only dropdown field. Built on a plain [DropdownMenu] with an explicit click overlay rather than
 * ExposedDropdownMenuBox, whose anchor gesture never expanded the menu in R8-minified release builds.
 */
@Composable
fun <T> ChoiceField(label: String, selected: T, options: List<T>, optionLabel: (T) -> String, select: (T) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box(Modifier.fillMaxWidth()) {
        OutlinedTextField(value = optionLabel(selected), onValueChange = {}, readOnly = true,
            label = { Text(label) },
            trailingIcon = { Icon(Icons.Filled.ArrowDropDown, null, Modifier.rotate(if (expanded) 180f else 0f)) },
            modifier = Modifier.fillMaxWidth(), singleLine = true)
        // Covers the text field so taps open the menu instead of focusing the read-only input.
        Box(Modifier.matchParentSize().clickable(
            interactionSource = remember { MutableInteractionSource() }, indication = null,
            role = Role.DropdownList, onClickLabel = label) { expanded = true })
        DropdownMenu(expanded, { expanded = false }) {
            options.forEach { option -> DropdownMenuItem(text = { Text(optionLabel(option)) }, onClick = { select(option); expanded = false }) }
        }
    }
}
