package cc.xdan.tempo.design

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.animation.animateColor
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.updateTransition
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
        colorScheme = animateColorScheme(scheme),
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
        ThemePreset.RED -> Triple(Color(0xFFFF3B30), Color(0xFFB3261E), Color(0xFFFFA36C))
        ThemePreset.VIOLET -> Triple(Color(0xFFD3BCFF), Color(0xFF67508D), Color(0xFFF4B8DB))
        ThemePreset.ROSE -> Triple(Color(0xFFFFB6C6), Color(0xFF914660), Color(0xFFD3BCFF))
    }
    val neutral = lerp(seed, Color(0xFFBFC6BE), .45f)
    val darkText = lerp(Color.Black, seed, .12f)
    val lightText = lerp(Color.White, seed, .10f)
    // Dark surfaces keep a visible trace of the selected hue instead of collapsing to grey.
    fun darkTone(fraction: Float) = lerp(Color.Black, seed, fraction)
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
        background = if (dark) darkTone(.16f) else lightTone(.12f),
        onBackground = if (dark) lightText else darkText,
        surface = if (dark) darkTone(.16f) else lightTone(.12f),
        onSurface = if (dark) lightText else darkText,
        surfaceVariant = if (dark) darkTone(.23f) else lightTone(.55f),
        onSurfaceVariant = if (dark) neutral else darkTone(.36f),
        outline = if (dark) darkTone(.65f) else darkTone(.52f),
        outlineVariant = if (dark) darkTone(.30f) else lightTone(.75f),
        inverseSurface = if (dark) lightText else darkText,
        inverseOnSurface = if (dark) darkText else lightText,
        surfaceBright = if (dark) darkTone(.25f) else lightTone(.08f),
        surfaceDim = if (dark) darkTone(.12f) else lightTone(.70f),
        surfaceContainerLowest = if (dark) darkTone(.06f) else Color.White,
        surfaceContainerLow = if (dark) darkTone(.17f) else lightTone(.18f),
        surfaceContainer = if (dark) darkTone(.20f) else lightTone(.28f),
        surfaceContainerHigh = if (dark) darkTone(.24f) else lightTone(.38f),
        surfaceContainerHighest = if (dark) darkTone(.29f) else lightTone(.48f),
        primaryFixed = seed, primaryFixedDim = lerp(seed, ink, .20f),
        onPrimaryFixed = if (preset == ThemePreset.RED) Color.Black else darkText,
        onPrimaryFixedVariant = if (preset == ThemePreset.RED) Color.Black else lerp(ink, Color.Black, .35f),
        secondaryFixed = neutral, secondaryFixedDim = lerp(neutral, Color.Black, .10f),
        onSecondaryFixed = darkText, onSecondaryFixedVariant = darkTone(.36f),
        tertiaryFixed = complement, tertiaryFixedDim = lerp(complement, Color.Black, .10f),
        onTertiaryFixed = darkText, onTertiaryFixedVariant = lerp(Color.Black, complement, .36f),
    )
}

@Composable
private fun animateColorScheme(target: ColorScheme): ColorScheme {
    val transition = updateTransition(targetState = target, label = "Tempo theme")
    @Composable fun color(label: String, value: @Composable (ColorScheme) -> Color) =
        transition.animateColor(
            transitionSpec = { spring(dampingRatio = 1f, stiffness = 1600f) },
            label = label,
            targetValueByState = value,
        ).value
    return target.copy(
        primary = color("primary") { it.primary }, onPrimary = color("on primary") { it.onPrimary },
        primaryContainer = color("primary container") { it.primaryContainer }, onPrimaryContainer = color("on primary container") { it.onPrimaryContainer },
        inversePrimary = color("inverse primary") { it.inversePrimary },
        error = color("error") { it.error }, onError = color("on error") { it.onError },
        errorContainer = color("error container") { it.errorContainer }, onErrorContainer = color("on error container") { it.onErrorContainer },
        secondary = color("secondary") { it.secondary }, onSecondary = color("on secondary") { it.onSecondary },
        secondaryContainer = color("secondary container") { it.secondaryContainer }, onSecondaryContainer = color("on secondary container") { it.onSecondaryContainer },
        tertiary = color("tertiary") { it.tertiary }, onTertiary = color("on tertiary") { it.onTertiary },
        tertiaryContainer = color("tertiary container") { it.tertiaryContainer }, onTertiaryContainer = color("on tertiary container") { it.onTertiaryContainer },
        background = color("background") { it.background }, onBackground = color("on background") { it.onBackground },
        surface = color("surface") { it.surface }, onSurface = color("on surface") { it.onSurface },
        surfaceVariant = color("surface variant") { it.surfaceVariant }, onSurfaceVariant = color("on surface variant") { it.onSurfaceVariant },
        surfaceTint = color("surface tint") { it.surfaceTint }, inverseSurface = color("inverse surface") { it.inverseSurface },
        inverseOnSurface = color("inverse on surface") { it.inverseOnSurface }, outline = color("outline") { it.outline },
        outlineVariant = color("outline variant") { it.outlineVariant }, scrim = color("scrim") { it.scrim },
        surfaceBright = color("surface bright") { it.surfaceBright }, surfaceDim = color("surface dim") { it.surfaceDim },
        surfaceContainer = color("surface container") { it.surfaceContainer }, surfaceContainerHigh = color("surface container high") { it.surfaceContainerHigh },
        surfaceContainerHighest = color("surface container highest") { it.surfaceContainerHighest }, surfaceContainerLow = color("surface container low") { it.surfaceContainerLow },
        surfaceContainerLowest = color("surface container lowest") { it.surfaceContainerLowest },
        primaryFixed = color("primary fixed") { it.primaryFixed }, primaryFixedDim = color("primary fixed dim") { it.primaryFixedDim },
        onPrimaryFixed = color("on primary fixed") { it.onPrimaryFixed }, onPrimaryFixedVariant = color("on primary fixed variant") { it.onPrimaryFixedVariant },
        secondaryFixed = color("secondary fixed") { it.secondaryFixed }, secondaryFixedDim = color("secondary fixed dim") { it.secondaryFixedDim },
        onSecondaryFixed = color("on secondary fixed") { it.onSecondaryFixed }, onSecondaryFixedVariant = color("on secondary fixed variant") { it.onSecondaryFixedVariant },
        tertiaryFixed = color("tertiary fixed") { it.tertiaryFixed }, tertiaryFixedDim = color("tertiary fixed dim") { it.tertiaryFixedDim },
        onTertiaryFixed = color("on tertiary fixed") { it.onTertiaryFixed }, onTertiaryFixedVariant = color("on tertiary fixed variant") { it.onTertiaryFixedVariant },
    )
}
