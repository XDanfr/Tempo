package cc.xdan.tempo.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import cc.xdan.tempo.Feedback
import cc.xdan.tempo.design.rememberTempoHaptics

/** Text shown for a piece of feedback, and the action it offers. */
internal fun Feedback.presentation(): Pair<String, String?> = when (this) {
    is Feedback.Changed -> label to "Undo"
    is Feedback.Undone -> "Undone: $label" to "Redo"
    is Feedback.Info -> text to null
}

/**
 * Shows one snackbar at a time for committed actions. A newer piece of feedback replaces the
 * visible one. Undo/Redo carry the history revision they describe, and timing out or closing
 * the snackbar never erases history: the top-bar menu still offers Undo and Redo.
 */
@Composable
fun FeedbackHost(feedback: Feedback?, undo: (Int) -> Unit, redo: (Int) -> Unit, dismiss: (Long) -> Unit, modifier: Modifier = Modifier) {
    val host = remember { SnackbarHostState() }
    val haptics = rememberTempoHaptics()
    val latestUndo by rememberUpdatedState(undo)
    val latestRedo by rememberUpdatedState(redo)
    val latestDismiss by rememberUpdatedState(dismiss)
    LaunchedEffect(feedback?.id) {
        val shown = feedback ?: return@LaunchedEffect
        val (text, action) = shown.presentation()
        if (shown !is Feedback.Info) haptics.confirm()
        // Actionable snackbars stay longer; Material extends them further for accessibility services.
        val result = host.showSnackbar(text, actionLabel = action, withDismissAction = true,
            duration = if (action != null) SnackbarDuration.Long else SnackbarDuration.Short)
        if (result == SnackbarResult.ActionPerformed) when (shown) {
            is Feedback.Changed -> latestUndo(shown.revision)
            is Feedback.Undone -> latestRedo(shown.revision)
            is Feedback.Info -> Unit
        }
        latestDismiss(shown.id)
    }
    SnackbarHost(host, modifier) { data ->
        Snackbar(data, shape = MaterialTheme.shapes.medium,
            containerColor = MaterialTheme.colorScheme.inverseSurface,
            contentColor = MaterialTheme.colorScheme.inverseOnSurface,
            actionColor = MaterialTheme.colorScheme.inversePrimary,
            dismissActionContentColor = MaterialTheme.colorScheme.inverseOnSurface)
    }
}
