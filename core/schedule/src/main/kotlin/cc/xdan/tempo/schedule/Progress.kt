package cc.xdan.tempo.schedule

import cc.xdan.tempo.model.*
import java.time.LocalDate

/**
 * Opt-in progression from explicit check-ins only.
 *
 * XP: 10 per attended session, plus 5 when every session that day is checked in, multiplied by a
 * streak bonus of +10% per streak day going into that day (up to ×2 at 10 days). Missing a day
 * resets the bonus straight away, even though the streak itself only pauses; frozen days (illness,
 * a day you couldn't make) count as if nothing was scheduled, so they protect both. Levels are
 * cosmetic and need 100, 150, 200 … XP each. Streaks count *scheduled* days (days with at least
 * one session) that have a check-in; days without sessions never break a streak. One missed
 * scheduled day pauses the streak; a second missed day in a row ends it. Today never breaks it.
 */
object Progress {
    const val XP_PER_CHECK_IN = 10
    const val FULL_DAY_BONUS = 5

    data class Summary(val xp: Int, val level: Int, val levelXp: Int, val levelGoal: Int,
        val streak: Int, val bestStreak: Int, val paused: Boolean, val checkedInToday: Int, val xpToday: Int,
        /** Today's streak bonus, e.g. 1.4 for a 4-day streak going into today. */
        val multiplier: Float = 1f)

    const val MAX_BONUS_DAYS = 10
    fun multiplierFor(streakBefore: Int) = 1f + 0.1f * streakBefore.coerceIn(0, MAX_BONUS_DAYS)

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
        val frozen = timetable.frozenDays.toSet()
        val scheduledDay = { day: LocalDate -> day.toString() !in frozen && ScheduleResolver.resolve(timetable, day).any { it is ScheduleBlock.Lesson } }
        // Walk forward once, tracking the streak going into each day, so each day's XP gets the bonus it earned then.
        var xp = 0; var running = 0; var todayMultiplier = 1f; var xpToday = 0
        if (dates.isNotEmpty()) {
            var walk = dates.first()
            while (!walk.isAfter(today)) {
                if (scheduledDay(walk)) {
                    val base = xpForDay(timetable, walk)
                    val multiplier = multiplierFor(running)
                    if (walk == today) todayMultiplier = multiplier
                    if (base > 0) {
                        val earned = kotlin.math.round(base * multiplier).toInt()
                        xp += earned; if (walk == today) xpToday = earned
                        running++
                    } else if (walk != today) { running = 0 } // A missed day resets the bonus.
                }
                walk = walk.plusDays(1)
            }
        }
        xp += timetable.bonusXp
        var level = 1; var remaining = xp
        while (remaining >= levelGoal(level)) { remaining -= levelGoal(level); level++ }
        val scheduled = scheduledDay
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
            byDate[today.toString()].orEmpty().size, xpToday, if (dates.isEmpty()) 1f else todayMultiplier)
    }

    /** A finished week at a glance: check-ins out of scheduled sessions, XP earned, and the day with the most check-ins. */
    data class WeekRecap(val attended: Int, val scheduled: Int, val xp: Int, val bestDay: LocalDate?)

    fun weekRecap(timetable: Timetable, monday: LocalDate): WeekRecap {
        val days = (0L..6L).map { monday.plusDays(it) }
        val lessons = days.associateWith { day -> ScheduleResolver.resolve(timetable, day).filterIsInstance<ScheduleBlock.Lesson>().map { it.session.id }.toSet() }
        val checked = days.associateWith { day -> timetable.checkIns.count { it.date == day.toString() && it.sessionId in lessons.getValue(day) } }
        val xp = summary(timetable.copy(bonusXp = 0), days.last()).xp - summary(timetable.copy(bonusXp = 0), monday.minusDays(1)).xp
        return WeekRecap(checked.values.sum(), lessons.values.sumOf { it.size }, xp, checked.filterValues { it > 0 }.maxByOrNull { it.value }?.key)
    }
}
