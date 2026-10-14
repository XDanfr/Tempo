package cc.xdan.tempo.widgets

import cc.xdan.tempo.model.*
import java.time.*
import org.junit.Assert.*
import org.junit.Test

class WidgetPresentationTest {
    private val table = Timetable(onboarded = true, subjects = listOf(Subject("s", "Computer Science", location = "Private room")),
        sessions = listOf(Session("a", "s", 1, TimeSpan(600, 660)), Session("b", "s", 2, TimeSpan(600, 660))))
    private fun at(hour: Int, minute: Int = 0) = ZonedDateTime.of(2026, 10, 12, hour, minute, 0, 0, ZoneId.of("Europe/London"))
    @Test fun boundaryChangesUpcomingToCurrentToDayComplete() {
        val upcoming = widgetPresentation(table, WidgetSettings(), at(9, 59))
        val current = widgetPresentation(table, WidgetSettings(), at(10))
        val complete = widgetPresentation(table, WidgetSettings(doneForDayMessage = true), at(11))
        assertEquals(WidgetState.UPCOMING, upcoming.state)
        assertEquals("Starts in", upcoming.countdownLabel)
        assertEquals(at(10).toInstant().toEpochMilli(), upcoming.targetEpochMillis)
        assertEquals(WidgetState.CURRENT, current.state)
        assertEquals("Time left", current.countdownLabel)
        assertEquals(at(11).toInstant().toEpochMilli(), current.targetEpochMillis)
        assertEquals(0f, current.progress)
        assertEquals(WidgetState.DONE_TODAY, complete.state)
        assertNull(complete.targetEpochMillis)
    }
    @Test fun snapshotRespectsPrivacyAndProgress() {
        val result = widgetPresentation(table, WidgetSettings(showLocation = false), at(10, 30))
        assertNull(result.location)
        assertEquals(.5f, result.progress)
        assertEquals("Computer Science", result.title)
    }
    @Test fun missingSetupEmptyAndWeeklyCompletionHaveNoTimer() {
        val cases = listOf(null to WidgetState.MISSING, table.copy(onboarded = false) to WidgetState.SETUP,
            table.copy(sessions = emptyList()) to WidgetState.EMPTY,
            table.copy(sessions = table.sessions.take(1)) to WidgetState.WEEK_DONE)
        cases.forEach { (t, state) ->
            val result = widgetPresentation(t, WidgetSettings(emptyText = "A clear week"), at(12))
            assertEquals(state, result.state)
            assertNull(result.targetEpochMillis)
        }
    }
    @Test fun scheduledBreakStaysABreakWithItsOwnEnd() {
        val t = table.copy(sessions = table.sessions + Session("c", "s", 1, TimeSpan(720, 780)),
            breaks = listOf(ScheduledBreak("break", "Lunch", listOf(1), TimeSpan(660, 690))))
        val result = widgetPresentation(t, WidgetSettings(), at(11, 10))
        assertEquals(WidgetState.BREAK, result.state)
        assertEquals("Lunch", result.title)
        assertEquals(at(11, 30).toInstant().toEpochMilli(), result.targetEpochMillis)
    }
    @Test fun freeGapShowsNextLessonWithoutInventingCurrentSubject() {
        val t = table.copy(sessions = table.sessions + Session("c", "s", 1, TimeSpan(720, 780)), days = listOf(DayHours(1, TimeSpan(540, 900))))
        val result = widgetPresentation(t, WidgetSettings(), at(11, 10))
        assertEquals(WidgetState.UPCOMING, result.state)
        assertFalse(result.current)
        assertNull(result.progress)
        assertEquals(at(12).toInstant().toEpochMilli(), result.targetEpochMillis)
    }
}
