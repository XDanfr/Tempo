package cc.xdan.tempo.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import cc.xdan.tempo.design.*
import cc.xdan.tempo.model.ThemePreset
import org.junit.Assert.*
import org.junit.Test

class PaletteTest {
    @Test fun everyPresetHasReadableRolePairsInBothModes() {
        for (preset in ThemePreset.entries) for (dark in listOf(false, true)) {
            val s = presetColorScheme(preset, dark)
            val pairs = listOf(s.primary to s.onPrimary, s.primaryContainer to s.onPrimaryContainer,
                s.secondary to s.onSecondary, s.secondaryContainer to s.onSecondaryContainer,
                s.tertiary to s.onTertiary, s.tertiaryContainer to s.onTertiaryContainer,
                s.surface to s.onSurface, s.surfaceVariant to s.onSurfaceVariant,
                s.background to s.onBackground, s.surfaceContainerHighest to s.onSurface,
                s.surfaceContainerHighest to s.onSurfaceVariant, s.inverseSurface to s.inverseOnSurface,
                s.primaryFixed to s.onPrimaryFixed, s.primaryFixed to s.onPrimaryFixedVariant,
                s.primaryFixedDim to s.onPrimaryFixed, s.primaryFixedDim to s.onPrimaryFixedVariant,
                s.secondaryFixed to s.onSecondaryFixed, s.secondaryFixed to s.onSecondaryFixedVariant,
                s.secondaryFixedDim to s.onSecondaryFixed, s.secondaryFixedDim to s.onSecondaryFixedVariant,
                s.tertiaryFixed to s.onTertiaryFixed, s.tertiaryFixed to s.onTertiaryFixedVariant,
                s.tertiaryFixedDim to s.onTertiaryFixed, s.tertiaryFixedDim to s.onTertiaryFixedVariant,
                s.error to s.onError, s.errorContainer to s.onErrorContainer)
            for ((background, ink) in pairs) assertTrue("$preset dark=$dark: $background / $ink",
                colourContrast(background, ink) >= 4.5f)
        }
    }

    @Test fun subjectsAndImportedColoursStayReadableInCardsComparisonAndOutlines() {
        val colours = subjectColours.map { Color(it.argb) } + (0..255).map { Color(it / 255f, it / 255f, it / 255f) }
        for (colour in colours) {
            assertTrue(colourContrast(colour, subjectForeground(colour)) >= 4.5f)
            for (preset in ThemePreset.entries) for (dark in listOf(false, true)) {
                val s = presetColorScheme(preset, dark)
                val fill = lerp(s.surface, colour, if (dark) .30f else .55f)
                assertTrue(colourContrast(fill, subjectForeground(fill)) >= 4.5f)
                assertTrue(colourContrast(s.surface, subjectOutline(colour, s.surface)) >= 3f)
                assertTrue(colourContrast(fill, subjectOutline(colour, fill)) >= 3f)
            }
        }
    }
}
