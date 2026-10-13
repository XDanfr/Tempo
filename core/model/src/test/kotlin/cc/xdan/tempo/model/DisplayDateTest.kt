package cc.xdan.tempo.model

import java.time.LocalDate
import java.util.Locale
import org.junit.Assert.*
import org.junit.Test

class DisplayDateTest {
    @Test fun englishOrdinalsCoverTheWholeMonthIncludingTeens() {
        val suffixes = listOf("st", "nd", "rd", "th", "th", "th", "th", "th", "th", "th", "th", "th", "th", "th", "th", "th", "th", "th", "th", "th", "st", "nd", "rd", "th", "th", "th", "th", "th", "th", "th", "st")
        for (day in 1..31) assertEquals("$day${suffixes[day - 1]} October 2026", displayDate(LocalDate.of(2026, 10, day), Locale.UK))
    }
    @Test fun otherLanguagesUseTheirLocalDateConvention() {
        assertEquals("11 octobre 2026", displayDate(LocalDate.of(2026, 10, 11), Locale.FRANCE))
        assertEquals("29th February 2024", displayDate(LocalDate.of(2024, 2, 29), Locale.UK))
        assertEquals("2024-02-29", LocalDate.of(2024, 2, 29).toString())
    }
}
