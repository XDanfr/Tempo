package cc.xdan.tempo.widgets

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.os.SystemClock
import android.util.TypedValue
import android.widget.RemoteViews
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialShapes
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import cc.xdan.tempo.R

/** Only the platform owns ticking. The app composes at schedule/setting boundaries. */
internal fun nativeWidgetCountdown(context: Context, target: Long, now: Long, ink: Color,
    widthDp: Float, heightDp: Float, requestedSp: Float): RemoteViews {
    val remaining = (target - now).coerceAtLeast(0)
    val seconds = (remaining + 999) / 1000
    val sample = if (seconds >= 3600) "%d:%02d:%02d".format(seconds / 3600, seconds / 60 % 60, seconds % 60)
        else "%02d:%02d".format(seconds / 60, seconds % 60)
    val metrics = context.resources.displayMetrics
    val paint = Paint().apply {
        typeface = context.resources.getFont(cc.xdan.tempo.core.designsystem.R.font.outfit_semibold)
        fontFeatureSettings = "tnum"
    }
    var low = 0f; var high = requestedSp
    repeat(14) {
        val size = (low + high) / 2
        paint.textSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, size, metrics)
        val fontHeight = paint.fontMetrics.run { descent - ascent }
        if (paint.measureText(sample) <= widthDp * metrics.density && fontHeight <= heightDp * metrics.density) low = size else high = size
    }
    return RemoteViews(context.packageName, R.layout.widget_native_countdown).apply {
        setTextColor(R.id.widget_live_time, ink.toArgb())
        setTextViewTextSize(R.id.widget_live_time, TypedValue.COMPLEX_UNIT_SP, low)
        setChronometerCountDown(R.id.widget_live_time, true)
        setChronometer(R.id.widget_live_time, SystemClock.elapsedRealtime() + remaining, "%s", remaining > 0)
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
