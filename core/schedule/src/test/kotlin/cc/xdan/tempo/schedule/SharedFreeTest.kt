package cc.xdan.tempo.schedule

import cc.xdan.tempo.model.*
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate

class SharedFreeTest {
    private val monday = LocalDate.parse("2026-11-23")
    private val hours = listOf(DayHours(1, TimeSpan(540, 720)))
    private val me = Timetable(subjects = listOf(Subject("m", "Maths")), days = hours, sessions = listOf(Session("a", "m", 1, TimeSpan(540, 600))))
    private val alex = Timetable(subjects = listOf(Subject("m", "Art")), days = hours, sessions = listOf(Session("b", "m", 1, TimeSpan(660, 720))))

    @Test fun sharedFreeIsWhereBothAreFree() {
        val shared = sharedFrees(me, listOf("Alex" to alex), monday)
        assertEquals(listOf(SharedFree(TimeSpan(600, 660), listOf("Alex"))), shared)
    }
    @Test fun identicalIntervalsGroupPeople() {
        val shared = sharedFrees(me, listOf("Alex" to alex, "Sam" to alex), monday)
        assertEquals(listOf("Alex", "Sam"), shared.single().people)
    }
    @Test fun everyoneFreeNeedsAllOfThem() {
        val busySam = alex.copy(sessions = listOf(Session("c", "m", 1, TimeSpan(600, 720))))
        assertTrue(everyoneFree(me, listOf(alex, busySam), monday).isEmpty())
    }
}

class PeriodLabelTest {
    private val periods = listOf(PeriodTemplate("1", "Period 1", TimeSpan(540, 630)), PeriodTemplate("2", "Period 2", TimeSpan(650, 740)),
        PeriodTemplate("3", "Period 3", TimeSpan(790, 880)))
    @Test fun namesExactPeriodsAndRuns() {
        assertEquals("Period 2", periodLabel(periods, TimeSpan(650, 740)))
        assertEquals("Periods 2–3", periodLabel(periods, TimeSpan(650, 880)))
        assertNull(periodLabel(periods, TimeSpan(650, 700)))
        assertEquals("Registration – Period 1", periodLabel(listOf(PeriodTemplate("r", "Registration", TimeSpan(520, 535))) + periods, TimeSpan(520, 630)))
    }
}
