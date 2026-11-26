package cc.xdan.tempo.schedule

import cc.xdan.tempo.model.*
import org.junit.Assert.*
import org.junit.Test

class PeriodPlannerTest {
    @Test fun fourPeriodsMatchTheReferenceCollegeDay() {
        val plan = PeriodPlanner.plan(TimeSpan(540, 980), 4, BreakLengths(20, 50, 10), (1..5).toList())!!
        assertEquals(listOf("Period 1", "Period 2", "Period 3", "Period 4"), plan.periods.map { it.name })
        assertEquals(listOf(TimeSpan(540, 630), TimeSpan(650, 740), TimeSpan(790, 880), TimeSpan(890, 980)), plan.periods.map { it.time })
        assertEquals(listOf(BreakKind.BREAK, BreakKind.LUNCH, BreakKind.CHANGEOVER), plan.breaks.map { it.kind })
        assertEquals(listOf(TimeSpan(630, 650), TimeSpan(740, 790), TimeSpan(880, 890)), plan.breaks.map { it.time })
    }
    @Test fun unusedGapsAreLeftOutAndLastPeriodTakesTheRemainder() {
        val plan = PeriodPlanner.plan(TimeSpan(540, 900), 3, BreakLengths(break_ = null, lunch = 45, changeover = null), listOf(1))!!
        assertEquals(1, plan.breaks.size)
        assertEquals(900, plan.periods.last().time.end)
        assertTrue(plan.periods.all { it.time.minutes >= 10 })
    }
    @Test fun impossibleDaysReturnNull() {
        assertNull(PeriodPlanner.plan(TimeSpan(540, 600), 6, BreakLengths(), listOf(1)))
    }
}
