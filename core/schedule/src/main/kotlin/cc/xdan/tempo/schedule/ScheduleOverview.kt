package cc.xdan.tempo.schedule

import cc.xdan.tempo.model.*
import java.time.LocalDateTime
import java.time.LocalDate

data class UpcomingLesson(val lesson: ScheduleBlock.Lesson, val starts: LocalDateTime)
data class ScheduleOverview(val current: ScheduleBlock?, val next: UpcomingLesson?, val boundary: LocalDateTime?)

fun overview(timetable: Timetable, now: LocalDateTime): ScheduleOverview {
    val minute = now.hour * 60 + now.minute
    val today = ScheduleResolver.resolve(timetable, now.toLocalDate())
    val current = ScheduleResolver.current(today, minute)
    val next = (0..366).firstNotNullOfOrNull { offset ->
        val date = now.toLocalDate().plusDays(offset.toLong())
        ScheduleResolver.resolve(timetable, date).filterIsInstance<ScheduleBlock.Lesson>()
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

fun compareTimetables(timetables: List<Timetable>, date: LocalDate): List<ComparisonSlice> =
    compareTimetables(timetables.map { ScheduleResolver.resolvedTemplate(it, date) }, date.dayOfWeek.value)

enum class WeekStatus { CURRENT, UPCOMING, DONE, EMPTY }
data class WeekOverview(val status: WeekStatus, val block: ScheduleBlock?, val next: UpcomingLesson?, val boundary: LocalDateTime?)

/** Widgets stop at Sunday; next week's lessons reappear when Monday begins. */
fun weekOverview(timetable: Timetable, now: LocalDateTime): WeekOverview {
    val result = overview(timetable, now)
    val end = now.toLocalDate().plusDays((7 - now.dayOfWeek.value).toLong())
    val next = result.next?.takeIf { !it.starts.toLocalDate().isAfter(end) }
    val current = result.current
    val status = when {
        current is ScheduleBlock.Lesson -> WeekStatus.CURRENT
        next != null && current is ScheduleBlock.Break -> WeekStatus.CURRENT
        next != null -> WeekStatus.UPCOMING
        timetable.sessions.isNotEmpty() || timetable.exceptions.any { it.sessions.isNotEmpty() } -> WeekStatus.DONE
        else -> WeekStatus.EMPTY
    }
    return WeekOverview(status, if (status == WeekStatus.CURRENT) current else next?.lesson, next, result.boundary)
}

data class LaneBlock(val block: ScheduleBlock, val lane: Int, val laneCount: Int)
fun scheduleLanes(blocks: List<ScheduleBlock>): List<LaneBlock> {
    val ends = mutableListOf<Int>()
    val assigned = blocks.filterNot { it is ScheduleBlock.Free }.sortedBy { it.time.start }.map { block ->
        val available = ends.indexOfFirst { it <= block.time.start }
        val lane = if (available >= 0) available else ends.size.also { ends.add(0) }
        ends[lane] = block.time.end
        block to lane
    }
    return assigned.map { LaneBlock(it.first, it.second, ends.size.coerceAtLeast(1)) }
}

/** An interval where you and [people] are all free. */
data class SharedFree(val time: TimeSpan, val people: List<String>)

/**
 * Free intervals you share with each person on [date], merged where adjacent. Only calculated
 * frees inside everyone's day hours count, never time outside them.
 */
fun sharedFrees(me: Timetable, people: List<Pair<String, Timetable>>, date: LocalDate): List<SharedFree> {
    val perPerson = people.flatMap { (name, timetable) ->
        val slices = compareTimetables(listOf(me, timetable), date).filter { it.sharedFree }.map { it.time }
        mergeSpans(slices).map { name to it }
    }
    // Group identical intervals so "Alex and Sam" read together.
    return perPerson.groupBy({ it.second }, { it.first }).map { (time, names) -> SharedFree(time, names.distinct()) }.sortedBy { it.time.start }
}

/** Intervals where everyone (you plus all [people]) is free. */
fun everyoneFree(me: Timetable, people: List<Timetable>, date: LocalDate): List<TimeSpan> =
    if (people.isEmpty()) emptyList() else mergeSpans(compareTimetables(listOf(me) + people, date).filter { it.sharedFree }.map { it.time })

private fun mergeSpans(spans: List<TimeSpan>): List<TimeSpan> = spans.sortedBy { it.start }.fold(mutableListOf()) { acc, span ->
    val last = acc.lastOrNull()
    if (last != null && last.end >= span.start) acc[acc.lastIndex] = TimeSpan(last.start, maxOf(last.end, span.end)) else acc += span
    acc
}

/**
 * Names a span by its periods when it lines up exactly with one ("Period 2") or a run of them
 * ("Periods 2–3", or "Registration – Period 1" for custom names). Returns null otherwise.
 */
fun periodLabel(periods: List<PeriodTemplate>, span: TimeSpan): String? {
    val sorted = periods.sortedBy { it.time.start }
    val first = sorted.indexOfFirst { it.time.start == span.start }
    val last = sorted.indexOfLast { it.time.end == span.end }
    if (first == -1 || last == -1 || last < first) return null
    if (first == last) return sorted[first].name
    val numbered = Regex("^Period (\\d+)$")
    val a = numbered.find(sorted[first].name)?.groupValues?.get(1)
    val b = numbered.find(sorted[last].name)?.groupValues?.get(1)
    return if (a != null && b != null) "Periods $a–$b" else "${sorted[first].name} – ${sorted[last].name}"
}
