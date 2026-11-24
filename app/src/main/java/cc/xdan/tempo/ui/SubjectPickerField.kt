package cc.xdan.tempo.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.dp
import cc.xdan.tempo.model.Subject

private fun onSubject(colour: Color) = if (colour.luminance() > .45f) Color(0xFF1B1B1B) else Color.White

/** Subject colour disc with its icon, or initial when it has none. */
@Composable
fun SubjectBadge(subject: Subject, size: Int = 32) {
    val colour = Color(subject.colour)
    Box(Modifier.size(size.dp).clip(CircleShape).background(colour), contentAlignment = Alignment.Center) {
        val vector = subject.icon.vector()
        if (vector != null) Icon(vector, null, Modifier.size((size * .6f).dp), tint = onSubject(colour))
        else Text(subject.name.take(1).uppercase(), color = onSubject(colour), style = MaterialTheme.typography.labelLarge)
    }
}

/** Subject choice using the shared bottom-sheet picker, with each subject's colour and icon. */
@Composable
fun SubjectPickerField(label: String, selectedId: String, subjects: List<Subject>, select: (String) -> Unit) {
    val byId = subjects.associateBy { it.id }
    ChoiceField(label, selectedId, subjects.map { it.id }, { byId[it]?.name ?: "" }, select,
        leading = { id -> byId[id]?.let { SubjectBadge(it, 28) } })
}
