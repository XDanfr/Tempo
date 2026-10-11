package cc.xdan.tempo.data

import cc.xdan.tempo.model.*
import kotlinx.serialization.json.*
import org.junit.Assert.*
import org.junit.Test

class DatedTransferTest {
    @Test fun datedSnapshotAndCollectionRoundTrip() {
        val table = Timetable(cycle = WeekCycle(2, "2026-10-12"), subjects = listOf(Subject("s", "Work")),
            sessions = listOf(Session("a", "s", 1, TimeSpan(540, 600), cycleWeek = 2)),
            holidays = listOf(Holiday("h", "Winter", "2026-12-21", "2027-01-03")),
            exceptions = listOf(DatedException("2026-10-13", sessions = listOf(Session("e", "s", 2, TimeSpan(720, 800))))))
        assertEquals(table, TimetableTransfer.decode(TimetableTransfer.encode(table)))
        val collection = TimetableCollection().add("dated", table)
        assertEquals(collection, TimetableCollectionCodec.decode(TimetableCollectionCodec.encode(collection)))
    }
    @Test fun realVersionTwoPayloadWithoutNewFieldsMigratesInCollection() {
        val root = Json.parseToJsonElement(TimetableCodec.encode(Timetable())).jsonObject
        val old = JsonObject(root.filterKeys { it !in setOf("cycle", "holidays", "exceptions") }.toMutableMap().apply { put("schemaVersion", JsonPrimitive(2)) })
        val expected = Timetable()
        assertEquals(expected, TimetableCodec.decode(old.toString()))
        val library = """{"format":"cc.xdan.tempo.library","formatVersion":1,"activeId":"old","timetables":[{"id":"old","timetable":$old}]}"""
        assertEquals(expected, TimetableCollectionCodec.decode(library).active)
    }
    @Test(expected = java.time.format.DateTimeParseException::class) fun invalidDatesFailBeforeImport() {
        TimetableTransfer.decode(TimetableTransfer.encode(Timetable()).replace("2026-01-05", "bad-date"))
    }
}
