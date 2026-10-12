package cc.xdan.tempo

import android.graphics.Bitmap
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import cc.xdan.tempo.design.TempoTheme
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

    @Test fun widgetCustomThemeOpensAPopupAndCancelPreservesFollowing() {
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
        compose.onNodeWithText("Cancel").performClick()
        compose.waitUntil(10_000) { compose.onAllNodesWithText("Widget theme").fetchSemanticsNodes().isEmpty() }
        compose.runOnIdle { assertNull(preset); assertEquals(ThemeMode.DARK, mode) }
        compose.onNodeWithText("Custom theme").performClick()
        compose.onNodeWithText("Ocean").assertIsSelected()
        compose.onNodeWithText("Red").performScrollTo().performClick()
        compose.onNodeWithText("System").performScrollTo().performClick()
        compose.onNodeWithText("Use theme").performClick()
        compose.waitUntil(10_000) { preset == ThemePreset.RED }
        compose.runOnIdle { assertEquals(ThemeMode.SYSTEM, mode) }
        compose.onNodeWithText("Red · System").assertIsDisplayed()
        compose.onNodeWithText("Custom theme").performClick()
        compose.onNodeWithText("Widget theme").assertIsDisplayed()
        compose.onNodeWithContentDescription("Close editor").performClick()
        compose.waitUntil(10_000) { compose.onAllNodesWithText("Widget theme").fetchSemanticsNodes().isEmpty() }
        compose.runOnIdle { assertEquals(ThemePreset.RED, preset) }
        compose.onNodeWithText("Follow timetable").performClick()
        compose.runOnIdle { assertNull(preset) }
    }
}
