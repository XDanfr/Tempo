package cc.xdan.tempo.widgets

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import cc.xdan.tempo.design.presetColorScheme
import cc.xdan.tempo.model.ThemePreset
import org.junit.Assert.*
import org.junit.Test

class WidgetColoursTest {
    private fun contrast(a: Color, b: Color): Float =
        (maxOf(a.luminance(), b.luminance()) + .05f) / (minOf(a.luminance(), b.luminance()) + .05f)

    @Test fun themeFillsRespectModeAndKeepReadableText() {
        for (preset in ThemePreset.entries) for (dark in listOf(false, true)) {
            val background = widgetThemeBackground(presetColorScheme(preset, dark), dark)
            assertTrue("$preset/$dark keeps a theme tint", background.luminance() > .25f)
            assertTrue(contrast(background, widgetForeground(background)) >= 4.5f)
        }
    }
    @Test fun customSubjectFillsKeepReadableTextAcrossTheLuminanceRange() {
        for (step in 0..255) {
            val colour = Color(0xFF000000.toInt() or (step shl 16) or (step shl 8) or step)
            assertTrue("Grey $step", contrast(colour, widgetForeground(colour)) >= 4.5f)
        }
        for (colour in listOf(Color(0xFFB7F397), Color(0xFFA2DFFF), Color(0xFFFFD59B), Color(0xFFD3BCFF), Color(0xFFFFB6C6))) {
            assertTrue(contrast(colour, widgetForeground(colour)) >= 4.5f)
        }
    }
    @Test fun allSizesShareTheEstablishedPillPaletteAndCompletionUsesTheTheme() {
        for (preset in ThemePreset.entries) for (dark in listOf(false, true)) {
            val scheme = presetColorScheme(preset, dark)
            val background = widgetThemeBackground(scheme, dark)
            assertEquals(androidx.compose.ui.graphics.lerp(scheme.surfaceContainerHigh, scheme.primaryFixed, if (dark) .70f else .82f), background)
            val completion = widgetCompletionBadge(scheme, dark)
            assertTrue(contrast(completion, widgetForeground(completion)) >= 4.5f)
        }
    }
    @Test fun themeLessonPanelsStayDistinctFromTheRootAndFromEachOther() {
        for (preset in ThemePreset.entries) for (dark in listOf(false, true)) {
            val scheme = presetColorScheme(preset, dark)
            val root = widgetThemeBackground(scheme, dark)
            val primary = widgetLessonBackground(scheme, Color.Magenta, dark, true, true)
            val secondary = widgetLessonBackground(scheme, Color.Cyan, dark, true, false)
            assertTrue(primary.luminance() < root.luminance())
            assertTrue(secondary.luminance() < primary.luminance())
            assertTrue(contrast(primary, widgetForeground(primary)) >= 4.5f)
            assertTrue(contrast(secondary, widgetForeground(secondary)) >= 4.5f)
            assertEquals(Color.Magenta, widgetLessonBackground(scheme, Color.Magenta, dark, false, true))
            assertEquals(scheme.primary, widgetOutline(scheme))
        }
    }

}
