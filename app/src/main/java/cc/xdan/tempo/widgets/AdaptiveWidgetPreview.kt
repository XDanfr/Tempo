package cc.xdan.tempo.widgets

import android.widget.FrameLayout
import android.widget.RemoteViews
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import cc.xdan.tempo.model.Timetable
import androidx.lifecycle.compose.LifecycleResumeEffect
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.time.ZonedDateTime

internal enum class WidgetPreviewSize(val label: String, val height: Int) {
    COMPACT("Compact", 88), SQUARE("Square", 240), WIDE("Wide", 190), EXPANDED("Agenda", 350)
}

/** Every preview is the same Glance snapshot sent to the launcher, not a Canvas approximation. */
@Composable
internal fun AdaptiveWidgetPreview(timetable: Timetable?, settings: WidgetSettings) {
    var selected by remember { mutableStateOf(WidgetPreviewSize.COMPACT) }
    val scheme = MaterialTheme.colorScheme
    val dark = scheme.background.luminance() < .5f
    val fontScale = LocalConfiguration.current.fontScale
    val context = LocalContext.current
    var now by remember(timetable, settings) { mutableStateOf(ZonedDateTime.now()) }
    val previewScope = rememberCoroutineScope()
    LifecycleResumeEffect(timetable, settings) {
        val job = previewScope.launch {
            while (isActive) {
                now = ZonedDateTime.now()
                val next = widgetPresentation(timetable, settings, now).nextRefreshMillis(now.toInstant().toEpochMilli())
                delay((next?.minus(System.currentTimeMillis()) ?: 60_000).coerceAtLeast(1_000))
            }
        }
        onPauseOrDispose { job.cancel() }
    }
    val presentation = remember(timetable, settings, now) { widgetPresentation(timetable, settings, now) }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            WidgetPreviewSize.entries.forEachIndexed { index, size ->
                SegmentedButton(selected == size, { selected = size },
                    SegmentedButtonDefaults.itemShape(index, WidgetPreviewSize.entries.size), icon = {}) {
                    Text(size.label, style = MaterialTheme.typography.labelMedium)
                }
            }
        }
        BoxWithConstraints(Modifier.fillMaxWidth().height(selected.height.dp), contentAlignment = Alignment.Center) {
            val width = if (selected == WidgetPreviewSize.SQUARE) minOf(maxWidth.value, 220f) else maxWidth.value
            var remote by remember { mutableStateOf<RemoteViews?>(null) }
            LaunchedEffect(presentation, settings, selected, width, scheme, fontScale) {
                remote = adaptiveWidgetViews(context, presentation, WidgetSizing(width, selected.height.toFloat(), fontScale), settings,
                    scheme, dark, settings.showIcons ?: (timetable?.appearance?.showIcons ?: true), System.currentTimeMillis(), preview = true)
            }
            AndroidView(modifier = Modifier.width(width.dp).fillMaxHeight(), factory = { FrameLayout(it) }, update = { frame ->
                remote?.let { views ->
                    frame.removeAllViews()
                    frame.addView(views.apply(frame.context, frame))
                }
            })
        }
        Text("Resize on your home screen to reveal more. Preview shows your timetable at the current time.",
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
