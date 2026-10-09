package cc.xdan.tempo.widgets

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance

internal fun widgetThemeBackground(scheme: ColorScheme, dark: Boolean): Color =
    lerp(scheme.surfaceContainerHigh, scheme.primaryFixed, if (dark) .70f else .82f)

internal fun widgetCompletionBadge(scheme: ColorScheme, dark: Boolean): Color =
    if (dark) scheme.primaryFixedDim else scheme.primaryFixed

internal fun widgetForeground(background: Color): Color {
    val ink = Color(0xFF172116)
    fun contrast(foreground: Color): Float {
        val a = foreground.luminance(); val b = background.luminance()
        return (maxOf(a, b) + .05f) / (minOf(a, b) + .05f)
    }
    val preferred = if (contrast(ink) >= contrast(Color.White)) ink else Color.White
    return if (contrast(preferred) >= 4.5f) preferred else Color.Black
}

internal fun widgetOutline(accent: Color, background: Color): Color =
    if (background.luminance() > .20f) lerp(Color.Black, accent, .38f) else accent
