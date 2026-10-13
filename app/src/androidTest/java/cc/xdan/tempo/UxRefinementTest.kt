package cc.xdan.tempo

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsActions
import android.graphics.Bitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.core.graphics.ColorUtils
import androidx.lifecycle.ViewModelStore
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import cc.xdan.tempo.data.TimetableRepository
import cc.xdan.tempo.design.TempoTheme
import cc.xdan.tempo.design.presetColorScheme
import cc.xdan.tempo.model.*
import cc.xdan.tempo.schedule.*
import cc.xdan.tempo.ui.*
import kotlinx.coroutines.runBlocking
import java.time.LocalDate
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class UxRefinementTest {
    @get:Rule val compose = createComposeRule()
    private val date = LocalDate.now()
    private val table = Timetable(onboarded = true, subjects = listOf(Subject("s", "Maths")),
        sessions = listOf(Session("a", "s", date.dayOfWeek.value, TimeSpan(540, 660))))

    @Test fun contentFlingsNeverMoveTheEditorPanelOrItsSaveAction() {
        var saved = false
        compose.setContent { TempoTheme(table.appearance) {
            EditorSheet("Scrolling regression", {}, confirm = { saved = true }) {
                repeat(40) { Text("Row $it", Modifier.fillMaxWidth()) }
            }
        } }
        val initial = compose.onNodeWithTag("editor-panel").fetchSemanticsNode().boundsInRoot
        repeat(8) { compose.onNodeWithTag("editor-content").performTouchInput { swipeUp(durationMillis = 100) } }
        compose.onNodeWithText("Row 39").assertIsDisplayed()
        compose.onNodeWithText("Save").assertIsDisplayed()
        compose.mainClock.advanceTimeBy(2000)
        val settled = compose.onNodeWithTag("editor-panel").fetchSemanticsNode().boundsInRoot
        assertEquals(initial.top, settled.top, 1f)
        assertEquals(initial.bottom, settled.bottom, 1f)
        compose.onNodeWithText("Save").performClick()
        compose.waitUntil(10_000) { saved }
    }

    @Test fun settingsPagesAndCustomCycleRetainRealActions() {
        var value by mutableStateOf(table)
        compose.setContent { TempoTheme(value.appearance) {
            SettingsScreen(value, { value = it(value) }, TimetableFileActions({}, {}), {}, false)
        } }
        compose.onNodeWithText("Schedule").performClick()
        compose.onNodeWithText("Edit week cycle").performScrollTo().performClick()
        compose.onNodeWithText("4 weeks").performClick()
        compose.onNodeWithText("Number of weeks").performTextReplacement("3")
        compose.onNodeWithText("Number of weeks").performImeAction()
        compose.onNodeWithText("Save").performClick()
        compose.waitUntil(10_000) { value.cycle.weeks == 3 }
        compose.onNodeWithContentDescription("Back to settings").performScrollTo().performClick()
        compose.onNodeWithText("Timetables & files").performClick()
        compose.onNodeWithText("Export day / week PNG").assertIsDisplayed()
        compose.onNodeWithContentDescription("Back to settings").performScrollTo().performClick()
        compose.onNodeWithText("Appearance").performClick()
        compose.onNodeWithText("Light").performScrollTo().performClick()
        compose.waitUntil(10_000) { value.appearance.mode == ThemeMode.LIGHT }
    }

    @Test fun editingScopeCreatesADatedCopyAndLeavesRecurringSessionUntouched() {
        val repository = TimetableRepository(InstrumentationRegistry.getInstrumentation().targetContext)
        runBlocking { repository.updateCollection { TimetableCollection(timetables = listOf(SavedTimetable("local", table))) } }
        val model = TempoViewModel(repository)
        val store = ViewModelStore().also { it.put("qa", model) }
        try {
            compose.setContent { TempoApp(model) }
            compose.waitUntil(10_000) { model.state.value.timetable != null }
            compose.onNodeWithContentDescription("Edit only this date").assertDoesNotExist()
            compose.onNodeWithContentDescription("Edit block").performClick()
            compose.onNodeWithText("Applies to").performScrollTo().performClick()
            compose.onNodeWithText("Only ${displayDate(date, InstrumentationRegistry.getInstrumentation().targetContext.resources.configuration.locales[0])}").performClick()
            compose.onNodeWithText("Day").assertDoesNotExist()
            compose.onNodeWithText("Room / location override").performScrollTo().performTextReplacement("Dated room")
            compose.onNodeWithText("Save").performClick()
            compose.waitUntil(10_000) { model.state.value.timetable?.exceptions?.singleOrNull()?.sessions?.singleOrNull()?.location == "Dated room" }
            assertEquals("", model.state.value.timetable!!.sessions.single().location)
            compose.waitUntil(10_000) { compose.onAllNodesWithText("Dated room").fetchSemanticsNodes().isNotEmpty() }
            compose.onNodeWithContentDescription("Edit block").performSemanticsAction(SemanticsActions.OnClick) { it() }
            compose.waitUntil(10_000) { compose.onAllNodesWithText("Applies only to ${displayDate(date, InstrumentationRegistry.getInstrumentation().targetContext.resources.configuration.locales[0])}. This date has its own schedule.").fetchSemanticsNodes().isNotEmpty() }
            compose.onNodeWithText("Day").assertDoesNotExist()
        } finally { compose.runOnIdle { store.clear() } }
    }

    @Test fun clockPickerSupportsEndOfDayAndAllExportPalettesHaveReadableInk() {
        var end by mutableStateOf("10:00")
        compose.setContent { TempoTheme(table.appearance) { TimeField("End time", end, { end = it }, allowEndOfDay = true) } }
        compose.onNodeWithText("End time").performClick()
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val clock = InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot()
        java.io.File(context.filesDir, "qa-clock-picker.png").outputStream().use { clock.compress(Bitmap.CompressFormat.PNG, 100, it) }
        compose.onNodeWithText("End of day (24:00)").performScrollTo().performClick()
        compose.waitUntil(10_000) { end == "24:00" }
        compose.onNodeWithText("End time").performClick()
        compose.onNodeWithText("Choose time").performClick()
        compose.runOnIdle { assertEquals("24:00", end) }
        assertEquals(TimeSpan(540, 1440), validSpan("09:00", end))
        val plan = scheduleImagePlan(table, date, false, 720, ImagePrivacy())
        for (preset in ThemePreset.entries) for (dark in listOf(false, true)) {
            val palette = imagePalette(presetColorScheme(preset, dark))
            val bitmap = renderSchedulePng(context, plan, palette)
            assertEquals(palette.background, bitmap.getPixel(0, 0))
            java.io.File(context.filesDir, "qa-${preset.name.lowercase()}-${if (dark) "dark" else "light"}.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
            for (subject in listOf(0xFF000000.toInt(), 0xFFFFFFFF.toInt()) + cc.xdan.tempo.design.subjectColours.map { it.argb.toInt() }) {
                val fill = ColorUtils.blendARGB(palette.background, subject, .24f)
                assertTrue("$preset dark=$dark subject=$subject", ColorUtils.calculateContrast(palette.ink, fill) >= 4.5)
            }
            bitmap.recycle()
        }
    }
}
