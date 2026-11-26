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
        assertEquals(10 + 20 + 5, s.xp)
        assertEquals(25, s.xpToday)
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
}
