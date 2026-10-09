package cc.xdan.tempo.design

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import cc.xdan.tempo.model.*

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun TempoTheme(appearance: Appearance, content: @Composable () -> Unit) {
    val dark = when (appearance.mode) { ThemeMode.DARK -> true; ThemeMode.LIGHT -> false; ThemeMode.SYSTEM -> isSystemInDarkTheme() }
    val seed = when (appearance.preset) {
        ThemePreset.FOREST, ThemePreset.MATERIAL_YOU -> Color(0xFFB7F397)
        ThemePreset.OCEAN -> Color(0xFFA2DFFF)
        ThemePreset.AMBER -> Color(0xFFFFD59B)
    }
    val context = LocalContext.current
    val scheme = if (appearance.selectedPreset == ThemePreset.MATERIAL_YOU && Build.VERSION.SDK_INT >= 31) {
        if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    } else if (dark) darkColorScheme(
        primary = seed, onPrimary = Color(0xFF152A18), primaryContainer = seed.copy(red = seed.red * .28f, green = seed.green * .28f, blue = seed.blue * .28f),
        onPrimaryContainer = seed, secondary = Color(0xFFBDCFB4), background = Color(0xFF101510), onBackground = Color(0xFFE2EADF),
        surface = Color(0xFF171E17), onSurface = Color(0xFFE2EADF), surfaceVariant = Color(0xFF283127), onSurfaceVariant = Color(0xFFBECAB9),
    ) else lightColorScheme(
        primary = when (appearance.preset) { ThemePreset.FOREST, ThemePreset.MATERIAL_YOU -> Color(0xFF386A25); ThemePreset.OCEAN -> Color(0xFF00658A); ThemePreset.AMBER -> Color(0xFF805600) },
        onPrimary = Color.White, primaryContainer = seed, onPrimaryContainer = Color(0xFF142718),
        background = Color(0xFFF5F8EF), surface = Color(0xFFF5F8EF), onSurface = Color(0xFF192117), onBackground = Color(0xFF192117),
    )
    MaterialExpressiveTheme(
        colorScheme = scheme,
        typography = TempoTypography,
        shapes = Shapes(
            extraSmall = RoundedCornerShape(12.dp), small = RoundedCornerShape(16.dp),
            medium = RoundedCornerShape(24.dp), large = RoundedCornerShape(28.dp), extraLarge = RoundedCornerShape(32.dp),
        ), content = content,
    )
}
