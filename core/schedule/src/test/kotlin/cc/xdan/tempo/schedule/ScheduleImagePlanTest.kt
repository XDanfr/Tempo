package cc.xdan.tempo.schedule

import cc.xdan.tempo.model.*
import java.time.LocalDate
import org.junit.Assert.*
import org.junit.Test

class ScheduleImagePlanTest {
    private val date = LocalDate.parse("2026-10-12")
    private val table = Timetable(name = "Private title", subjects = listOf(Subject("s", "Private subject", location = "Private room")),
        sessions = listOf(Session("a", "s", 1, TimeSpan(540, 600), notes = "Private notes")))
    @Test fun privacyRemovesContentBeforeRenderingIncludingInheritedLocations() {
        val plan = scheduleImagePlan(table, date, false, 1080, ImagePrivacy(false, false))
        assertEquals("Timetable", plan.title)
        val lesson = plan.days.single().rows.first { it.title == "Session" }
        assertEquals("", lesson.location); assertEquals("", lesson.notes)
        assertFalse(plan.toString().contains("Private"))
        val visible = scheduleImagePlan(table, date, false, 1080, ImagePrivacy(locations = true, notes = true)).days.single().rows.first()
        assertEquals("Private room", visible.location); assertEquals("Private notes", visible.notes)
    }
    @Test fun weekUsesMondayAndResolvedDatedOverrides() {
        val changed = table.copy(exceptions = listOf(DatedException(date.toString(), closed = true)))
        val plan = scheduleImagePlan(changed, date.plusDays(3), true, 720, ImagePrivacy())
        assertEquals(date, plan.days.first().date)
        assertEquals(date.plusDays(6), plan.days.last().date)
        assertTrue(plan.days.first().rows.isEmpty())
    }
    @Test(expected = IllegalArgumentException::class) fun rejectsUnboundedImageWidth() {
        scheduleImagePlan(table, date, true, 100000, ImagePrivacy())
    }
}
