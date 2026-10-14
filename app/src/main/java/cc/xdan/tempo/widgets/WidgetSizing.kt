package cc.xdan.tempo.widgets

import android.appwidget.AppWidgetManager
import android.os.Build
import android.os.Bundle
import android.util.SizeF

internal enum class WidgetComposition { COMPACT, SQUARE, WIDE, EXPANDED }

internal data class WidgetSizing(val widthDp: Float, val heightDp: Float, val fontScale: Float) {
    val width = (widthDp / fontScale.coerceAtLeast(1f)).toInt()
    val height = (heightDp / fontScale.coerceAtLeast(1f)).toInt()
    // Content budgets, independent of provider and launcher cell count.
    val composition: WidgetComposition get() = when {
        height < 136 -> WidgetComposition.COMPACT
        width >= 280 && height >= 230 || width < 280 && height >= 300 -> WidgetComposition.EXPANDED
        width >= 280 -> WidgetComposition.WIDE
        else -> WidgetComposition.SQUARE
    }
    val stacked = width < 280

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
