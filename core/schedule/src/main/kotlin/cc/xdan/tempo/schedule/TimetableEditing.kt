package cc.xdan.tempo.schedule

import cc.xdan.tempo.model.*
import java.time.LocalDate

fun Timetable.replacementDay(date: LocalDate): DatedException = exceptions.firstOrNull { it.date == date.toString() }
    ?: ScheduleResolver.resolvedTemplate(this, date).let { resolved ->
        DatedException(date.toString(), sessions = resolved.sessions.filter { it.day == date.dayOfWeek.value }.map { it.copy(cycleWeek = null) },
            breaks = resolved.breaks.filter { date.dayOfWeek.value in it.days }.map { it.copy(days = listOf(date.dayOfWeek.value), cycleWeek = null) })
    }

fun Timetable.editDate(date: LocalDate, transform: (DatedException) -> DatedException): Timetable =
    copy(schemaVersion = 3, exceptions = exceptions.filterNot { it.date == date.toString() } + transform(replacementDay(date)))

fun Timetable.editSessions(date: LocalDate? = null, transform: (List<Session>) -> List<Session>): Timetable =
    if (date == null) copy(sessions = transform(sessions))
    else editDate(date) { it.copy(closed = false, sessions = transform(it.sessions)) }

fun Timetable.editBreaks(date: LocalDate? = null, transform: (List<ScheduledBreak>) -> List<ScheduledBreak>): Timetable =
    if (date == null) copy(breaks = transform(breaks))
    else editDate(date) { it.copy(closed = false, breaks = transform(it.breaks)) }

fun splitSession(session: Session, minute: Int, secondId: String): List<Session> {
    require(minute > session.time.start && minute < session.time.end)
    require(secondId.isNotBlank() && secondId != session.id)
    return listOf(session.copy(time = TimeSpan(session.time.start, minute)), session.copy(id = secondId, time = TimeSpan(minute, session.time.end)))
}

fun Timetable.removeSubject(id: String): Timetable = copy(subjects = subjects.filterNot { it.id == id },
    sessions = sessions.filterNot { it.subjectId == id },
    exceptions = exceptions.map { it.copy(sessions = it.sessions.filterNot { s -> s.subjectId == id }) })
