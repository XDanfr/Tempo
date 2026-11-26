package cc.xdan.tempo.schedule

import cc.xdan.tempo.model.*
import java.time.LocalDate

/**
 * Opt-in progression from explicit check-ins only.
 *
 * XP: 10 per attended session, plus 5 when every session that day is checked in. Levels are
 * cosmetic and need 100, 150, 200 … XP each. Streaks count *scheduled* days (days with at least
 * one session) that have a check-in; days without sessions never break a streak. One missed
 * scheduled day pauses the streak; a second missed day in a row ends it. Today never breaks it.
 */
object Progress {
    const val XP_PER_CHECK_IN = 10
    const val FULL_DAY_BONUS = 5

    data class Summary(val xp: Int, val level: Int, val levelXp: Int, val levelGoal: Int,
        val streak: Int, val bestStreak: Int, val paused: Boolean, val checkedInToday: Int, val xpToday: Int)

    fun levelGoal(level: Int) = 100 + 50 * (level - 1)

    fun xpForDay(timetable: Timetable, date: LocalDate, checkIns: Collection<CheckIn> = timetable.checkIns): Int {
        val ids = checkIns.filter { it.date == date.toString() }.map { it.sessionId }.toSet()
        if (ids.isEmpty()) return 0
        val lessons = ScheduleResolver.resolve(timetable, date).filterIsInstance<ScheduleBlock.Lesson>().map { it.session.id }
        val attended = lessons.count { it in ids }
        return attended * XP_PER_CHECK_IN + if (lessons.isNotEmpty() && attended == lessons.size) FULL_DAY_BONUS else 0
    }

    fun summary(timetable: Timetable, today: LocalDate): Summary {
        val byDate = timetable.checkIns.groupBy { it.date }
        val dates = byDate.keys.mapNotNull { runCatching { LocalDate.parse(it) }.getOrNull() }.sorted()
        val xp = dates.sumOf { xpForDay(timetable, it) }
        var level = 1; var remaining = xp
        while (remaining >= levelGoal(level)) { remaining -= levelGoal(level); level++ }
        val scheduled = { day: LocalDate -> ScheduleResolver.resolve(timetable, day).any { it is ScheduleBlock.Lesson } }
        val attended = { day: LocalDate -> byDate[day.toString()].orEmpty().isNotEmpty() }

        // Current streak, walking back from today over scheduled days.
        var streak = 0; var misses = 0; var paused = false; var day = today
        val earliest = dates.firstOrNull() ?: today
        while (!day.isBefore(earliest)) {
            if (scheduled(day)) {
                when {
                    attended(day) -> { streak++; misses = 0 }
                    day == today -> Unit
                    else -> { misses++; if (streak == 0 && misses == 1) paused = true; if (misses >= 2) break }
                }
            }
            day = day.minusDays(1)
        }
        if (streak == 0) paused = false

        // Best streak, walking forward with the same rule.
        var best = 0; var run = 0; var gap = 0; day = earliest
        while (!day.isAfter(today)) {
            if (scheduled(day)) {
                if (attended(day)) { run++; gap = 0; best = maxOf(best, run) }
                else if (day != today) { gap++; if (gap >= 2) { run = 0; gap = 0 } }
            }
            day = day.plusDays(1)
        }
        return Summary(xp, level, remaining, levelGoal(level), streak, maxOf(best, streak), paused,
            byDate[today.toString()].orEmpty().size, xpForDay(timetable, today))
    }
}
