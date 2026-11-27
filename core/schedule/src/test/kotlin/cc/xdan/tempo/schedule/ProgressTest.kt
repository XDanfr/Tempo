package cc.xdan.tempo.schedule

import cc.xdan.tempo.model.*
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate

class ProgressTest {
    // Monday 2026-11-23; lessons Mon–Fri only.
    private val monday = LocalDate.parse("2026-11-23")
    private val table = Timetable(onboarded = true, subjects = listOf(Subject("m", "Maths")),
        sessions = (1..5).flatMap { d -> listOf(Session("a$d", "m", d, TimeSpan(540, 600)), Session("b$d", "m", d, TimeSpan(600, 660))) },
        progress = ProgressMode.STREAKS)
    private fun checks(vararg days: Long, both: Boolean = false) = days.flatMap { o ->
        val d = monday.plusDays(o); val dow = d.dayOfWeek.value
        listOfNotNull(CheckIn(d.toString(), "a$dow"), if (both) CheckIn(d.toString(), "b$dow") else null) }

    @Test fun xpCountsCheckInsAndFullDayBonus() {
        val s = Progress.summary(table.copy(checkIns = checks(0) + checks(1, both = true)), monday.plusDays(1))
        // Day 1: 10 XP. Day 2: (20 + 5) × 1.1 for a 1-day streak going in = 28.
        assertEquals(10 + 28, s.xp)
        assertEquals(28, s.xpToday)
        assertEquals(1.1f, s.multiplier, 0.001f)
    }
    @Test fun weekendsDoNotBreakStreaks() {
        // Thu, Fri, then Mon: the weekend has no lessons.
        val s = Progress.summary(table.copy(checkIns = checks(3, 4, 7)), monday.plusDays(7))
        assertEquals(3, s.streak)
        assertFalse(s.paused)
    }
    @Test fun oneMissedDayPausesAndTwoEndIt() {
        val paused = Progress.summary(table.copy(checkIns = checks(0, 1)), monday.plusDays(3))
        assertEquals(2, paused.streak)
        val ended = Progress.summary(table.copy(checkIns = checks(0, 1)), monday.plusDays(4))
        assertEquals(0, ended.streak)
        assertEquals(2, ended.bestStreak)
    }
    @Test fun todayWithoutCheckInDoesNotBreakTheStreak() {
        assertEquals(2, Progress.summary(table.copy(checkIns = checks(0, 1)), monday.plusDays(2)).streak)
    }
    @Test fun levelsNeedGrowingXp() {
        assertEquals(100, Progress.levelGoal(1)); assertEquals(150, Progress.levelGoal(2))
    }
    @Test fun streakBonusCapsAtDouble() {
        assertEquals(2f, Progress.multiplierFor(25), 0.001f)
        assertEquals(1f, Progress.multiplierFor(0), 0.001f)
    }
    @Test fun missedDayResetsBonusButFreezeProtectsIt() {
        // Mon, Tue checked in; Wed missed; Thu checked in → Thu has no bonus.
        val missed = Progress.summary(table.copy(checkIns = checks(0, 1, 3)), monday.plusDays(3))
        assertEquals(1f, missed.multiplier, 0.001f)
        // Same, but Wednesday frozen → Thursday keeps a 2-day bonus and the streak carries on.
        val frozen = Progress.summary(table.copy(checkIns = checks(0, 1, 3), frozenDays = listOf(monday.plusDays(2).toString())), monday.plusDays(3))
        assertEquals(1.2f, frozen.multiplier, 0.001f)
        assertEquals(3, frozen.streak)
    }
}
