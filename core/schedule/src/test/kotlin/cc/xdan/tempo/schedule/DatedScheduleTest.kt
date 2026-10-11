package cc.xdan.tempo.schedule

import cc.xdan.tempo.model.*
import java.time.LocalDate
import org.junit.Assert.*
import org.junit.Test

class DatedScheduleTest {
    private val monday = LocalDate.parse("2026-10-12")
    private val table = Timetable(subjects = listOf(Subject("s", "Maths")), cycle = WeekCycle(3, monday.toString()),
        sessions = listOf(Session("a", "s", 1, TimeSpan(540, 600), cycleWeek = 1),
            Session("b", "s", 1, TimeSpan(600, 660), cycleWeek = 2),
            Session("all", "s", 1, TimeSpan(720, 780))))

    @Test fun cyclesAcrossAnchorIncludingNegativeWeeks() {
        assertEquals(1, ScheduleResolver.cycleWeek(table, monday.plusDays(6)))
        assertEquals(2, ScheduleResolver.cycleWeek(table, monday.plusWeeks(1)))
        assertEquals(3, ScheduleResolver.cycleWeek(table, monday.minusWeeks(1)))
        assertEquals(1, ScheduleResolver.cycleWeek(table, monday.plusWeeks(3)))
        assertEquals(listOf("b", "all"), ScheduleResolver.resolve(table, monday.plusWeeks(1))
            .filterIsInstance<ScheduleBlock.Lesson>().map { it.session.id })
    }
    @Test fun holidaysCloseAllBlocksAndExceptionsOverrideThem() {
        val holiday = table.copy(holidays = listOf(Holiday("h", "Holiday", monday.toString(), monday.plusWeeks(1).toString())))
        assertTrue(ScheduleResolver.resolve(holiday, monday).isEmpty())
        assertTrue(ScheduleResolver.resolve(holiday, monday.plusWeeks(1)).isEmpty())
        val replacement = Session("extra", "s", 1, TimeSpan(810, 900))
        val override = holiday.copy(exceptions = listOf(DatedException(monday.toString(), sessions = listOf(replacement))))
        assertEquals(listOf(replacement), ScheduleResolver.resolve(override, monday).filterIsInstance<ScheduleBlock.Lesson>().map { it.session })
    }
    @Test fun nextLessonSearchesPastHolidaysAndWidgetStillStopsAtSunday() {
        val holiday = table.copy(holidays = listOf(Holiday("h", "Holiday", monday.toString(), monday.plusWeeks(3).toString())))
        assertEquals(monday.plusWeeks(4), overview(holiday, monday.atStartOfDay()).next!!.starts.toLocalDate())
        assertEquals(WeekStatus.DONE, weekOverview(holiday, monday.atStartOfDay()).status)
    }
    @Test fun comparisonUsesEachTablesOwnAnchorAndClosedDaysAreNotSharedFree() {
        val other = table.copy(cycle = WeekCycle(3, monday.minusWeeks(1).toString()))
        val slices = compareTimetables(listOf(table, other), monday)
        assertTrue(slices.any { it.blocks[0].any { b -> b is ScheduleBlock.Lesson && b.session.id == "a" } })
        assertTrue(slices.any { it.blocks[1].any { b -> b is ScheduleBlock.Lesson && b.session.id == "b" } })
        val closed = other.copy(exceptions = listOf(DatedException(monday.toString(), closed = true)))
        assertFalse(compareTimetables(listOf(table, closed), monday).any { it.sharedFree })
    }
    @Test(expected = IllegalArgumentException::class) fun danglingExceptionSubjectIsRejected() {
        Timetable(exceptions = listOf(DatedException(monday.toString(), sessions = listOf(Session("x", "missing", 1, TimeSpan(540, 600))))))
    }
}
