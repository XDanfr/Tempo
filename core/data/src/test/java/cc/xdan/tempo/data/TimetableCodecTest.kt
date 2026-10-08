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
        TimetableCodec.decode(TimetableCodec.encode(Timetable()).replace("\"schemaVersion\":2", "\"schemaVersion\":99"))
    }
    @Test fun versionOneMigratesWithoutLosingSessionsOrTheme() {
        val original = demoTimetable(Appearance(dynamicColour = true))
        val legacy = TimetableCodec.encode(original).replace("\"schemaVersion\":2", "\"schemaVersion\":1")
        val migrated = TimetableCodec.decode(legacy)
        assertEquals(original, migrated)
        assertEquals(ThemePreset.MATERIAL_YOU, migrated.appearance.selectedPreset)
    }
    @Test fun interruptedOnboardingMovesToMatchingNewStep() {
        val old = TimetableCodec.encode(Timetable(onboardingStep = 1)).replace("\"schemaVersion\":2", "\"schemaVersion\":1")
        assertEquals(2, TimetableCodec.decode(old).onboardingStep)
    }
    @Test fun breaksAndOutlineStyleRoundTrip() {
        val value = Timetable(breaks = listOf(ScheduledBreak("l", "Lunch", listOf(1, 3), TimeSpan(720, 765), BreakKind.LUNCH)),
            appearance = Appearance(preset = ThemePreset.MATERIAL_YOU, blockStyle = BlockStyle.OUTLINED))
        assertEquals(value, TimetableCodec.decode(TimetableCodec.encode(value)))
    }
    @Test(expected = IllegalArgumentException::class) fun danglingSubjectRejected() {
        Timetable(subjects = emptyList(), sessions = listOf(Session("s", "missing", 1, TimeSpan(540, 600))))
    }
}
