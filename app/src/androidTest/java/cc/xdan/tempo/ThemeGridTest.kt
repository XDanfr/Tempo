package cc.xdan.tempo

import android.graphics.Bitmap
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import cc.xdan.tempo.design.TempoTheme
import cc.xdan.tempo.design.presetColorScheme
import cc.xdan.tempo.model.*
import cc.xdan.tempo.ui.ThemeSelector
import cc.xdan.tempo.widgets.WidgetThemeControl
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class ThemeGridTest {
    @get:Rule val compose = createComposeRule()

    private fun screenshot(name: String) {
        compose.waitForIdle()
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val bitmap = instrumentation.uiAutomation.takeScreenshot()
        File(instrumentation.targetContext.filesDir, "qa-grid-$name.png").outputStream().use {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
        }
    }

    @Test fun changingThemesAnimatesTheirColourScheme() {
        compose.mainClock.autoAdvance = false
        var appearance by mutableStateOf(Appearance(preset = ThemePreset.FOREST, mode = ThemeMode.DARK))
        var rendered by mutableStateOf(Color.Unspecified)
        compose.setContent {
            TempoTheme(appearance) {
                val background = MaterialTheme.colorScheme.background
                SideEffect { rendered = background }
            }
        }
        val initial = presetColorScheme(ThemePreset.FOREST, true).background
        val target = presetColorScheme(ThemePreset.OCEAN, true).background
        compose.runOnIdle { assertEquals(initial, rendered); appearance = appearance.copy(preset = ThemePreset.OCEAN) }
        compose.mainClock.advanceTimeBy(80)
        compose.runOnIdle {
            assertNotEquals(initial, rendered)
            assertNotEquals(target, rendered)
        }
        compose.mainClock.advanceTimeBy(1000)
        compose.runOnIdle { assertEquals(target, rendered) }
    }

    @Test fun forestAndWallpaperShareTheFirstRowAndOtherPresetsWrap() {
        compose.setContent { TempoTheme(Appearance()) { Surface {
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp)) {
                ThemeSelector(Appearance()) {}
            }
        } } }
        val forest = compose.onNodeWithText("Forest").fetchSemanticsNode().boundsInRoot
        val wallpaper = compose.onNodeWithText("Material You").fetchSemanticsNode().boundsInRoot
        val ocean = compose.onNodeWithText("Ocean").fetchSemanticsNode().boundsInRoot
        assertEquals(forest.top, wallpaper.top, 1f)
        assertTrue(wallpaper.left > forest.right)
        assertTrue(ocean.top > forest.bottom)
        screenshot("themes")
    }

    @Test fun widgetCustomThemeAppliesSelectionsImmediatelyAndKeepsThemOnClose() {
        var preset by mutableStateOf<ThemePreset?>(null)
        var mode by mutableStateOf(ThemeMode.DARK)
        compose.setContent { TempoTheme(Appearance()) { Surface {
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp)) {
                WidgetThemeControl(preset, mode, Appearance(preset = ThemePreset.OCEAN, mode = ThemeMode.LIGHT)) { p, m -> preset = p; mode = m }
            }
        } } }
        compose.onNodeWithText("Forest").assertDoesNotExist()
        compose.onNodeWithText("Custom theme").performClick()
        compose.onNodeWithText("Widget theme").assertIsDisplayed()
        screenshot("widget-popup")
        compose.onNodeWithText("Red").performScrollTo().performClick()
        compose.runOnIdle { assertEquals(ThemePreset.RED, preset); assertEquals(ThemeMode.LIGHT, mode) }
        compose.onNodeWithText("Red · Light").assertIsDisplayed()
        compose.onNodeWithText("System").performScrollTo().performClick()
        compose.runOnIdle { assertEquals(ThemeMode.SYSTEM, mode) }
        compose.onNodeWithText("Red · System").assertIsDisplayed()
        compose.onNodeWithContentDescription("Close editor").performClick()
        compose.waitUntil(10_000) { compose.onAllNodesWithText("Widget theme").fetchSemanticsNodes().isEmpty() }
        compose.runOnIdle { assertEquals(ThemePreset.RED, preset) }
        compose.onNodeWithText("Follow timetable").performClick()
        compose.runOnIdle { assertNull(preset) }
    }
}
