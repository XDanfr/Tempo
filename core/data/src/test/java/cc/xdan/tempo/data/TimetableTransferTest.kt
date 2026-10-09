package cc.xdan.tempo.data

import cc.xdan.tempo.model.*
import org.junit.Assert.*
import org.junit.Test

class TimetableTransferTest {
    @Test fun portableFilePreservesEntireSnapshot() {
        val value = demoTimetable().copy(breaks = listOf(ScheduledBreak("b", "Lunch", listOf(1, 3), TimeSpan(720, 760), BreakKind.LUNCH)),
            appearance = Appearance(preset = ThemePreset.AMBER, blockStyle = BlockStyle.OUTLINED))
        assertEquals(value, TimetableTransfer.read(TimetableTransfer.encode(value).byteInputStream()))
    }
    @Test fun acceptsLegacySnapshotAndMigratesIt() {
        val value = demoTimetable()
        val legacy = TimetableCodec.encode(value).replace("\"schemaVersion\":2", "\"schemaVersion\":1")
        assertEquals(value, TimetableTransfer.decode(legacy))
    }
    @Test(expected = IllegalArgumentException::class) fun rejectsWrongMarker() {
        TimetableTransfer.decode(TimetableTransfer.encode(Timetable()).replace("cc.xdan.tempo.timetable", "another.app"))
    }
    @Test(expected = IllegalArgumentException::class) fun rejectsFutureTransferVersion() {
        TimetableTransfer.decode(TimetableTransfer.encode(Timetable()).replace("\"formatVersion\": 1", "\"formatVersion\": 99"))
    }
    @Test(expected = IllegalArgumentException::class) fun rejectsDanglingSubjectWithoutSaving() {
        TimetableTransfer.decode(TimetableTransfer.encode(demoTimetable()).replace("\"subjectId\": \"cs\"", "\"subjectId\": \"missing\""))
    }
    @Test(expected = IllegalArgumentException::class) fun rejectsOversizedStreams() {
        TimetableTransfer.read(ByteArray(TimetableTransfer.MAX_BYTES + 1).inputStream())
    }
    @Test(expected = IllegalArgumentException::class) fun rejectsDeeplyNestedInput() {
        TimetableTransfer.decode("[".repeat(33) + "0" + "]".repeat(33))
    }
    @Test fun bracesInsideNotesDoNotCountAsNesting() {
        val value = demoTimetable().let { t -> t.copy(sessions = t.sessions.map { it.copy(notes = "[".repeat(100) + "\\\"") }) }
        assertEquals(value, TimetableTransfer.decode(TimetableTransfer.encode(value)))
    }
    @Test fun existingSingleTimetableMigratesToActiveCollection() {
        val timetable = demoTimetable()
        val migrated = TimetableCollectionCodec.decode(TimetableCodec.encode(timetable))
        assertEquals(timetable, migrated.active)
        assertEquals(migrated, TimetableCollectionCodec.decode(TimetableCollectionCodec.encode(migrated)))
    }
    @Test fun editsRemainScopedToTheirOriginalTimetableAfterSwitching() {
        val collection = TimetableCollection().add("college", demoTimetable()).select("local")
        val edited = collection.update("college") { it.copy(name = "Updated college") }
        assertEquals("My timetable", edited.active.name)
        assertEquals("Updated college", edited.select("college").active.name)
    }
    @Test fun deletingActiveTimetableSelectsRemainingSnapshot() {
        val collection = TimetableCollection().add("college", demoTimetable()).remove("college")
        assertEquals("local", collection.activeId)
        assertEquals(Timetable(), collection.active)
    }
    @Test(expected = IllegalArgumentException::class) fun cannotDeleteLastTimetable() { TimetableCollection().remove("local") }
    @Test(expected = IllegalArgumentException::class) fun rejectsDuplicateCollectionIds() { TimetableCollection().add("local", Timetable()) }
}
