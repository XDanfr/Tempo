package cc.xdan.tempo

import android.graphics.Bitmap
import android.view.KeyEvent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import cc.xdan.tempo.design.TempoTheme
import cc.xdan.tempo.design.subjectColours
import cc.xdan.tempo.model.*
import cc.xdan.tempo.ui.*
import java.io.File
import java.time.LocalDate
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class Polish051Test {
    @get:Rule val compose = createComposeRule()

    private fun screenshot(name: String) {
        compose.waitForIdle()
        compose.mainClock.advanceTimeBy(500)
        compose.waitForIdle()
        val bitmap = InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot()
        File(InstrumentationRegistry.getInstrumentation().targetContext.filesDir, "qa-051-$name.png").outputStream().use {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
        }
    }

    @Test fun newThemesCanBeSelectedInLightDarkAndSystemModes() {
        var appearance by mutableStateOf(Appearance())
        compose.setContent { TempoTheme(appearance) { Surface { Column(Modifier.padding(20.dp)) {
            ThemeSelector(appearance) { appearance = it }
        } } } }
        for (mode in ThemeMode.entries) {
            compose.onNodeWithText(mode.name.lowercase().replaceFirstChar { it.titlecase() }).performClick()
            for (preset in listOf(ThemePreset.RED, ThemePreset.VIOLET, ThemePreset.ROSE)) {
                compose.onNodeWithTag("theme-presets").performScrollToIndex(ThemePreset.entries.indexOf(preset))
                compose.onNodeWithText(preset.label()).performClick()
                compose.runOnIdle { assertEquals(preset, appearance.selectedPreset); assertEquals(mode, appearance.mode) }
                compose.onNodeWithText("Selected").assertIsDisplayed()
                screenshot("${preset.name.lowercase()}-${mode.name.lowercase()}")
            }
        }
    }

    @Test fun allNamedColoursAreSelectableAndRedSavesWithASuggestedIcon() {
        var saved: Subject? = null
        compose.setContent { TempoTheme(Appearance(mode = ThemeMode.LIGHT)) {
            SubjectEditor(null, 0, {}, { saved = it }, {})
        } }
        compose.onNodeWithText("Name").performTextReplacement("Maths")
        for (colour in subjectColours) {
            compose.onNodeWithContentDescription("${colour.name} subject colour").performScrollTo().performClick().assertIsSelected()
        }
        val blue = compose.onNodeWithContentDescription("Blue subject colour").performScrollTo()
        InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(KeyEvent.KEYCODE_TAB)
        compose.waitForIdle()
        blue.performSemanticsAction(SemanticsActions.RequestFocus) { it() }
        blue.assertIsFocused()
        InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(KeyEvent.KEYCODE_ENTER)
        compose.waitForIdle()
        blue.assertIsSelected()
        blue.assertIsFocused()
        compose.onNodeWithContentDescription("Red subject colour").performScrollTo().performClick().assertIsSelected()
        screenshot("subject-red-light")
        compose.onNodeWithText("Save").performClick()
        compose.waitUntil(10_000) { saved != null }
        assertEquals(0xFFE57373L, saved!!.colour)
        assertEquals(SubjectIcon.MATHS, saved!!.icon)
    }

    @Test fun explicitNoneSurvivesRecognisedNameChanges() {
        var saved: Subject? = null
        compose.setContent { TempoTheme(Appearance()) { SubjectEditor(null, 0, {}, { saved = it }, {}) } }
        compose.onNodeWithText("Name").performTextReplacement("Maths")
        compose.onNodeWithText("Maths · Choose icon").performScrollTo().performClick()
        compose.waitUntil(10_000) { compose.onAllNodesWithText("None").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("None").performScrollTo().performClick()
        compose.onNodeWithText("Use icon").performClick()
        compose.onNodeWithText("Name").performScrollTo().performTextReplacement("Physics")
        compose.onNodeWithText("Name").performImeAction()
        compose.onNodeWithText("None · Choose icon").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Save").performClick()
        compose.waitUntil(10_000) { saved != null }
        assertEquals(SubjectIcon.NONE, saved!!.icon)
    }

    @Test fun editingPreservesSavedCustomColourIconAndIdentity() {
        val original = Subject("custom", "Maths", 0xFF123456, SubjectIcon.MUSIC)
        var saved: Subject? = null
        compose.setContent { TempoTheme(Appearance()) { SubjectEditor(original, 0, {}, { saved = it }, {}) } }
        compose.onNodeWithText("Name").performTextReplacement("Biology")
        val forest = compose.onNodeWithContentDescription("Forest subject colour").performScrollTo()
        InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(KeyEvent.KEYCODE_TAB)
        compose.waitForIdle()
        forest.performSemanticsAction(SemanticsActions.RequestFocus) { it() }
        compose.onNodeWithText("Saved custom colour").performScrollTo().assertIsDisplayed()
        screenshot("subject-custom-dark")
        compose.onNodeWithText("Save").performClick()
        compose.waitUntil(10_000) { saved != null }
        assertEquals(original.copy(name = "Biology"), saved)
    }

    @Test fun dateFieldShowsAFriendlyDateAndKeepsIsoOnSelection() {
        var value by mutableStateOf("2026-10-11")
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        compose.setContent { TempoTheme(Appearance()) { DateField("Date", value) { value = it } } }
        compose.onNodeWithText(displayDate(LocalDate.parse(value), context.resources.configuration.locales[0])).assertIsDisplayed().performClick()
        compose.onNodeWithText("Choose date").performClick()
        compose.runOnIdle { assertEquals("2026-10-11", value) }
    }
}
