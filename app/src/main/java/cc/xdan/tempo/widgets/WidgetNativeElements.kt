package cc.xdan.tempo.widgets

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.util.TypedValue
import android.widget.RemoteViews
import android.widget.TextView
import android.view.LayoutInflater
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialShapes
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb

/** Measure the inflated host font, rather than assuming the Glance font. */
internal fun fittedWidgetText(context: Context, layout: Int, id: Int, text: String, ink: Color,
    widthDp: Float, heightDp: Float, requestedSp: Float): RemoteViews {
    val view = LayoutInflater.from(context).inflate(layout, null) as TextView
    val paint = Paint(view.paint)
    val metrics = context.resources.displayMetrics
    // Reserve a small inset for host rounding, glyph overhang and changing timer digits.
    val availableWidth = ((widthDp - 4).coerceAtLeast(1f) * metrics.density)
    val availableHeight = ((heightDp - 2).coerceAtLeast(1f) * metrics.density)
    var low = 0f
    var high = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, requestedSp, metrics)
    repeat(14) {
        val px = (low + high) / 2
        paint.textSize = px
        val fontHeight = paint.fontMetrics.run { descent - ascent }
        if (paint.measureText(text) <= availableWidth && fontHeight <= availableHeight) low = px else high = px
    }
    return RemoteViews(context.packageName, layout).apply {
        setTextViewText(id, text)
        setTextColor(id, ink.toArgb())
        setTextViewTextSize(id, TypedValue.COMPLEX_UNIT_PX, low)
    }
}

/** Rasterise only an official silhouette; text and countdown stay native host views. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
internal fun widgetCookie(sizePx: Int, colour: Color): Bitmap {
    val polygon = MaterialShapes.Cookie9Sided
    val path = Path()
    polygon.cubics.forEachIndexed { index, cubic ->
        if (index == 0) path.moveTo(cubic.anchor0X, cubic.anchor0Y)
        path.cubicTo(cubic.control0X, cubic.control0Y, cubic.control1X, cubic.control1Y, cubic.anchor1X, cubic.anchor1Y)
    }
    path.close()
    path.transform(Matrix().apply { setScale(sizePx.toFloat(), sizePx.toFloat()) })
    return Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888).also {
        Canvas(it).drawPath(path, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = colour.toArgb() })
    }
}
