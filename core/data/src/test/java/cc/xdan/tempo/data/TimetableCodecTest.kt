package cc.xdan.tempo.data

import cc.xdan.tempo.model.*
import org.junit.Assert.*
import org.junit.Test

class TimetableCodecTest {
    @Test fun roundTripPreservesEverything() {
        val original = demoTimetable(Appearance(ThemePreset.OCEAN, ThemeMode.LIGHT, showIcons = false))
        assertEquals(original, TimetableCodec.decode(TimetableCodec.encode(original)))
    }
    @Test fun emptyOnboardingRoundTrip() { assertEquals(Timetable(), TimetableCodec.decode(TimetableCodec.encode(Timetable()))) }
    @Test(expected = IllegalArgumentException::class) fun futureVersionRejected() {
        TimetableCodec.decode(TimetableCodec.encode(Timetable()).replace("\"schemaVersion\":1", "\"schemaVersion\":99"))
    }
    @Test(expected = IllegalArgumentException::class) fun danglingSubjectRejected() {
        Timetable(subjects = emptyList(), sessions = listOf(Session("s", "missing", 1, TimeSpan(540, 600))))
    }
}
