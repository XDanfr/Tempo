package cc.xdan.tempo.design

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback

/**
 * Meaningful haptics only. Android applies the user's system haptic setting, so calls are
 * silent when the person has turned touch feedback off.
 */
@Stable
class TempoHaptics internal constructor(private val feedback: HapticFeedback) {
    /** A selection moved: a day in the week strip, a chip, a segmented button. */
    fun tick() = feedback.performHapticFeedback(HapticFeedbackType.SegmentTick)

    /** A tappable surface was pressed. */
    fun press() = feedback.performHapticFeedback(HapticFeedbackType.ContextClick)

    /** An edit was committed, undone or redone. */
    fun confirm() = feedback.performHapticFeedback(HapticFeedbackType.Confirm)

    /** Something failed or was refused. */
    fun reject() = feedback.performHapticFeedback(HapticFeedbackType.Reject)

    /** A rare, voluntary milestone. */
    fun celebrate() = feedback.performHapticFeedback(HapticFeedbackType.LongPress)
}

@Composable
fun rememberTempoHaptics(): TempoHaptics {
    val feedback = LocalHapticFeedback.current
    return remember(feedback) { TempoHaptics(feedback) }
}

/**
 * Springy pressed state for a tappable surface. Pass the same [interactionSource] to the
 * surface's click handling so the scale follows the real press.
 */
@Composable
fun Modifier.pressScale(interactionSource: MutableInteractionSource): Modifier {
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) TempoMotion.PressedScale else 1f, TempoMotion.fastSpatial(), label = "press scale")
    return graphicsLayer { scaleX = scale; scaleY = scale }
}
