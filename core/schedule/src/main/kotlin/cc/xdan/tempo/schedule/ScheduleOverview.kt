package cc.xdan.tempo.schedule

import cc.xdan.tempo.model.*
import java.time.LocalDateTime

data class UpcomingLesson(val lesson: ScheduleBlock.Lesson, val starts: LocalDateTime)
data class ScheduleOverview(val current: ScheduleBlock?, val next: UpcomingLesson?, val boundary: LocalDateTime?)

fun overview(timetable: Timetable, now: LocalDateTime): ScheduleOverview {
    val minute = now.hour * 60 + now.minute
    val today = ScheduleResolver.resolve(timetable, now.dayOfWeek.value)
    val current = ScheduleResolver.current(today, minute)
    val next = (0..7).firstNotNullOfOrNull { offset ->
        val date = now.toLocalDate().plusDays(offset.toLong())
        ScheduleResolver.resolve(timetable, date.dayOfWeek.value).filterIsInstance<ScheduleBlock.Lesson>()
            .firstOrNull { offset > 0 || it.time.start > minute }
            ?.let { UpcomingLesson(it, date.atStartOfDay().plusMinutes(it.time.start.toLong())) }
    }
    val boundaryMinute = today.flatMap { listOf(it.time.start, it.time.end) }.filter { it > minute }.minOrNull()
    val boundary = boundaryMinute?.let { now.toLocalDate().atStartOfDay().plusMinutes(it.toLong()) }
        ?: now.toLocalDate().plusDays(1).atStartOfDay()
    return ScheduleOverview(current, next, boundary)
}

data class ComparisonSlice(val time: TimeSpan, val blocks: List<List<ScheduleBlock>>) {
    val sharedFree: Boolean get() = blocks.all { it.isNotEmpty() && it.all { block -> block is ScheduleBlock.Free } }
    val occupiedCount: Int get() = blocks.count { row -> row.any { it !is ScheduleBlock.Free } }
}

/** Split on every boundary so overlaps and shared gaps describe the same interval. */
fun compareTimetables(timetables: List<Timetable>, day: Int): List<ComparisonSlice> {
    require(timetables.size >= 2)
    val schedules = timetables.map { ScheduleResolver.resolve(it, day) }
    val points = schedules.flatten().flatMap { listOf(it.time.start, it.time.end) }.distinct().sorted()
    return points.zipWithNext().map { (a, b) ->
        ComparisonSlice(TimeSpan(a, b), schedules.map { blocks -> blocks.filter { it.time.start < b && it.time.end > a } })
    }.filter { it.blocks.any { row -> row.isNotEmpty() } }
}
