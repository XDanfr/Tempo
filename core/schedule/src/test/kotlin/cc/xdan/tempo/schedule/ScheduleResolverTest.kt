package cc.xdan.tempo.schedule

import cc.xdan.tempo.model.*
import org.junit.Assert.*
import org.junit.Test

class ScheduleResolverTest {
    private val subject = Subject("t", "Tutor")
    private fun session(id: String, start: Int, end: Int, day: Int = 3) = Session(id, "t", day, TimeSpan(start, end))
    private fun timetable(sessions: List<Session> = emptyList(), periods: List<PeriodTemplate> = emptyList()) =
        Timetable(subjects = listOf(subject), sessions = sessions, days = listOf(DayHours(3, TimeSpan(540, 630))), periods = periods)

    @Test fun tutorAndFreeKeepRealDurations() {
        val blocks = ScheduleResolver.resolve(timetable(listOf(session("1", 540, 600))), 3)
        assertEquals(listOf(60, 30), blocks.map { it.time.minutes })
        assertTrue(blocks.last() is ScheduleBlock.Free)
    }
    @Test fun emptyActiveDayIsAllFree() {
        assertEquals(listOf(ScheduleBlock.Free(TimeSpan(540, 630))), ScheduleResolver.resolve(timetable(), 3))
    }
    @Test fun inactiveDayHasNoArtificialFrees() { assertTrue(ScheduleResolver.resolve(timetable(), 2).isEmpty()) }
    @Test fun touchingSessionsDoNotConflict() {
        assertTrue(ScheduleResolver.conflicts(listOf(session("1", 540, 580), session("2", 580, 630))).isEmpty())
    }
    @Test fun nestedOverlapsDoNotCreateFalseGaps() {
        val sessions = listOf(session("1", 540, 620), session("2", 560, 580))
        assertEquals(1, ScheduleResolver.conflicts(sessions).size)
        assertEquals(listOf(TimeSpan(620, 630)), ScheduleResolver.resolve(timetable(sessions), 3).filterIsInstance<ScheduleBlock.Free>().map { it.time })
    }
    @Test fun periodsSplitFreeTime() {
        val period = PeriodTemplate("p", "Usual", TimeSpan(560, 600))
        assertEquals(listOf(20, 40, 30), ScheduleResolver.resolve(timetable(periods = listOf(period)), 3).map { it.time.minutes })
    }
    @Test fun outsideHoursSessionsDoNotExtendFrees() {
        val result = ScheduleResolver.resolve(timetable(listOf(session("1", 480, 550), session("2", 620, 700))), 3)
        assertEquals(listOf(TimeSpan(550, 620)), result.filterIsInstance<ScheduleBlock.Free>().map { it.time })
        assertEquals(2, result.filterIsInstance<ScheduleBlock.Lesson>().size)
    }
    @Test fun currentPeriodIsEndExclusive() {
        val blocks = ScheduleResolver.resolve(timetable(listOf(session("1", 540, 600))), 3)
        assertTrue(ScheduleResolver.current(blocks, 599) is ScheduleBlock.Lesson)
        assertTrue(ScheduleResolver.current(blocks, 600) is ScheduleBlock.Free)
        assertNull(ScheduleResolver.current(blocks, 630))
    }
    @Test fun nextLessonDoesNotIncludeCurrent() {
        val blocks = ScheduleResolver.resolve(timetable(listOf(session("1", 540, 600))), 3)
        assertNotNull(ScheduleResolver.nextLesson(blocks, 539))
        assertNull(ScheduleResolver.nextLesson(blocks, 540))
    }
    @Test fun lunchOccupiesTimeInsteadOfBecomingFree() {
        val t = timetable().copy(breaks = listOf(ScheduledBreak("l", "Lunch", listOf(3), TimeSpan(570, 600), BreakKind.LUNCH)))
        val blocks = ScheduleResolver.resolve(t, 3)
        assertEquals(listOf(30, 30, 30), blocks.map { it.time.minutes })
        assertTrue(blocks[1] is ScheduleBlock.Break)
        assertEquals(60, blocks.filterIsInstance<ScheduleBlock.Free>().sumOf { it.time.minutes })
    }
    @Test fun breakOnlyAppliesToSelectedDays() {
        val t = timetable().copy(breaks = listOf(ScheduledBreak("b", "Break", listOf(1), TimeSpan(570, 600))))
        assertTrue(ScheduleResolver.resolve(t, 3).none { it is ScheduleBlock.Break })
    }
    @Test fun nestedBreakAndLessonDoNotCreateFalseFrees() {
        val t = timetable(listOf(session("1", 540, 620))).copy(breaks = listOf(ScheduledBreak("b", "Changeover", listOf(3), TimeSpan(560, 580), BreakKind.CHANGEOVER)))
        assertEquals(listOf(TimeSpan(620, 630)), ScheduleResolver.resolve(t, 3).filterIsInstance<ScheduleBlock.Free>().map { it.time })
        assertEquals(1, ScheduleResolver.blockConflicts(t, 3).size)
    }
    @Test fun currentCanBeLunch() {
        val t = timetable().copy(breaks = listOf(ScheduledBreak("l", "Lunch", listOf(3), TimeSpan(570, 600), BreakKind.LUNCH)))
        assertTrue(ScheduleResolver.current(ScheduleResolver.resolve(t, 3), 580) is ScheduleBlock.Break)
    }
    @Test fun midnightEndSupported() { assertEquals(60, TimeSpan(1380, 1440).minutes); assertEquals(1440, parseMinute("24:00")) }
    @Test fun invalidTimesRejected() { assertNull(parseMinute("12:99")); assertNull(parseMinute("25:00")); assertNull(parseMinute("-1:30")) }
    @Test(expected = IllegalArgumentException::class) fun zeroDurationRejected() { TimeSpan(540, 540) }
}
