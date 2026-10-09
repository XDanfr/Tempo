package cc.xdan.tempo.schedule

import cc.xdan.tempo.model.*
import java.time.LocalDateTime
import org.junit.Assert.*
import org.junit.Test

class WeekOverviewTest {
    private val table = Timetable(subjects = listOf(Subject("s", "Maths")), sessions = listOf(Session("a", "s", 5, TimeSpan(600, 660))))
    @Test fun fridayAfterFinalLessonIsDoneRatherThanNextWeek() {
        val result = weekOverview(table, LocalDateTime.of(2026, 10, 9, 11, 0))
        assertEquals(WeekStatus.DONE, result.status); assertNull(result.next)
    }
    @Test fun weekendStaysDoneAndMondayRestoresUpcoming() {
        assertEquals(WeekStatus.DONE, weekOverview(table, LocalDateTime.of(2026, 10, 11, 23, 59)).status)
        assertEquals(WeekStatus.UPCOMING, weekOverview(table, LocalDateTime.of(2026, 10, 12, 0, 0)).status)
    }
    @Test fun activeFinalLessonRemainsCurrent() {
        assertEquals(WeekStatus.CURRENT, weekOverview(table, LocalDateTime.of(2026, 10, 9, 10, 59)).status)
    }
    @Test fun emptyTimetableUsesItsOwnEmptyState() {
        assertEquals(WeekStatus.EMPTY, weekOverview(Timetable(), LocalDateTime.of(2026, 10, 9, 12, 0)).status)
    }
    @Test fun sundayLessonIsIncluded() {
        assertNotNull(weekOverview(table.copy(sessions = listOf(Session("x", "s", 7, TimeSpan(900, 960)))), LocalDateTime.of(2026, 10, 9, 12, 0)).next)
    }
    @Test fun chainedOverlapsUseStableNonCollidingLanes() {
        val subject = table.subjects.first()
        val blocks = listOf(600 to 660, 630 to 690, 660 to 720).mapIndexed { i, (a, b) -> ScheduleBlock.Lesson(Session("$i", "s", 1, TimeSpan(a, b)), subject) }
        val result = scheduleLanes(blocks)
        assertEquals(listOf(0, 1, 0), result.map { it.lane })
        assertTrue(result.all { it.laneCount == 2 })
    }
}
