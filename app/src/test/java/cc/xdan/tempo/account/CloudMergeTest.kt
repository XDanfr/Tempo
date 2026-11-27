package cc.xdan.tempo.account

import cc.xdan.tempo.model.*
import org.junit.Assert.*
import org.junit.Test

class CloudMergeTest {
    private fun saved(id: String, name: String, onboarded: Boolean = true) = SavedTimetable(id, Timetable(name = name, onboarded = onboarded))

    @Test fun keepsTimetablesFromBothSides() {
        val remote = TimetableCollection(timetables = listOf(saved("a", "College"), saved("b", "Work")), activeId = "b")
        val local = TimetableCollection(timetables = listOf(saved("a", "College"), saved("c", "Gym")), activeId = "c")
        val merged = CloudSync.merge(remote, local, preferLocal = true)
        assertEquals(setOf("a", "b", "c"), merged.timetables.map { it.id }.toSet())
        assertEquals("c", merged.activeId)
    }

    @Test fun localEditsWinOnlyWhenPreferred() {
        val remote = TimetableCollection(timetables = listOf(saved("a", "Remote name")), activeId = "a")
        val local = TimetableCollection(timetables = listOf(saved("a", "Local name")), activeId = "a")
        assertEquals("Local name", CloudSync.merge(remote, local, true).timetables.single().timetable.name)
        assertEquals("Remote name", CloudSync.merge(remote, local, false).timetables.single().timetable.name)
    }

    @Test fun emptySetupDeviceTakesTheCloudSelection() {
        val remote = TimetableCollection(timetables = listOf(saved("a", "College")), activeId = "a")
        val local = TimetableCollection(timetables = listOf(saved("local", "My timetable", onboarded = false)), activeId = "local")
        assertEquals("a", CloudSync.merge(remote, local, false).activeId)
    }
}
