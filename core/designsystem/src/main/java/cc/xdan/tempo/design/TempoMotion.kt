package cc.xdan.tempo.design

import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.ui.unit.IntOffset

/**
 * Shared motion vocabulary. Screens use these instead of hand-tuned stiffness values so the
 * whole app moves with one rhythm. Compose already scales these by the system animator duration
 * scale, so reduced-motion settings shorten or remove them without extra checks.
 */
object TempoMotion {
    /** Small, quick movements: press scale, chips, toggles. */
    fun <T> fastSpatial(): FiniteAnimationSpec<T> = spring(dampingRatio = 0.6f, stiffness = 1400f)

    /** Default movement for content changing place: list items, cards, sheets. */
    fun <T> spatial(): FiniteAnimationSpec<T> = spring(dampingRatio = 0.85f, stiffness = 500f)

    /** Large movements across the screen: day paging, page transitions. */
    fun <T> slowSpatial(): FiniteAnimationSpec<T> = spring(dampingRatio = 0.9f, stiffness = Spring.StiffnessMediumLow)

    /** Colour and opacity changes, which should never overshoot. */
    fun <T> effects(): FiniteAnimationSpec<T> = spring(dampingRatio = 1f, stiffness = 1600f)

    /** Exit fades stay short so the next state arrives without a gap. */
    fun <T> exit(): FiniteAnimationSpec<T> = tween(durationMillis = 120)

    /** Offset spring for slide transitions in AnimatedContent. */
    val slide: FiniteAnimationSpec<IntOffset> = spring(dampingRatio = 0.88f, stiffness = 500f)

    /** Scale used for pressed tappable surfaces. */
    const val PressedScale = 0.97f
}
