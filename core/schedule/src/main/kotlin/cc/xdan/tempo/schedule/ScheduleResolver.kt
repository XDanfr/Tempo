package cc.xdan.tempo.schedule

import cc.xdan.tempo.model.*

sealed interface ScheduleBlock {
    val time: TimeSpan
    data class Lesson(val session: Session, val subject: Subject) : ScheduleBlock {
        override val time get() = session.time
        val location get() = session.location.ifBlank { subject.location }
    }
    data class Break(val scheduled: ScheduledBreak) : ScheduleBlock { override val time get() = scheduled.time }
    data class Free(override val time: TimeSpan) : ScheduleBlock
}

data class SessionConflict(val first: Session, val second: Session)

object ScheduleResolver {
    /** Frees occupy the configured day only. Sessions outside it remain visible. */
    fun resolve(timetable: Timetable, day: Int): List<ScheduleBlock> {
        require(day in 1..7)
        val lessons = timetable.sessions.filter { it.day == day }
            .sortedWith(compareBy<Session> { it.time.start }.thenBy { it.time.end })
            .map { ScheduleBlock.Lesson(it, timetable.subjects.first { subject -> subject.id == it.subjectId }) }
        val breaks = timetable.breaks.filter { day in it.days }.map { ScheduleBlock.Break(it) }
        val occupied: List<ScheduleBlock> = (lessons + breaks).sortedBy { it.time.start }
        val hours = timetable.days.firstOrNull { it.day == day }?.time ?: return occupied
        val boundaries = timetable.periods.flatMap { listOf(it.time.start, it.time.end) }
            .filter { it > hours.start && it < hours.end }.distinct().sorted()
        val frees = mutableListOf<ScheduleBlock.Free>()
        fun addFree(start: Int, end: Int) {
            if (end <= start) return
            val points = listOf(start) + boundaries.filter { it > start && it < end } + end
            points.zipWithNext().forEach { (a, b) -> frees += ScheduleBlock.Free(TimeSpan(a, b)) }
        }
        var cursor = hours.start
        for (lesson in occupied) {
            val start = lesson.time.start.coerceIn(hours.start, hours.end)
            val end = lesson.time.end.coerceIn(hours.start, hours.end)
            addFree(cursor, start)
            cursor = maxOf(cursor, end)
        }
        addFree(cursor, hours.end)
        return (occupied + frees).sortedBy { it.time.start }
    }

    fun conflicts(sessions: List<Session>): List<SessionConflict> = buildList {
        for (i in sessions.indices) for (j in i + 1 until sessions.size) {
            val a = sessions[i]; val b = sessions[j]
            if (a.day == b.day && a.time.start < b.time.end && b.time.start < a.time.end) {
                add(SessionConflict(a, b))
            }
        }
    }

    fun blockConflicts(timetable: Timetable, day: Int): List<Pair<ScheduleBlock, ScheduleBlock>> {
        val blocks = resolve(timetable, day).filterNot { it is ScheduleBlock.Free }
        return buildList {
            for (i in blocks.indices) for (j in i + 1 until blocks.size) {
                val a = blocks[i]; val b = blocks[j]
                if (a.time.start < b.time.end && b.time.start < a.time.end) add(a to b)
            }
        }
    }

    fun current(blocks: List<ScheduleBlock>, minute: Int): ScheduleBlock? =
        blocks.firstOrNull { minute >= it.time.start && minute < it.time.end }

    fun nextLesson(blocks: List<ScheduleBlock>, minute: Int): ScheduleBlock.Lesson? =
        blocks.filterIsInstance<ScheduleBlock.Lesson>().firstOrNull { it.time.start > minute }
}
