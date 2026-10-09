package cc.xdan.tempo.widgets

import cc.xdan.tempo.model.*
import cc.xdan.tempo.schedule.*
import java.time.LocalDateTime
import org.junit.Assert.*
import org.junit.Test

class WidgetDayPlanTest {
    private val subject = Subject("s", "Maths")
    private fun table(vararg sessions: Session) = Timetable(subjects = listOf(subject), sessions = sessions.toList())
    private fun plan(t: Timetable, now: LocalDateTime) = widgetDayPlan(t, now, weekOverview(t, now))

    @Test fun tomorrowPreviewUsesTomorrowsDayAndStopsThere() {
        val t = table(Session("a", "s", 2, TimeSpan(600, 660)), Session("b", "s", 2, TimeSpan(720, 780)), Session("c", "s", 3, TimeSpan(600, 900)))
        val result = plan(t, LocalDateTime.of(2026, 10, 12, 20, 0))!!
        assertEquals(2, result.date.dayOfWeek.value)
        assertEquals(2, result.lessonsLeft)
        assertEquals(780, result.finish)
        assertEquals(listOf("b"), result.later.filterIsInstance<ScheduleBlock.Lesson>().map { it.session.id })
    }
    @Test fun liveOverlapIsKeptButFinishedLessonsAreExcluded() {
        val t = table(Session("done", "s", 1, TimeSpan(480, 540)), Session("a", "s", 1, TimeSpan(600, 660)), Session("b", "s", 1, TimeSpan(630, 690)))
        val result = plan(t, LocalDateTime.of(2026, 10, 12, 10, 40))!!
        assertEquals(2, result.lessonsLeft)
        assertEquals(listOf("b"), result.later.filterIsInstance<ScheduleBlock.Lesson>().map { it.session.id })
        assertEquals(690, result.finish)
    }
    @Test fun agendaKeepsMiddayBreaksWithoutGapsOrAfterSchoolBreaks() {
        val t = table(Session("a", "s", 1, TimeSpan(600, 660)), Session("b", "s", 1, TimeSpan(720, 780))).copy(
            days = listOf(DayHours(1, TimeSpan(540, 900))),
            breaks = listOf(ScheduledBreak("lunch", "Lunch", listOf(1), TimeSpan(660, 690)), ScheduledBreak("late", "Late break", listOf(1), TimeSpan(840, 870))))
        val result = plan(t, LocalDateTime.of(2026, 10, 12, 10, 15))!!
        assertEquals(2, result.lessonsLeft)
        assertEquals(2, result.later.size)
        assertEquals("lunch", (result.later.first() as ScheduleBlock.Break).scheduled.id)
        assertEquals(780, result.finish)
    }
    @Test fun completedWeekNeverBuildsANextWeekAgenda() {
        val t = table(Session("a", "s", 5, TimeSpan(600, 660)))
        assertNull(plan(t, LocalDateTime.of(2026, 10, 9, 11, 0)))
    }
    @Test fun completionMessageBeginsAfterTodayEndsButNotDuringAGap() {
        val t = table(Session("a", "s", 1, TimeSpan(600, 660)), Session("b", "s", 1, TimeSpan(720, 780)), Session("c", "s", 2, TimeSpan(600, 660)))
        fun complete(hour: Int) = LocalDateTime.of(2026, 10, 12, hour, 0).let { widgetDayComplete(t, it, weekOverview(t, it)) }
        assertFalse(complete(9))
        assertFalse(complete(11))
        assertTrue(complete(13))
    }
    @Test fun aClearDayCanCompleteButWeeklyDoneOverridesTheDayMessage() {
        val t = table(Session("a", "s", 5, TimeSpan(600, 660)))
        val thursday = LocalDateTime.of(2026, 10, 8, 12, 0)
        assertTrue(widgetDayComplete(t, thursday, weekOverview(t, thursday)))
        val friday = LocalDateTime.of(2026, 10, 9, 11, 0)
        assertFalse(widgetDayComplete(t, friday, weekOverview(t, friday)))
    }
}
