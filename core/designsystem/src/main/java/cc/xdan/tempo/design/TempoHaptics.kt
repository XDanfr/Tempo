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
class TempoHaptics internal constructor(private val feedback: HapticFeedback, private val context: android.content.Context? = null) {
    /** A selection moved: a day in the week strip, a chip, a segmented button. */
    fun tick() = feedback.performHapticFeedback(HapticFeedbackType.SegmentTick)

    /** A tappable surface was pressed. */
    fun press() = feedback.performHapticFeedback(HapticFeedbackType.ContextClick)

    /** A switch or checkbox changed. */
    fun toggle(on: Boolean) = feedback.performHapticFeedback(if (on) HapticFeedbackType.ToggleOn else HapticFeedbackType.ToggleOff)

    /** An edit was committed, undone or redone. */
    fun confirm() = feedback.performHapticFeedback(HapticFeedbackType.Confirm)

    /** Something failed or was refused. */
    fun reject() = feedback.performHapticFeedback(HapticFeedbackType.Reject)

    /**
     * One step of a counting animation. Played straight through the vibrator, because phones often drop
     * touch-style haptics when nothing is being touched. Still follows the system touch-feedback setting.
     */
    fun count() {
        val context = context ?: return tick()
        val enabled = android.provider.Settings.System.getInt(context.contentResolver, android.provider.Settings.System.HAPTIC_FEEDBACK_ENABLED, 1) != 0
        if (!enabled) return
        val vibrator = if (android.os.Build.VERSION.SDK_INT >= 31) context.getSystemService(android.os.VibratorManager::class.java)?.defaultVibrator
            else @Suppress("DEPRECATION") context.getSystemService(android.os.Vibrator::class.java)
        vibrator ?: return
        runCatching {
            val primitive = android.os.VibrationEffect.Composition.PRIMITIVE_LOW_TICK
            if (android.os.Build.VERSION.SDK_INT >= 31 && vibrator.areAllPrimitivesSupported(primitive))
                vibrator.vibrate(android.os.VibrationEffect.startComposition().addPrimitive(primitive, .6f).compose())
            else if (android.os.Build.VERSION.SDK_INT >= 29) vibrator.vibrate(android.os.VibrationEffect.createPredefined(android.os.VibrationEffect.EFFECT_TICK))
            else vibrator.vibrate(android.os.VibrationEffect.createOneShot(8, 60))
        }
    }

    /** A rare, voluntary milestone. */
    fun celebrate() = feedback.performHapticFeedback(HapticFeedbackType.LongPress)
}

@Composable
fun rememberTempoHaptics(): TempoHaptics {
    val feedback = LocalHapticFeedback.current
    val context = androidx.compose.ui.platform.LocalContext.current
    return remember(feedback, context) { TempoHaptics(feedback, context.applicationContext) }
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
