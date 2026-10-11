package cc.xdan.tempo.widgets

import android.appwidget.AppWidgetManager
import android.os.Build
import android.os.Bundle
import android.util.SizeF

internal data class WidgetSizing(val widthDp: Float, val heightDp: Float, val fontScale: Float) {
    val width = (widthDp / fontScale.coerceAtLeast(1f)).toInt()
    val height = (heightDp / fontScale.coerceAtLeast(1f)).toInt()
    val stacked = width < 280
    val showAgenda = if (stacked) height >= 300 else height >= 150
    val agendaRows = if (height >= (if (stacked) 420 else 230)) 2 else 1

    fun textSize(requested: Float, pill: Boolean, split: Boolean): Float {
        val available = if (split) (width - 24) / 2 else width - 24
        val cap = when {
            pill -> if (width < 180) 14f else 18f
            available < 130 || height < 150 -> 16f
            available < 190 || height < 220 -> 20f
            else -> 28f
        }
        return requested.coerceAtMost(cap)
    }
}

internal fun widgetSizes(options: Bundle, pill: Boolean): List<SizeF> {
    if (Build.VERSION.SDK_INT >= 31) {
        @Suppress("DEPRECATION")
        val reported = options.getParcelableArrayList<SizeF>(AppWidgetManager.OPTION_APPWIDGET_SIZES)
        reported?.filter { it.width.isFinite() && it.height.isFinite() && it.width > 0 && it.height > 0 }
            ?.distinct()?.take(16)?.takeIf { it.isNotEmpty() }?.let { return it }
    }
    val width = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 140).coerceAtLeast(1)
    val height = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, if (pill) 56 else 110).coerceAtLeast(1)
    // Legacy options describe portrait width / landscape height, not one rectangle.
    return listOf(
        SizeF(width.toFloat(), options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, height).coerceAtLeast(height).toFloat()),
        SizeF(options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH, width).coerceAtLeast(width).toFloat(), height.toFloat())
    ).distinct()
}
