package cc.xdan.tempo.widgets

import cc.xdan.tempo.model.Timetable
import cc.xdan.tempo.schedule.*
import java.time.LocalDate
import java.time.LocalDateTime

internal data class WidgetDayPlan(val date: LocalDate, val later: List<ScheduleBlock>, val lessonsLeft: Int, val finish: Int)

/** Keep the wide widget on the hero's day; include live overlaps, but no completed periods or artificial gaps. */
internal fun widgetDayPlan(timetable: Timetable, now: LocalDateTime, summary: WeekOverview): WidgetDayPlan? {
    val primary = summary.block ?: return null
    val date = if (summary.status == WeekStatus.CURRENT) now.toLocalDate() else summary.next?.starts?.toLocalDate() ?: return null
    val minute = if (summary.status == WeekStatus.CURRENT) now.hour * 60 + now.minute else primary.time.start
    val remaining = ScheduleResolver.resolve(timetable, date.dayOfWeek.value)
        .filter { it !is ScheduleBlock.Free && it.time.end > minute }
    val lessons = remaining.filterIsInstance<ScheduleBlock.Lesson>()
    val finish = lessons.maxOfOrNull { it.time.end } ?: primary.time.end
    return WidgetDayPlan(date, remaining.filter { it != primary && it.time.start < finish }, lessons.size, finish)
}

/** End-of-day messaging only applies while another lesson remains in this week. */
internal fun widgetDayComplete(timetable: Timetable, now: LocalDateTime, summary: WeekOverview): Boolean {
    if (summary.status != WeekStatus.UPCOMING || summary.next?.starts?.toLocalDate()?.isAfter(now.toLocalDate()) != true) return false
    val minute = now.hour * 60 + now.minute
    return ScheduleResolver.resolve(timetable, now.dayOfWeek.value).filterIsInstance<ScheduleBlock.Lesson>().none { it.time.end > minute }
}
