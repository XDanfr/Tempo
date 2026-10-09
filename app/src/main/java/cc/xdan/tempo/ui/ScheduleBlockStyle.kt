package cc.xdan.tempo.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import cc.xdan.tempo.model.*
import cc.xdan.tempo.schedule.ScheduleBlock

fun ScheduleBlock.title(): String = when (this) {
    is ScheduleBlock.Lesson -> subject.name
    is ScheduleBlock.Break -> scheduled.name
    is ScheduleBlock.Free -> "Free"
}
fun ScheduleBlock.accent(fallback: Color): Color = when (this) {
    is ScheduleBlock.Lesson -> Color(subject.colour)
    is ScheduleBlock.Break -> when (scheduled.kind) {
        BreakKind.LUNCH -> Color(0xFFFFD59B)
        BreakKind.CHANGEOVER -> Color(0xFFACE0D3)
        BreakKind.BREAK -> Color(0xFFFFB6C6)
        BreakKind.CUSTOM -> fallback
    }
    is ScheduleBlock.Free -> fallback
}
fun ScheduleBlock.symbol(): ImageVector? = when (this) {
    is ScheduleBlock.Lesson -> subject.icon.vector()
    is ScheduleBlock.Break -> when (scheduled.kind) {
        BreakKind.LUNCH -> Icons.Outlined.Restaurant
        BreakKind.CHANGEOVER -> Icons.Outlined.SwapHoriz
        BreakKind.BREAK -> Icons.Outlined.PauseCircleOutline
        BreakKind.CUSTOM -> Icons.Outlined.LocalCafe
    }
    is ScheduleBlock.Free -> Icons.Outlined.EventAvailable
}
