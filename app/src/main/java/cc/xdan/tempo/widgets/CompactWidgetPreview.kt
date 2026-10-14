package cc.xdan.tempo.widgets

import android.widget.FrameLayout
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import cc.xdan.tempo.model.Timetable
import java.time.ZonedDateTime

/** Inflate the production XML, so theme, timer typography, privacy and geometry agree. */
@Composable
internal fun CompactWidgetPreview(timetable: Timetable?, settings: WidgetSettings) {
    val scheme = MaterialTheme.colorScheme
    val dark = scheme.background.luminance() < .5f
    val now = remember(timetable, settings) { ZonedDateTime.now() }
    val presentation = remember(timetable, settings, now) { widgetPresentation(timetable, settings, now) }
    val fontScale = LocalConfiguration.current.fontScale
    Text("Compact preview", style = MaterialTheme.typography.titleSmall)
    BoxWithConstraints(Modifier.fillMaxWidth().height(88.dp)) {
        val width = maxWidth.value
        AndroidView(modifier = Modifier.fillMaxSize(), factory = { FrameLayout(it) }, update = { frame ->
            val views = compactWidgetViews(frame.context, 0, presentation, WidgetSizing(width, 88f, fontScale), settings,
                scheme, dark, settings.showIcons ?: (timetable?.appearance?.showIcons ?: true), now.toInstant().toEpochMilli(), preview = true)
            frame.removeAllViews()
            frame.addView(views.apply(frame.context, frame))
        })
    }
}
