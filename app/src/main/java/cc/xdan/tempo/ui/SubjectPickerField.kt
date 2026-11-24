package cc.xdan.tempo.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import cc.xdan.tempo.model.Subject

private fun onSubject(colour: Color) = if (colour.luminance() > .45f) Color(0xFF1B1B1B) else Color.White

@Composable
private fun SubjectBadge(subject: Subject, size: Int = 28) {
    val colour = Color(subject.colour)
    Box(Modifier.size(size.dp).clip(CircleShape).background(colour), contentAlignment = Alignment.Center) {
        val vector = subject.icon.vector()
        if (vector != null) Icon(vector, null, Modifier.size((size * .6f).dp), tint = onSubject(colour))
        else Text(subject.name.take(1).uppercase(), color = onSubject(colour), style = MaterialTheme.typography.labelLarge)
    }
}

/** Subject field that opens a dialog of colour-coded subject cards with their icons. */
@Composable
fun SubjectPickerField(label: String, selectedId: String, subjects: List<Subject>, select: (String) -> Unit) {
    var open by remember { mutableStateOf(false) }
    val current = subjects.firstOrNull { it.id == selectedId }
    Box(Modifier.fillMaxWidth()) {
        OutlinedTextField(value = current?.name ?: "", onValueChange = {}, readOnly = true, singleLine = true,
            label = { Text(label) },
            leadingIcon = current?.let { { SubjectBadge(it, 24) } },
            trailingIcon = { Icon(Icons.Filled.ArrowDropDown, null) },
            modifier = Modifier.fillMaxWidth())
        Box(Modifier.matchParentSize().clickable(remember { MutableInteractionSource() }, null,
            role = Role.DropdownList, onClickLabel = "Choose $label") { open = true })
    }
    if (open) AlertDialog(
        onDismissRequest = { open = false },
        title = { Text("Choose subject") },
        confirmButton = { TextButton(onClick = { open = false }) { Text("Cancel") } },
        text = {
            LazyVerticalGrid(GridCells.Adaptive(140.dp), Modifier.heightIn(max = 420.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(subjects, key = { it.id }) { subject ->
                    val colour = Color(subject.colour)
                    val isSelected = subject.id == selectedId
                    Surface(
                        onClick = { select(subject.id); open = false },
                        modifier = Modifier.semantics { selected = isSelected },
                        shape = MaterialTheme.shapes.large,
                        color = colour.copy(alpha = .22f),
                        border = BorderStroke(if (isSelected) 2.dp else 1.dp, if (isSelected) colour else colour.copy(alpha = .5f)),
                    ) {
                        Row(Modifier.padding(horizontal = 10.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            SubjectBadge(subject)
                            Text(subject.name, Modifier.weight(1f), maxLines = 2, overflow = TextOverflow.Ellipsis,
                                style = MaterialTheme.typography.bodyMedium)
                            if (isSelected) Icon(Icons.Filled.Check, "Selected", Modifier.size(18.dp))
                        }
                    }
                }
            }
        },
    )
}
