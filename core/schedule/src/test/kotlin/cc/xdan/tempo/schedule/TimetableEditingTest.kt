package cc.xdan.tempo.schedule

import cc.xdan.tempo.model.*
import java.time.LocalDate
import org.junit.Assert.*
import org.junit.Test

class TimetableEditingTest {
    private val date = LocalDate.parse("2026-10-12")
    private val session = Session("a", "s", 1, TimeSpan(540, 660), notes = "Keep", cycleWeek = 2)
    private val table = Timetable(subjects = listOf(Subject("s", "Work")), cycle = WeekCycle(2, "2026-10-05"), sessions = listOf(session))
    @Test fun splittingPreservesMetadataAndDuration() {
        val split = splitSession(session, 600, "b")
        assertEquals(session.time.minutes, split.sumOf { it.time.minutes })
        assertEquals(listOf(2, 2), split.map { it.cycleWeek })
        assertEquals(listOf("Keep", "Keep"), split.map { it.notes })
    }
    @Test fun dateEditFreezesResolvedDayWithoutChangingRecurrence() {
        val edited = table.editSessions(date) { it.map { s -> s.copy(location = "New room") } }
        assertEquals(table.sessions, edited.sessions)
        assertEquals("New room", edited.replacementDay(date).sessions.single().location)
        assertNull(edited.replacementDay(date).sessions.single().cycleWeek)
        assertEquals("", edited.replacementDay(date.plusWeeks(2)).sessions.single().location)
    }
    @Test fun subjectDeletionAlsoRemovesDatedReferences() {
        val edited = table.editSessions(date) { it }.removeSubject("s")
        assertTrue(edited.sessions.isEmpty())
        assertTrue(edited.exceptions.single().sessions.isEmpty())
    }
    @Test(expected = IllegalArgumentException::class) fun splitAtBoundaryIsRejected() { splitSession(session, 540, "b") }
    @Test fun selectiveBatchChangesOnlySelectedFieldsAndSessions() {
        val other = session.copy(id = "b", day = 2)
        val original = table.copy(sessions = listOf(session, other))
        val edited = SessionBatchEdit(30, day = 3, location = "Lab").apply(original, setOf("a"))
        assertEquals(other, edited.sessions[1])
        assertEquals(TimeSpan(570, 690), edited.sessions[0].time)
        assertEquals(2, edited.sessions[0].cycleWeek)
        assertEquals("Keep", edited.sessions[0].notes)
        assertEquals("Lab", edited.sessions[0].location)
    }
    @Test(expected = IllegalArgumentException::class) fun invalidBatchCannotPartiallyApply() {
        SessionBatchEdit(-600).apply(table, setOf("a"))
    }
}
