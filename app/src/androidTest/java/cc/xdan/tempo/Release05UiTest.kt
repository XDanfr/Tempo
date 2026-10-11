package cc.xdan.tempo

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import cc.xdan.tempo.design.TempoTheme
import cc.xdan.tempo.model.*
import cc.xdan.tempo.schedule.*
import cc.xdan.tempo.ui.*
import java.time.LocalDate
import java.time.LocalDateTime
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class Release05UiTest {
    @get:Rule val compose = createComposeRule()
    private val date = LocalDate.parse("2026-10-12")
    private val lesson = Session("a", "s", 1, TimeSpan(540, 660), notes = "Private notes")
    private val fixture = Timetable(onboarded = true, subjects = listOf(Subject("s", "Maths", location = "Private room")), sessions = listOf(lesson))

    @Test fun cycleHolidayAndReplacementEditorsSaveRealChanges() {
        var table by mutableStateOf(fixture)
        compose.setContent { TempoTheme(table.appearance) {
            Column(Modifier.verticalScroll(rememberScrollState())) { DatedScheduleSettings(table, { table = it(table) }) }
        } }
        compose.onNodeWithText("Edit week cycle").performScrollTo().performClick()
        compose.onNodeWithText("Number of weeks (1–52)").performTextReplacement("2")
        compose.onNodeWithText("Date in week 1").performTextReplacement(date.toString())
        compose.onNodeWithText("Save").performClick()
        compose.waitUntil { table.cycle.weeks == 2 }
        compose.onNodeWithText("Add holiday").performScrollTo().performClick()
        compose.onNodeWithText("Holiday name").performTextReplacement("Autumn break")
        compose.onNodeWithText("First day (included)").performTextReplacement(date.toString())
        compose.onNodeWithText("Last day (included)").performTextReplacement(date.plusDays(4).toString())
        compose.onNodeWithText("Save").performClick()
        compose.waitUntil { table.holidays.size == 1 }
        assertTrue(ScheduleResolver.resolve(table, date).isEmpty())
        compose.onNodeWithText("Add dated exception").performScrollTo().performClick()
        compose.onNodeWithText("Date").performTextReplacement(date.toString())
        compose.onNodeWithText("Day off").performClick()
        compose.onNodeWithText("Save").performClick()
        compose.waitUntil { table.exceptions.size == 1 }
        assertEquals(listOf("a"), ScheduleResolver.resolve(table, date).filterIsInstance<ScheduleBlock.Lesson>().map { it.session.id })
    }

    @Test fun duplicateThenSplitPreservesTheOriginalAndMetadata() {
        var table by mutableStateOf(fixture)
        var open by mutableStateOf(true)
        compose.setContent { TempoTheme(table.appearance) {
            if (open) SessionEditor(table, table.sessions.first(), 1,
                onDismiss = { open = false }, onSave = { table = table.copy(sessions = table.sessions + it); open = false },
                onDelete = {}, onAddSubject = {}, onSplit = { split -> table = table.copy(sessions = table.sessions.filterNot { it.id == "a" } + split); open = false })
        } }
        compose.onNodeWithText("Duplicate session").performScrollTo().performClick()
        compose.waitUntil { table.sessions.size == 2 }
        assertEquals(lesson, table.sessions.first())
        compose.runOnIdle { open = true }
        compose.onNodeWithText("Split at HH:MM").performScrollTo().performTextReplacement("10:00")
        compose.onNodeWithText("Split session").performScrollTo().performClick()
        compose.waitUntil { table.sessions.size == 3 }
        assertEquals(listOf(60, 60), table.sessions.filter { it.id == "a" || it.time.start == 600 }.map { it.time.minutes })
        assertTrue(table.sessions.all { it.notes == "Private notes" })
    }

    @Test fun dayNavigationUsesDatesAndScopeControl() {
        var selected by mutableStateOf(date)
        var dated by mutableStateOf(false)
        val table = fixture.copy(cycle = WeekCycle(2, date.toString()), sessions = listOf(lesson.copy(cycleWeek = 1)))
        compose.setContent { TempoTheme(table.appearance) {
            DayScreen(table, selected.dayOfWeek.value, LocalDateTime.of(2026, 10, 12, 10, 0), false,
                { selected = selected.plusDays((it - selected.dayOfWeek.value).toLong()) }, {}, { _, _ -> }, {},
                date = selected, selectDate = { selected = it }, dateEditing = dated, setDateEditing = { dated = it })
        } }
        compose.onNodeWithContentDescription("Next week").performClick()
        compose.waitUntil { selected == date.plusWeeks(1) }
        compose.onNodeWithText("2026-10-19 · Week 2 of 2").assertExists()
        compose.onNodeWithContentDescription("Edit only this date").performClick()
        compose.runOnIdle { assertTrue(dated) }
        compose.onNodeWithContentDescription("Previous week").performClick()
        compose.waitUntil { selected == date }
        compose.onNodeWithText("Maths").assertExists()
        saveScreen("qa-timetable.png")
    }

    @Test fun pngPrivacyPreviewRendersAndValidatesDimensions() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val privatePlan = scheduleImagePlan(fixture, date, false, 720, ImagePrivacy(false, false))
        val image = renderSchedulePng(context, privatePlan)
        val visible = renderSchedulePng(context, scheduleImagePlan(fixture, date, false, 720, ImagePrivacy(locations = true, notes = true)))
        assertEquals(720, image.width)
        assertTrue(visible.height > image.height)
        val dayFile = java.io.File(context.filesDir, "qa-day.png")
        dayFile.outputStream().use { assertTrue(image.compress(Bitmap.CompressFormat.PNG, 100, it)) }
        val decoded = BitmapFactory.decodeFile(dayFile.path)
        assertEquals(image.width, decoded.width); assertEquals(image.height, decoded.height)
        val week = renderSchedulePng(context, scheduleImagePlan(fixture, date, true, 1080, ImagePrivacy()))
        java.io.File(context.filesDir, "qa-week.png").outputStream().use { assertTrue(week.compress(Bitmap.CompressFormat.PNG, 100, it)) }
        assertTrue(week.height > image.height)
        compose.setContent { TempoTheme(fixture.appearance) { PngExportSheet(fixture, {}, {}) } }
        compose.onNodeWithText("Locations").performScrollTo().assertIsOff()
        compose.onNodeWithText("Notes").performScrollTo().assertIsOff()
        compose.waitUntil(20_000) { compose.onAllNodesWithContentDescription("Exact PNG export preview").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("Save PNG").performScrollTo().assertIsEnabled()
        saveScreen("qa-preview.png")
        compose.onNodeWithText("Locations").performScrollTo().performClick().assertIsOn()
        compose.onNodeWithText("Locations").performClick().assertIsOff()
        compose.onNodeWithText("Width in pixels (480–2048)").performScrollTo().performTextReplacement("9999")
        compose.onNodeWithText("Save PNG").performScrollTo().assertIsNotEnabled()
    }
    private fun saveScreen(name: String) {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val screenshot = compose.onRoot().captureToImage().asAndroidBitmap()
        java.io.File(context.filesDir, name).outputStream().use { screenshot.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }
}
