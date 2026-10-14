package cc.xdan.tempo.widgets

import android.graphics.Paint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.SystemClock
import android.util.TypedValue
import android.view.View
import android.widget.RemoteViews
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.toArgb
import cc.xdan.tempo.MainActivity
import cc.xdan.tempo.R
import cc.xdan.tempo.ui.accent
import cc.xdan.tempo.ui.symbol

/** RemoteViews only: host-driven countdown, no per-second app updates or bitmap text. */
internal fun compactWidgetViews(context: Context, id: Int, presentation: WidgetPresentation,
    sizing: WidgetSizing, settings: WidgetSettings, scheme: ColorScheme, dark: Boolean,
    showIcons: Boolean, nowEpochMillis: Long, preview: Boolean = false): RemoteViews {
    val until = presentation.targetEpochMillis?.minus(nowEpochMillis)
    val ticking = until != null && until in 1..604_800_000
    val split = sizing.compactSplit && ticking
    val views = RemoteViews(context.packageName, if (split) R.layout.tempo_widget_compact_split else R.layout.tempo_widget_compact)
    val accent = presentation.block?.accent(scheme.primaryFixed) ?: widgetCompletionBadge(scheme, dark)
    val fill = if (settings.lessonBackground && presentation.block != null && ticking) accent else widgetThemeBackground(scheme, dark)
    val timerFill = if (settings.lessonBackground) widgetThemeBackground(scheme, dark) else accent
    val ink = widgetForeground(fill)
    val timerInk = widgetForeground(if (split) timerFill else fill)
    fun surface(fillId: Int, outlineId: Int, colour: Int, side: Int) {
        // Physical ends are mirrored under RTL, while reading order remains subject then timer.
        val rtl = context.resources.configuration.layoutDirection == View.LAYOUT_DIRECTION_RTL
        val start = (side == 0) != rtl
        views.setImageViewResource(fillId, if (!split) R.drawable.widget_compact_single else if (start) R.drawable.widget_compact_start else R.drawable.widget_compact_end)
        views.setImageViewResource(outlineId, if (!split) R.drawable.widget_compact_single_outline else if (start) R.drawable.widget_compact_start_outline else R.drawable.widget_compact_end_outline)
        views.setInt(fillId, "setColorFilter", colour)
        views.setInt(outlineId, "setColorFilter", widgetOutline(scheme).toArgb())
        views.setViewVisibility(outlineId, if (settings.showOutline) View.VISIBLE else View.GONE)
    }
    surface(R.id.widget_background, R.id.widget_outline, fill.toArgb(), 0)
    if (split) surface(R.id.widget_background_time, R.id.widget_outline_time, timerFill.toArgb(), 1)
    if (!split && sizing.height < 48) {
        val density = context.resources.displayMetrics.density
        views.setViewPadding(R.id.widget_compact_content, (12 * density).toInt(), (3 * density).toInt(), (12 * density).toInt(), (3 * density).toInt())
    }
    views.setTextViewText(R.id.widget_title, presentation.title)
    views.setTextColor(R.id.widget_title, ink.toArgb())
    val titleSize = when {
        sizing.height < 48 -> sizing.compactTitleSp
        presentation.state == WidgetState.WEEK_DONE -> 22f
        presentation.state == WidgetState.DONE_TODAY -> 18f
        else -> sizing.compactTitleSp
    }
    views.setTextViewTextSize(R.id.widget_title, TypedValue.COMPLEX_UNIT_SP, titleSize)
    views.setInt(R.id.widget_title, "setMaxLines", if (!ticking && sizing.height >= 48 || split && sizing.height >= 80) 2 else 1)
    views.setTextColor(R.id.widget_timer, timerInk.toArgb())
    // A future lesson can produce a three-digit hour count. Measure the actual font,
    // including the label and system font scale, before choosing the timer size.
    val seconds = ((until ?: 0).coerceAtLeast(0) + 999) / 1000
    val clock = if (seconds >= 3600) "%d:%02d:%02d".format(seconds / 3600, seconds / 60 % 60, seconds % 60)
        else "%02d:%02d".format(seconds / 60, seconds % 60)
    val sample = clock
    val requested = if (split) 28f else sizing.compactTimerSp
    val completion = presentation.state in listOf(WidgetState.WEEK_DONE, WidgetState.DONE_TODAY)
    val visibleIcon = showIcons && sizing.compactIcon && !split &&
        (completion || presentation.block == null || presentation.block?.symbol() != null)
    val label = if (split) presentation.countdownLabel else if (presentation.current) "Left" else "In"
    val font = Paint().apply {
        typeface = context.resources.getFont(cc.xdan.tempo.core.designsystem.R.font.outfit_semibold)
        fontFeatureSettings = "tnum"
    }
    val availableDp = (if (split) (sizing.widthDp - WidgetShapeRules.GAP) / 2 else sizing.widthDp) - 24 -
        if (visibleIcon) 32 else 0
    font.textSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 12f, context.resources.displayMetrics)
    val labelWidth = if (split) 0f else font.measureText(label) + 6 * context.resources.displayMetrics.density
    val availablePx = availableDp * context.resources.displayMetrics.density - labelWidth
    var minimum = 0f
    var maximum = requested
    repeat(12) {
        val candidate = (minimum + maximum) / 2
        font.textSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, candidate, context.resources.displayMetrics)
        if (font.measureText(sample) <= availablePx) minimum = candidate else maximum = candidate
    }
    views.setTextViewTextSize(R.id.widget_timer, TypedValue.COMPLEX_UNIT_SP, minimum)
    views.setViewVisibility(R.id.widget_timer, if (ticking) View.VISIBLE else View.GONE)
    views.setChronometerCountDown(R.id.widget_timer, true)
    views.setChronometer(R.id.widget_timer, SystemClock.elapsedRealtime() + (until ?: 0), "%s", ticking)
    views.setTextViewText(R.id.widget_countdown_label, label)
    views.setTextColor(R.id.widget_countdown_label, timerInk.toArgb())
    views.setViewVisibility(R.id.widget_countdown_label, if (ticking) View.VISIBLE else View.GONE)
    if (!split) views.setViewVisibility(R.id.widget_countdown_row, if (ticking) View.VISIBLE else View.GONE)
    val detail = if (ticking) presentation.location else when (presentation.state) {
        WidgetState.MISSING -> "Configure in widget settings"
        WidgetState.SETUP -> "Open Tempo to finish setup"
        WidgetState.WEEK_DONE -> "Nothing else this week"
        WidgetState.DONE_TODAY -> "More to come this week"
        else -> null
    }
    views.setTextViewText(R.id.widget_detail, detail.orEmpty())
    views.setTextColor(R.id.widget_detail, ink.toArgb())
    views.setViewVisibility(R.id.widget_detail, if (detail != null && sizing.height >= if (split) 96 else 88) View.VISIBLE else View.GONE)
    val icon = if (presentation.state in listOf(WidgetState.WEEK_DONE, WidgetState.DONE_TODAY)) Icons.Outlined.CheckCircle
        else presentation.block?.symbol() ?: Icons.Outlined.CalendarMonth
    views.setViewVisibility(R.id.widget_icon, if (visibleIcon) View.VISIBLE else View.GONE)
    views.setImageViewBitmap(R.id.widget_icon, widgetIcon(icon, ink.toArgb()))
    val open = Intent(context, MainActivity::class.java).putExtra("widgetTimetableId", settings.timetableId)
    if (!preview) views.setOnClickPendingIntent(R.id.widget_root, PendingIntent.getActivity(context, id, open, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE))
    return views
}
