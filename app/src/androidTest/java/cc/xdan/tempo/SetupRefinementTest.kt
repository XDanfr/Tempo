package cc.xdan.tempo

import androidx.compose.runtime.*
import androidx.compose.material3.Surface
import androidx.activity.ComponentActivity
import androidx.activity.BackEventCompat
import androidx.compose.ui.graphics.asAndroidBitmap
import android.graphics.Bitmap
import java.io.File
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import cc.xdan.tempo.design.TempoTheme
import cc.xdan.tempo.model.*
import cc.xdan.tempo.ui.*
import cc.xdan.tempo.widgets.WidgetSettings
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SetupRefinementTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    @Test fun predictiveSettingsBackCanCancelOrCommit() {
        val table = Timetable(onboarded = true)
        compose.setContent { TempoTheme(table.appearance) { SettingsScreen(table, {}, TimetableFileActions({}, {}), {}, false) } }
        compose.onNodeWithText("Appearance").performClick()
        compose.onNodeWithText("Subject blocks").performScrollTo().assertIsDisplayed()
        compose.runOnIdle { compose.activity.onBackPressedDispatcher.dispatchOnBackStarted(BackEventCompat(0f, 100f, 0f, BackEventCompat.EDGE_LEFT)) }
        compose.runOnIdle { compose.activity.onBackPressedDispatcher.dispatchOnBackProgressed(BackEventCompat(100f, 100f, .6f, BackEventCompat.EDGE_LEFT)) }
        compose.runOnIdle { compose.activity.onBackPressedDispatcher.dispatchOnBackCancelled() }
        compose.onNodeWithText("Subject blocks").assertIsDisplayed()
        compose.runOnIdle { compose.activity.onBackPressedDispatcher.dispatchOnBackStarted(BackEventCompat(0f, 100f, 0f, BackEventCompat.EDGE_LEFT)) }
        compose.runOnIdle { compose.activity.onBackPressedDispatcher.dispatchOnBackProgressed(BackEventCompat(200f, 100f, .8f, BackEventCompat.EDGE_LEFT)) }
        compose.runOnIdle { compose.activity.onBackPressedDispatcher.onBackPressed() }
        compose.onNodeWithText("Your Tempo").assertIsDisplayed()
    }

    @Test fun onboardingEditsTheExistingSubjectAndKeepsItsIdentity() {
        var value by mutableStateOf(Timetable(onboardingStep = 2, subjects = listOf(Subject("maths", "Maths", location = "Room 5"))))
        compose.setContent { TempoTheme(value.appearance) { Surface { Onboarding(value, { value = it(value) }, {}) } } }
        compose.onNodeWithText("Room 5").performScrollTo().assertIsDisplayed()
        val image = compose.onRoot().captureToImage().asAndroidBitmap()
        File(InstrumentationRegistry.getInstrumentation().targetContext.filesDir, "qa-onboarding-subjects.png").outputStream().use { image.compress(Bitmap.CompressFormat.PNG, 100, it) }
        compose.onNodeWithText("Maths").performClick()
        compose.onNodeWithText("Name").performTextReplacement("Mathematics")
        compose.onNodeWithText("Save").performClick()
        compose.waitUntil(10_000) { value.subjects.single().name == "Mathematics" }
        assertEquals("maths", value.subjects.single().id)
        assertEquals("Room 5", value.subjects.single().location)
    }

    @Test fun onboardingPersistsCustomCycleBeforeProceeding() {
        var value by mutableStateOf(Timetable())
        compose.setContent { TempoTheme(value.appearance) { Surface { Onboarding(value, { value = it(value) }, {}) } } }
        compose.onNodeWithText("Number of weeks").performScrollTo().performTextReplacement("0")
        compose.onNodeWithText("Continue").assertIsNotEnabled()
        compose.onNodeWithText("Number of weeks").performTextReplacement("3")
        compose.onNodeWithText("Continue").performClick()
        compose.waitUntil(10_000) { value.onboardingStep == 1 }
        assertEquals(3, value.cycle.weeks)
    }

    @Test fun compactEditorKeepsActionsVisibleWithoutFillingTheScreen() {
        var saved = false
        compose.setContent { TempoTheme(Appearance()) {
            EditorSheet("Monday hours", {}, confirm = { saved = true }, compact = true) { TimeFields("09:00", "16:30", {}, {}) }
        } }
        compose.onNodeWithText("09:00").assertIsDisplayed()
        val clockText = compose.onNodeWithText("09:00").fetchSemanticsNode().boundsInRoot
        val endText = compose.onNodeWithText("16:30").fetchSemanticsNode().boundsInRoot
        assertTrue("Each narrow clock value needs a single readable line", clockText.width > clockText.height && endText.width > endText.height)
        val panel = compose.onNodeWithTag("editor-panel").fetchSemanticsNode().boundsInRoot
        val screenHeight = InstrumentationRegistry.getInstrumentation().targetContext.resources.displayMetrics.heightPixels
        assertTrue("Short editor should wrap its content", panel.height < screenHeight * .75f)
        compose.onNodeWithText("Save").assertIsDisplayed().performClick()
        assertTrue(saved)
    }

    @Test fun widgetLocationPreferencesMigrateRoundTripAndDelete() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val id = 995051
        val preferences = context.getSharedPreferences("widgets", 0)
        try {
            preferences.edit().putBoolean("$id.location", false).commit()
            val old = WidgetSettings.read(context, id)
            assertFalse(old.followLocations)
            assertFalse(old.showsLocations(Appearance()))
            old.copy(followLocations = true).save(context, id)
            val follow = WidgetSettings.read(context, id)
            assertTrue(follow.showsLocations(Appearance(showLocations = true)))
            assertFalse(follow.showsLocations(Appearance(showLocations = false)))
            WidgetSettings.delete(context, id)
            assertFalse(preferences.contains("$id.followLocations"))
            assertFalse(preferences.contains("$id.location"))
        } finally { WidgetSettings.delete(context, id) }
    }
}
