package cc.xdan.tempo.model

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

fun displayDate(date: LocalDate, locale: Locale = Locale.getDefault()): String {
    if (locale.language != "en") return date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG).withLocale(locale))
    val day = date.dayOfMonth
    val suffix = if (day % 100 in 11..13) "th" else when (day % 10) { 1 -> "st"; 2 -> "nd"; 3 -> "rd"; else -> "th" }
    return "$day$suffix ${date.format(DateTimeFormatter.ofPattern("MMMM uuuu", locale))}"
}
