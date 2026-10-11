package cc.xdan.tempo

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import androidx.test.platform.app.InstrumentationRegistry

fun ComposeContentTestRule.chooseDate(label: String, initial: LocalDate, date: LocalDate) {
    onNodeWithText(label).performScrollTo().performClick()
    val months = ChronoUnit.MONTHS.between(initial.withDayOfMonth(1), date.withDayOfMonth(1)).toInt()
    repeat(kotlin.math.abs(months)) {
        onNode(hasContentDescription(if (months > 0) "next month" else "previous month", substring = true, ignoreCase = true)).performClick()
    }
    val locale = InstrumentationRegistry.getInstrumentation().targetContext.resources.configuration.locales[0]
    val description = date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL).withLocale(locale))
    onNode((hasText(description, substring = true) or hasContentDescription(description, substring = true)) and hasClickAction()).performClick()
    onNodeWithText("Choose date").performClick()
}
