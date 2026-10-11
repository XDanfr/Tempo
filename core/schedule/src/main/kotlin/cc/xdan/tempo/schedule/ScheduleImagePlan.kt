package cc.xdan.tempo.schedule

import cc.xdan.tempo.model.*
import java.time.LocalDate

data class ImagePrivacy(val timetableName: Boolean = true, val subjectNames: Boolean = true,
    val locations: Boolean = false, val notes: Boolean = false)
data class ImageRow(val time: TimeSpan, val title: String, val location: String, val notes: String,
    val colour: Long?, val outlined: Boolean)
data class ImageDay(val date: LocalDate, val rows: List<ImageRow>)
data class ScheduleImagePlan(val title: String, val days: List<ImageDay>, val width: Int)

fun scheduleImagePlan(table: Timetable, date: LocalDate, week: Boolean, width: Int, privacy: ImagePrivacy): ScheduleImagePlan {
    require(width in 480..2048) { "Choose a width between 480 and 2048 pixels" }
    val first = if (week) date.minusDays((date.dayOfWeek.value - 1).toLong()) else date
    val days = (0 until if (week) 7 else 1).map { offset ->
        val day = first.plusDays(offset.toLong())
        ImageDay(day, ScheduleResolver.resolve(table, day).map { block ->
            when (block) {
                is ScheduleBlock.Lesson -> ImageRow(block.time, if (privacy.subjectNames) block.subject.name else "Session",
                    if (privacy.locations) block.location else "", if (privacy.notes) block.session.notes else "",
                    block.subject.colour, table.appearance.blockStyle == BlockStyle.OUTLINED)
                is ScheduleBlock.Break -> ImageRow(block.time, if (privacy.subjectNames) block.scheduled.name else "Break", "", "",
                    0xFFE6E6E6, table.appearance.effectiveBreakStyle == BlockStyle.OUTLINED)
                is ScheduleBlock.Free -> ImageRow(block.time, "Free", "", "", null, true)
            }
        })
    }
    return ScheduleImagePlan(if (privacy.timetableName) table.name else "Timetable", days, width)
}
