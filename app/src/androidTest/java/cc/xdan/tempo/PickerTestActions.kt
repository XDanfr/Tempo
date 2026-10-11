package cc.xdan.tempo

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import java.time.LocalDate
import java.time.temporal.ChronoUnit

fun ComposeContentTestRule.chooseDate(label: String, initial: LocalDate, date: LocalDate) {
    onNodeWithText(label).performScrollTo().performClick()
    val months = ChronoUnit.MONTHS.between(initial.withDayOfMonth(1), date.withDayOfMonth(1)).toInt()
    repeat(kotlin.math.abs(months)) {
        onNode(hasContentDescription(if (months > 0) "next month" else "previous month", substring = true, ignoreCase = true)).performClick()
    }
    onNode(hasText(date.dayOfMonth.toString()) and hasClickAction()).performClick()
    onNodeWithText("Choose date").performClick()
}
