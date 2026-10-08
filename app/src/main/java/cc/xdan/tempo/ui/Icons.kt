package cc.xdan.tempo.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.ui.graphics.vector.ImageVector
import cc.xdan.tempo.model.SubjectIcon

fun SubjectIcon.vector(): ImageVector? = when (this) {
    SubjectIcon.NONE -> null
    SubjectIcon.CODE -> Icons.Outlined.Code
    SubjectIcon.SCIENCE -> Icons.Outlined.Science
    SubjectIcon.MUSIC -> Icons.Outlined.MusicNote
    SubjectIcon.PERSON -> Icons.Outlined.Person
    SubjectIcon.WORK -> Icons.Outlined.WorkOutline
    SubjectIcon.BOOK -> Icons.Outlined.MenuBook
}
val weekdayNames = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")
