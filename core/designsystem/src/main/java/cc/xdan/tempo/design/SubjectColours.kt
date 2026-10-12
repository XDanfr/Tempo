package cc.xdan.tempo.design

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance

data class SubjectColour(val name: String, val argb: Long)

// Keep the original six values so saved subjects retain their selected swatch.
val subjectColours = listOf(
    SubjectColour("Forest", 0xFFB7F397), SubjectColour("Violet", 0xFFD3BCFF),
    SubjectColour("Ocean", 0xFFA2DFFF), SubjectColour("Amber", 0xFFFFD59B),
    SubjectColour("Rose", 0xFFFFB6C6), SubjectColour("Mint", 0xFFACE0D3),
    SubjectColour("Red", 0xFFE57373), SubjectColour("Orange", 0xFFFFB870),
    SubjectColour("Lemon", 0xFFF1E48A), SubjectColour("Teal", 0xFF77D5CA),
    SubjectColour("Blue", 0xFF91B8F8), SubjectColour("Magenta", 0xFFF0A5DF),
)

fun colourContrast(a: Color, b: Color): Float =
    (maxOf(a.luminance(), b.luminance()) + .05f) / (minOf(a.luminance(), b.luminance()) + .05f)

fun subjectForeground(background: Color): Color {
    val ink = Color(0xFF172116)
    if (colourContrast(ink, background) >= 4.5f) return ink
    return if (colourContrast(Color.White, background) >= colourContrast(Color.Black, background)) Color.White else Color.Black
}

/** Preserve the hue while giving outlines and icons 3:1 contrast on the actual surface. */
fun subjectOutline(colour: Color, surface: Color): Color {
    val opaque = colour.copy(alpha = 1f)
    if (colourContrast(opaque, surface) >= 3f) return opaque
    val target = if (surface.luminance() > .179f) Color.Black else Color.White
    for (step in 1..100) {
        val candidate = lerp(opaque, target, step / 100f)
        if (colourContrast(candidate, surface) >= 3f) return candidate
    }
    return target
}
