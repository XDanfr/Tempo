package cc.xdan.tempo.ui

import androidx.activity.BackEventCompat
import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.withContext

/** Top-level Back returns to Today; Today leaves system back-to-home unconsumed. */
@Composable
fun PredictiveRouteHost(route: Destination, enabled: Boolean, navigate: (Destination) -> Unit, screen: @Composable (Destination) -> Unit) {
    val progress = remember { Animatable(0f) }
    var edge by remember { mutableIntStateOf(BackEventCompat.EDGE_LEFT) }
    var committed by remember { mutableStateOf(false) }
    val holder = rememberSaveableStateHolder()
    val shift = with(LocalDensity.current) { 36.dp.toPx() }
    val latestNavigate by rememberUpdatedState(navigate)
    PredictiveBackHandler(enabled = enabled && route != Destination.TODAY) { events ->
        try {
            events.collect { event -> edge = event.swipeEdge; progress.snapTo(event.progress) }
            committed = true
            latestNavigate(Destination.TODAY)
            progress.snapTo(0f)
        } catch (cancelled: CancellationException) {
            withContext(NonCancellable) { progress.animateTo(0f, spring(dampingRatio = .85f, stiffness = 550f)) }
        }
    }
    LaunchedEffect(route) { withFrameNanos { }; committed = false }
    Box(Modifier.fillMaxSize()) {
        if (progress.value > 0f && route != Destination.TODAY) {
            Box(Modifier.fillMaxSize().clearAndSetSemantics { }) { screen(Destination.TODAY) }
        }
        Surface(Modifier.fillMaxSize().graphicsLayer {
            scaleX = 1f - .07f * progress.value
            scaleY = 1f - .07f * progress.value
            translationX = (if (edge == BackEventCompat.EDGE_LEFT) 1 else -1) * shift * progress.value
            shape = RoundedCornerShape(28.dp)
            clip = progress.value > 0f
            shadowElevation = progress.value * 12f
        }, color = MaterialTheme.colorScheme.background) {
            AnimatedContent(route, label = "destination", transitionSpec = {
                if (committed) EnterTransition.None togetherWith ExitTransition.None
                else {
                    val direction = if (targetState.ordinal > initialState.ordinal) 1 else -1
                    (slideInHorizontally(spring(dampingRatio = .88f, stiffness = 500f)) { it * direction / 6 } + fadeIn(tween(170))) togetherWith
                        (slideOutHorizontally(tween(130)) { -it * direction / 8 } + fadeOut(tween(100)))
                }
            }) { destination -> holder.SaveableStateProvider(destination.name) { screen(destination) } }
        }
    }
}
