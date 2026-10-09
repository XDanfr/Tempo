package cc.xdan.tempo.schedule

import cc.xdan.tempo.model.*
import java.time.LocalDateTime
import org.junit.Test
import org.junit.Assert.*

class ScheduleOverviewTest {
    private val subject = Subject("s", "Maths")
    private fun timetable(day: Int = 1, start: Int = 600) = Timetable(subjects = listOf(subject), sessions = listOf(Session("a", "s", day, TimeSpan(start, start + 60))))
    @Test fun findsNextWeekAfterLastSession() {
        val result = overview(timetable(), LocalDateTime.of(2026, 10, 5, 12, 0))
        assertEquals(LocalDateTime.of(2026, 10, 12, 10, 0), result.next?.starts)
    }
    @Test fun exactEndIsNoLongerCurrentLesson() {
        assertFalse(overview(timetable(), LocalDateTime.of(2026, 10, 5, 11, 0)).current is ScheduleBlock.Lesson)
    }
    @Test fun breaksKeepTheirNextLesson() {
        val t = timetable(start = 720).copy(breaks = listOf(ScheduledBreak("b", "Lunch", listOf(1), TimeSpan(660, 720))))
        val result = overview(t, LocalDateTime.of(2026, 10, 5, 11, 30))
        assertTrue(result.current is ScheduleBlock.Break)
        assertEquals("Maths", result.next?.lesson?.subject?.name)
    }
    @Test fun emptyTimetableHasNoUpcomingLesson() {
        assertNull(overview(Timetable(), LocalDateTime.of(2026, 10, 5, 9, 0)).next)
    }
    @Test fun sharedFreeExcludesTimeOutsideEitherDay() {
        val a = timetable().copy(days = listOf(DayHours(1, TimeSpan(540, 900))))
        val b = timetable(start = 720).copy(days = listOf(DayHours(1, TimeSpan(600, 840))))
        val slices = compareTimetables(listOf(a, b), 1)
        assertEquals(120, slices.filter { it.sharedFree }.sumOf { it.time.minutes })
        assertFalse(slices.first().sharedFree)
    }
    @Test fun overlappingLessonsRemainVisibleInComparison() {
        val a = timetable().let { it.copy(sessions = it.sessions + Session("b", "s", 1, TimeSpan(630, 690))) }
        assertTrue(compareTimetables(listOf(a, timetable()), 1).any { it.blocks.first().size == 2 })
    }
}
