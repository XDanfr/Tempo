package cc.xdan.tempo.schedule

import cc.xdan.tempo.model.*

data class SessionBatchEdit(val shiftMinutes: Int = 0, val day: Int? = null,
    val changeWeek: Boolean = false, val cycleWeek: Int? = null, val location: String? = null) {
    fun apply(table: Timetable, ids: Set<String>): Timetable {
        require(ids.isNotEmpty() && ids.all { id -> table.sessions.any { it.id == id } }) { "Select existing sessions" }
        return table.copy(sessions = table.sessions.map { session ->
            if (session.id !in ids) session else session.copy(
                time = TimeSpan(session.time.start + shiftMinutes, session.time.end + shiftMinutes),
                day = day ?: session.day, cycleWeek = if (changeWeek) cycleWeek else session.cycleWeek,
                location = location ?: session.location)
        })
    }
}
