package cc.xdan.tempo.design

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import cc.xdan.tempo.model.*

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun TempoTheme(appearance: Appearance, content: @Composable () -> Unit) {
    val dark = when (appearance.mode) { ThemeMode.DARK -> true; ThemeMode.LIGHT -> false; ThemeMode.SYSTEM -> isSystemInDarkTheme() }
    val context = LocalContext.current
    val scheme = if (appearance.selectedPreset == ThemePreset.MATERIAL_YOU && Build.VERSION.SDK_INT >= 31) {
        if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    } else presetColorScheme(appearance.selectedPreset, dark)
    MaterialExpressiveTheme(
        colorScheme = scheme,
        typography = TempoTypography,
        shapes = Shapes(
            extraSmall = RoundedCornerShape(12.dp), small = RoundedCornerShape(16.dp),
            medium = RoundedCornerShape(24.dp), large = RoundedCornerShape(28.dp), extraLarge = RoundedCornerShape(32.dp),
        ), content = content,
    )
}

/** Every accent and surface role belongs to the selected preset, including fixed roles. */
fun presetColorScheme(preset: ThemePreset, dark: Boolean): ColorScheme {
    val (seed, ink, complement) = when (preset) {
        ThemePreset.FOREST, ThemePreset.MATERIAL_YOU -> Triple(Color(0xFFB7F397), Color(0xFF386A25), Color(0xFFACE0D3))
        ThemePreset.OCEAN -> Triple(Color(0xFFA2DFFF), Color(0xFF00658A), Color(0xFF9DE3CF))
        ThemePreset.AMBER -> Triple(Color(0xFFFFD59B), Color(0xFF805600), Color(0xFFFFBEA5))
    }
    val neutral = lerp(seed, Color(0xFFBFC6BE), .75f)
    val darkText = lerp(Color.Black, seed, .12f)
    val lightText = lerp(Color.White, seed, .10f)
    fun darkTone(fraction: Float) = lerp(Color.Black, neutral, fraction)
    fun lightTone(fraction: Float) = lerp(Color.White, neutral, fraction)
    val secondary = if (dark) neutral else lerp(Color.Black, neutral, .42f)
    val tertiary = if (dark) complement else lerp(Color.Black, complement, .40f)
    val base = if (dark) darkColorScheme() else lightColorScheme()
    return base.copy(
        primary = if (dark) seed else ink, onPrimary = if (dark) darkText else Color.White,
        primaryContainer = if (dark) lerp(Color.Black, seed, .27f) else seed,
        onPrimaryContainer = if (dark) seed else darkText,
        secondary = secondary, onSecondary = if (dark) darkText else Color.White,
        secondaryContainer = if (dark) darkTone(.27f) else lightTone(.48f),
        onSecondaryContainer = if (dark) lightText else darkText,
        tertiary = tertiary, onTertiary = if (dark) darkText else Color.White,
        tertiaryContainer = if (dark) lerp(Color.Black, complement, .27f) else complement,
        onTertiaryContainer = if (dark) complement else darkText,
        inversePrimary = if (dark) ink else seed, surfaceTint = if (dark) seed else ink,
        background = if (dark) darkTone(.085f) else lightTone(.12f),
        onBackground = if (dark) lightText else darkText,
        surface = if (dark) darkTone(.12f) else lightTone(.12f),
        onSurface = if (dark) lightText else darkText,
        surfaceVariant = if (dark) darkTone(.23f) else lightTone(.55f),
        onSurfaceVariant = if (dark) neutral else darkTone(.36f),
        outline = if (dark) darkTone(.65f) else darkTone(.52f),
        outlineVariant = if (dark) darkTone(.30f) else lightTone(.75f),
        inverseSurface = if (dark) lightText else darkText,
        inverseOnSurface = if (dark) darkText else lightText,
        surfaceBright = if (dark) darkTone(.25f) else lightTone(.08f),
        surfaceDim = if (dark) darkTone(.085f) else lightTone(.70f),
        surfaceContainerLowest = if (dark) darkTone(.06f) else Color.White,
        surfaceContainerLow = if (dark) darkTone(.13f) else lightTone(.18f),
        surfaceContainer = if (dark) darkTone(.16f) else lightTone(.28f),
        surfaceContainerHigh = if (dark) darkTone(.20f) else lightTone(.38f),
        surfaceContainerHighest = if (dark) darkTone(.25f) else lightTone(.48f),
        primaryFixed = seed, primaryFixedDim = lerp(seed, ink, .20f),
        onPrimaryFixed = darkText, onPrimaryFixedVariant = ink,
        secondaryFixed = neutral, secondaryFixedDim = lerp(neutral, Color.Black, .10f),
        onSecondaryFixed = darkText, onSecondaryFixedVariant = darkTone(.36f),
        tertiaryFixed = complement, tertiaryFixedDim = lerp(complement, Color.Black, .10f),
        onTertiaryFixed = darkText, onTertiaryFixedVariant = lerp(Color.Black, complement, .36f),
    )
}
